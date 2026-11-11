package com.npg418.markerlib.marker.server.command

import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.arguments.BoolArgumentType
import com.mojang.brigadier.arguments.BoolArgumentType.bool
import com.mojang.brigadier.arguments.FloatArgumentType
import com.mojang.brigadier.arguments.FloatArgumentType.floatArg
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.arguments.StringArgumentType.greedyString
import com.mojang.brigadier.arguments.StringArgumentType.word
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.npg418.markerlib.marker.shared.Marker
import com.npg418.markerlib.marker.shared.MarkerTarget
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands.argument
import net.minecraft.commands.Commands.literal
import net.minecraft.commands.arguments.EntityArgument
import net.minecraft.commands.arguments.ResourceLocationArgument

object SetCommand {
    private val arrowNode = literal("arrow")
        .then(
            literal("off").executes { ctx ->
                updateMarker(ctx) { it.copy(arrowIcon = null) }
            },
        )
        .then(
            argument("icon", ResourceLocationArgument.id()).executes { ctx ->
                val icon = ResourceLocationArgument.getId(ctx, "icon")
                updateMarker(ctx) { it.copy(arrowIcon = icon) }
            },
        )

    private val visibleToNode = literal("visible_to")
        .then(literal("all").executes { ctx -> updateServerMarker(ctx) { it.copy(visibleTo = null) } })
        .then(
            argument("players", EntityArgument.players()).executes { ctx ->
                val raw = ctx.rawArgument()
                updateServerMarker(ctx) { it.copy(visibleTo = raw) }
            },
        )
    private val selectorNode = literal("selector").then(
        argument("selector", EntityArgument.entities()).executes { ctx ->
            val raw = ctx.rawArgument()
            updateServerMarker(ctx, refresh = true) { e ->
                if (e.marker.target !is MarkerTarget.Entities) throw CommandErrors.NOT_ENTITIES.create()
                e.copy(entitySelector = raw)
            }
        },
    )

    val node: LiteralArgumentBuilder<CommandSourceStack> = literal("set").then(
        idArgumentNode.then(property("color", word(), StringArgumentType::getString) { copy(color = parseColor(it)) })
            .then(property("size", floatArg(0.25f, 8f), FloatArgumentType::getFloat) { copy(size = it) })
            .then(property("label", greedyString(), StringArgumentType::getString) { copy(label = it) })
            .then(property("icon", ResourceLocationArgument.id(), ResourceLocationArgument::getId) { copy(icon = it) })
            .then(arrowNode)
            .then(property("show_distance", bool(), BoolArgumentType::getBool) { copy(showDistance = it) })
            .then(visibleToNode)
            .then(selectorNode),
    )

    private fun <T> property(
        name: String,
        type: ArgumentType<T>,
        read: (Ctx, String) -> T,
        change: Marker.(T) -> Marker,
    ) = literal(name).then(
        argument(name, type).executes { ctx ->
            val value = read(ctx, name)
            updateServerMarker(ctx) { it.copy(marker = it.marker.change(value)) }
        },
    )
}