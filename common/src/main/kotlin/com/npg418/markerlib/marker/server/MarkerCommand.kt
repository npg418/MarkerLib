package com.npg418.markerlib.marker.server

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.FloatArgumentType.floatArg
import com.mojang.brigadier.arguments.StringArgumentType.greedyString
import com.mojang.brigadier.arguments.StringArgumentType.word
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType
import com.mojang.brigadier.suggestion.SuggestionProvider
import com.npg418.markerlib.marker.common.Marker
import com.npg418.markerlib.marker.common.Marker.Target
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands.argument
import net.minecraft.commands.Commands.literal
import net.minecraft.commands.SharedSuggestionProvider
import net.minecraft.commands.arguments.EntityArgument
import net.minecraft.commands.arguments.ResourceLocationArgument
import net.minecraft.commands.arguments.coordinates.BlockPosArgument
import net.minecraft.commands.arguments.coordinates.Vec3Argument
import net.minecraft.network.chat.Component

object MarkerCommand {
    private val NOT_FOUND = DynamicCommandExceptionType { Component.literal("Marker not found: $it") }
    private val EXISTS = DynamicCommandExceptionType { Component.literal("Marker already exists: $it") }
    private val BAD_COLOR = SimpleCommandExceptionType(Component.literal("Color must be RRGGBB or AARRGGBB (hex)"))

    private val ID_SUGGESTIONS = SuggestionProvider<CommandSourceStack> { ctx, builder ->
        SharedSuggestionProvider.suggest(ServerMarkers.get(ctx.source.server).all.map { it.id }, builder)
    }

    fun register(dispatcher: CommandDispatcher<CommandSourceStack>, service: MarkerService) {
        dispatcher.register(
            literal("marker").requires { it.hasPermission(2) }
                .then(
                    literal("add").then(
                        argument("id", word())
                            .then(
                                literal("pos").then(
                                    argument("pos", Vec3Argument.vec3()).executes {
                                        add(it, service, Target.Fixed(Vec3Argument.getVec3(it, "pos")))
                                    },
                                ),
                            )
                            .then(
                                literal("entity").then(
                                    argument("entity", EntityArgument.entity()).executes {
                                        add(it, service, Target.Entity(EntityArgument.getEntity(it, "entity").uuid))
                                    },
                                ),
                            )
                            .then(
                                literal("block").then(
                                    argument("pos", BlockPosArgument.blockPos()).executes {
                                        add(it, service, Target.Block(BlockPosArgument.getBlockPos(it, "pos")))
                                    },
                                ),
                            ),
                    ),
                )
                .then(
                    literal("remove").then(
                        argument("id", word()).suggests(ID_SUGGESTIONS).executes { remove(it, service) },
                    ),
                )
                .then(literal("clear").executes { clear(it, service) })
                .then(literal("list").executes { list(it) })
                .then(
                    literal("set").then(
                        argument("id", word()).suggests(ID_SUGGESTIONS)
                            .then(
                                literal("color").then(
                                    argument("color", word()).executes { ctx ->
                                        val color = parseColor(ctx.getArgument("color", String::class.java))
                                        update(ctx, service) { it.copy(color = color) }
                                    },
                                ),
                            )
                            .then(
                                literal("size").then(
                                    argument("size", floatArg(0.25f, 8f)).executes { ctx ->
                                        val size = ctx.getArgument("size", Float::class.java)
                                        update(ctx, service) { it.copy(size = size) }
                                    },
                                ),
                            )
                            .then(
                                literal("label").then(
                                    argument("label", greedyString()).executes { ctx ->
                                        val label = ctx.getArgument("label", String::class.java)
                                        update(ctx, service) { it.copy(label = label) }
                                    },
                                ),
                            )
                            .then(
                                literal("icon").then(
                                    argument("icon", ResourceLocationArgument.id()).executes { ctx ->
                                        val icon = ResourceLocationArgument.getId(ctx, "icon")
                                        update(ctx, service) { it.copy(icon = icon) }
                                    },
                                ),
                            )
                            .then(
                                literal("visible_to")
                                    .then(
                                        literal("all").executes {
                                            update(
                                                it,
                                                service,
                                            ) { m -> m.copy(visibleTo = emptySet()) }
                                        },
                                    )
                                    .then(
                                        argument("players", EntityArgument.players()).executes { ctx ->
                                            val ids = EntityArgument.getPlayers(ctx, "players").map { it.uuid }.toSet()
                                            update(ctx, service) { it.copy(visibleTo = ids) }
                                        },
                                    ),
                            ),
                    ),
                ),
        )
    }

    private fun parseColor(s: String): Int {
        val hex = s.removePrefix("#")
        val value = hex.toLongOrNull(16) ?: throw BAD_COLOR.create()
        return when (hex.length) {
            6 -> (0xFF000000 or value).toInt()
            8 -> value.toInt()
            else -> throw BAD_COLOR.create()
        }
    }

    private fun add(ctx: CommandContext<CommandSourceStack>, service: MarkerService, target: Target): Int {
        val id = ctx.getArgument("id", String::class.java)
        val server = ctx.source.server
        if (ServerMarkers.get(server)[id] != null) throw EXISTS.create(id)
        service.add(server, Marker(id, target))
        ctx.source.sendSuccess({ Component.literal("Added marker $id") }, true)
        return 1
    }

    private fun update(
        ctx: CommandContext<CommandSourceStack>,
        service: MarkerService,
        transform: (Marker) -> Marker,
    ): Int {
        val id = ctx.getArgument("id", String::class.java)
        val server = ctx.source.server
        val marker = ServerMarkers.get(server)[id] ?: throw NOT_FOUND.create(id)
        service.add(server, transform(marker))
        ctx.source.sendSuccess({ Component.literal("Updated marker $id") }, true)
        return 1
    }

    private fun remove(ctx: CommandContext<CommandSourceStack>, service: MarkerService): Int {
        val id = ctx.getArgument("id", String::class.java)
        if (!service.remove(ctx.source.server, id)) throw NOT_FOUND.create(id)
        ctx.source.sendSuccess({ Component.literal("Removed marker $id") }, true)
        return 1
    }

    private fun clear(ctx: CommandContext<CommandSourceStack>, service: MarkerService): Int {
        service.clear(ctx.source.server)
        ctx.source.sendSuccess({ Component.literal("Cleared all markers") }, true)
        return 1
    }

    private fun list(ctx: CommandContext<CommandSourceStack>): Int {
        val markers = ServerMarkers.get(ctx.source.server).all
        ctx.source.sendSuccess(
            { Component.literal("Markers (${markers.size}): " + markers.joinToString { it.id }) },
            false,
        )
        return markers.size
    }
}