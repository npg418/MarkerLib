package com.npg418.markerlib.marker.server

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.DoubleArgumentType.doubleArg
import com.mojang.brigadier.arguments.DoubleArgumentType.getDouble
import com.mojang.brigadier.arguments.FloatArgumentType.floatArg
import com.mojang.brigadier.arguments.FloatArgumentType.getFloat
import com.mojang.brigadier.arguments.IntegerArgumentType.getInteger
import com.mojang.brigadier.arguments.IntegerArgumentType.integer
import com.mojang.brigadier.arguments.StringArgumentType.*
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType
import com.mojang.brigadier.suggestion.SuggestionProvider
import com.npg418.markerlib.marker.common.Marker
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands.argument
import net.minecraft.commands.Commands.literal
import net.minecraft.commands.SharedSuggestionProvider
import net.minecraft.commands.arguments.EntityArgument
import net.minecraft.commands.arguments.ResourceLocationArgument
import net.minecraft.commands.arguments.coordinates.Vec3Argument
import net.minecraft.network.chat.Component

private typealias Ctx = CommandContext<CommandSourceStack>

object MarkerCommand {
    private val NOT_FOUND = DynamicCommandExceptionType { Component.literal("Marker not found: $it") }
    private val EXISTS = DynamicCommandExceptionType { Component.literal("Marker already exists: $it") }
    private val BAD_COLOR = SimpleCommandExceptionType(Component.literal("Color must be RRGGBB or AARRGGBB (hex)"))
    private val NOT_FIXED = SimpleCommandExceptionType(Component.literal("Marker target is not fixed points"))
    private val NOT_ENTITY = SimpleCommandExceptionType(Component.literal("Marker target is not an entity selector"))
    private val BAD_INDEX = SimpleCommandExceptionType(Component.literal("Point index out of range"))

    private val ID_SUGGESTIONS = SuggestionProvider<CommandSourceStack> { ctx, builder ->
        SharedSuggestionProvider.suggest(ServerMarkers.get(ctx.source.server).all.map { it.id }, builder)
    }

    fun register(dispatcher: CommandDispatcher<CommandSourceStack>, service: MarkerService) {
        dispatcher.register(
            literal("marker").requires { it.hasPermission(2) }.then(addNode(service)).then(setNode(service)).then(
                    literal("remove").then(
                        argument("id", word()).suggests(ID_SUGGESTIONS).executes { remove(it, service) },
                    ),
                ).then(literal("clear").executes { clear(it, service) }).then(literal("list").executes { list(it) }),
        )
    }

    private fun addNode(service: MarkerService) = literal("add").then(
        argument("id", word()).then(
                literal("pos").then(
                    argument("pos", Vec3Argument.vec3()).executes {
                        add(it, service, Marker.Target.Fixed(listOf(Vec3Argument.getVec3(it, "pos"))))
                    },
                ),
            ).then(
                literal("entity").then(
                    argument("selector", EntityArgument.entities()).executes {
                        add(it, service, Marker.Target.Entity(uuids = emptySet(), selector = rawArgument(it)))
                    },
                ),
            ),
    )

    private fun add(ctx: Ctx, service: MarkerService, target: Marker.Target): Int {
        val id = getString(ctx, "id")
        val server = ctx.source.server
        if (ServerMarkers.get(server)[id] != null) throw EXISTS.create(id)
        val marker = Marker(id, target)
        service.add(server, marker)
        service.refresh(server, marker)
        ctx.source.sendSuccess({ Component.literal("Added marker $id") }, true)
        return 1
    }

    private fun setNode(service: MarkerService) = literal("set").then(
        argument("id", word()).suggests(ID_SUGGESTIONS).then(
                literal("color").then(
                    argument("color", word()).executes { ctx ->
                        val color = parseColor(getString(ctx, "color"))
                        update(ctx, service) { it.copy(color = color) }
                    },
                ),
            ).then(
                literal("size").then(
                    argument("size", floatArg(0.25f, 8f)).executes { ctx ->
                        val size = getFloat(ctx, "size")
                        update(ctx, service) { it.copy(size = size) }
                    },
                ),
            ).then(
                literal("label").then(
                    argument("label", greedyString()).executes { ctx ->
                        val label = getString(ctx, "label")
                        update(ctx, service) { it.copy(label = label) }
                    },
                ),
            ).then(
                literal("icon").then(
                    argument("icon", ResourceLocationArgument.id()).executes { ctx ->
                        val icon = ResourceLocationArgument.getId(ctx, "icon")
                        update(ctx, service) { it.copy(icon = icon) }
                    },
                ),
            ).then(visibleToNode(service)).then(pointNode(service)).then(selectorNode(service)),
    )

