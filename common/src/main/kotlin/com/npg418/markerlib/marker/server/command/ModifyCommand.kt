package com.npg418.markerlib.marker.server.command

import com.mojang.brigadier.arguments.*
import com.mojang.brigadier.arguments.BoolArgumentType.bool
import com.mojang.brigadier.arguments.DoubleArgumentType.doubleArg
import com.mojang.brigadier.arguments.FloatArgumentType.floatArg
import com.mojang.brigadier.arguments.StringArgumentType.greedyString
import com.mojang.brigadier.arguments.StringArgumentType.word
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.npg418.markerlib.marker.shared.Marker
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands.argument
import net.minecraft.commands.Commands.literal
import net.minecraft.commands.arguments.EntityArgument
import net.minecraft.commands.arguments.ResourceLocationArgument

object ModifyCommand {
    fun node(): LiteralArgumentBuilder<CommandSourceStack> = literal("modify").then(
        idArgumentNode().suggests(idSuggestions)
            .then(property("color", word(), StringArgumentType::getString) { copy(color = parseColor(it)) })
            .then(
                property(
                    "icon",
                    ResourceLocationArgument.id(),
                    ResourceLocationArgument::getId,
                ) { copy(icon = icon.copy(iconTexture = it)) },
            ).then(
                property(
                    "arrow",
                    ResourceLocationArgument.id(),
                    ResourceLocationArgument::getId,
                ) { copy(icon = icon.copy(arrowTexture = it)) }.then(
                    literal("off").executes { ctx ->
                        updateMarker(ctx) { copy(icon = icon.copy(arrowTexture = null)) }
                    },
                ),
            ).then(
                property("entity_offset", doubleArg(), DoubleArgumentType::getDouble) {
                    copy(icon = icon.copy(entityIconOffset = it))
                },
            ).then(property("blend", floatArg(0f, 1f), FloatArgumentType::getFloat) { copy(blend = it) })
            .then(property("label", greedyString(), StringArgumentType::getString) { copy(label = it) })
            .then(property("show_distance", bool(), BoolArgumentType::getBool) { copy(showDistance = it) })
            .then(visibleToNode()),
    )

    private fun <T> property(
        name: String,
        type: ArgumentType<T>,
        read: (Ctx, String) -> T,
        change: Marker.(T) -> Marker,
    ) = literal(name).then(
        argument(name, type).executes { ctx ->
            val value = read(ctx, name)
            updateMarker(ctx) { change(value) }
        },
    )

    private fun visibleToNode() = literal("visible_to").then(
        literal("all").executes { ctx ->
            updateServerEntry(ctx) { it.copy(visibleTo = null) }
        },
    )
        .then(
            argument("players", EntityArgument.players()).executes { ctx ->
                val raw = ctx.rawArgument("players")
                updateServerEntry(ctx, refresh = true) { it.copy(visibleTo = raw) }
            },
        )
}