package com.npg418.markerlib.marker.server.command

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.npg418.markerlib.marker.server.MarkerService
import com.npg418.markerlib.marker.server.ServerMarker
import com.npg418.markerlib.marker.server.ServerMarkers
import com.npg418.markerlib.marker.shared.Marker
import com.npg418.markerlib.marker.shared.MarkerTarget
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands.argument
import net.minecraft.commands.Commands.literal
import net.minecraft.commands.arguments.EntityArgument.entities
import net.minecraft.network.chat.Component

object CreateCommand {
    fun node(): LiteralArgumentBuilder<CommandSourceStack> = literal("create").then(
        idArgumentNode().then(
            literal("fixed").then(
                positionsNode { ctx, positions ->
                    create(
                        ctx,
                        ServerMarker(
                            Marker(ctx.id, MarkerTarget.Fixed(positions)),
                        ),
                    )
                },
            ),
        ).then(
            literal("entities").then(
                argument("selector", entities()).executes {
                    create(
                        it,
                        ServerMarker(
                            Marker(it.id, MarkerTarget.Entities(emptySet())),
                            entitySelector = it.rawArgument(),
                        ),
                    )
                },
            ),
        ),
    )

    private fun create(ctx: Ctx, marker: ServerMarker): Int {
        val server = ctx.source.server
        if (ServerMarkers.of(server)[marker.id] != null) throw CommandErrors.EXISTS.create(marker.id)
        MarkerService.put(server, marker)
        MarkerService.refreshVisibleTo(server, marker)
        MarkerService.refreshEntities(server, marker)
        ctx.source.sendSuccess({ Component.literal("Created marker ${marker.id}") }, true)
        return 1
    }
}