    private fun visibleToNode(service: MarkerService) = literal("visible_to").then(literal("all").executes {
            update(
                it,
                service
            ) { m -> m.copy(visibleTo = emptySet()) }
        }).then(
            argument("players", EntityArgument.players()).executes { ctx ->
                val ids = EntityArgument.getPlayers(ctx, "players").map { it.uuid }.toSet()
                update(ctx, service) { it.copy(visibleTo = ids) }
            },
        )

    private fun pointNode(service: MarkerService) = literal("point").then(
            literal("add").then(
                argument("pos", Vec3Argument.vec3()).executes { ctx ->
                    val p = Vec3Argument.getVec3(ctx, "pos")
                    updateFixed(ctx, service) { it.copy(positions = it.positions + p) }
                },
            ),
        ).then(
            literal("remove").then(
                argument("index", integer(0)).executes { ctx ->
                    val i = getInteger(ctx, "index")
                    updateFixed(ctx, service) {
                        if (i >= it.positions.size) throw BAD_INDEX.create()
                        it.copy(positions = it.positions.filterIndexed { idx, _ -> idx != i })
                    }
                },
            ),
        )

    private fun selectorNode(service: MarkerService) = literal("selector").then(
            argument("selector", EntityArgument.entities()).executes { ctx ->
                val raw = rawArgument(ctx)
                updateEntity(ctx, service, refresh = true) { it.copy(selector = raw) }
            },
        ).then(
            literal("offset").then(
                argument("offset", doubleArg(-16.0, 16.0)).executes { ctx ->
                    val offset = getDouble(ctx, "offset")
                    updateEntity(ctx, service) { it.copy(yOffset = offset) }
                },
            ),
        )

    private fun update(
        ctx: Ctx,
        service: MarkerService,
        refresh: Boolean = false,
        transform: (Marker) -> Marker,
    ): Int {
        val id = getString(ctx, "id")
        val server = ctx.source.server
        val marker = ServerMarkers.get(server)[id] ?: throw NOT_FOUND.create(id)
        val updated = transform(marker)
        service.add(server, updated)
        if (refresh) service.refresh(server, updated)
        ctx.source.sendSuccess({ Component.literal("Updated marker $id") }, true)
        return 1
    }

    private fun updateFixed(ctx: Ctx, service: MarkerService, transform: (Marker.Target.Fixed) -> Marker.Target.Fixed) =
        update(ctx, service) { m ->
            m.copy(target = transform(m.target as? Marker.Target.Fixed ?: throw NOT_FIXED.create()))
        }

    private fun updateEntity(
        ctx: Ctx,
        service: MarkerService,
        refresh: Boolean = false,
        transform: (Marker.Target.Entity) -> Marker.Target.Entity,
    ) = update(ctx, service, refresh) { m ->
        m.copy(target = transform(m.target as? Marker.Target.Entity ?: throw NOT_ENTITY.create()))
    }

    private fun remove(ctx: Ctx, service: MarkerService): Int {
        val id = getString(ctx, "id")
        if (!service.remove(ctx.source.server, id)) throw NOT_FOUND.create(id)
        ctx.source.sendSuccess({ Component.literal("Removed marker $id") }, true)
        return 1
    }

    private fun clear(ctx: Ctx, service: MarkerService): Int {
        service.clear(ctx.source.server)
        ctx.source.sendSuccess({ Component.literal("Cleared all markers") }, true)
        return 1
    }

    private fun list(ctx: Ctx): Int {
        val markers = ServerMarkers.get(ctx.source.server).all
        val text = markers.joinToString { m ->
            when (val t = m.target) {
                is Marker.Target.Fixed -> "${m.id} (fixed x${t.positions.size})"
                is Marker.Target.Entity -> "${m.id} (entity x${t.uuids.size})"
            }
        }
        ctx.source.sendSuccess({ Component.literal("Markers (${markers.size}): $text") }, false)
        return markers.size
    }

    private fun rawArgument(ctx: Ctx): String = ctx.nodes.last().range.get(ctx.input)

    private fun parseColor(s: String): Int {
        val hex = s.removePrefix("#")
        val value = hex.toLongOrNull(16) ?: throw BAD_COLOR.create()
        return when (hex.length) {
            6 -> (0xFF000000 or value).toInt()
            8 -> value.toInt()
            else -> throw BAD_COLOR.create()
        }
    }
}