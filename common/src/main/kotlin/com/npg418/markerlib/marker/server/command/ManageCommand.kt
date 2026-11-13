package com.npg418.markerlib.marker.server.command

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.npg418.markerlib.marker.server.MarkerService
import com.npg418.markerlib.marker.server.ServerMarkers
import com.npg418.markerlib.marker.shared.MarkerTarget
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands.literal
import net.minecraft.network.chat.Component

object ManageCommand {
    fun removeNode(): LiteralArgumentBuilder<CommandSourceStack> = literal("remove").then(
        idArgumentNode().executes {
            if (!MarkerService.remove(it.source.server, it.id)) {
                throw CommandErrors.NOT_FOUND.create(it.id)
            }
            it.source.sendSuccess({ Component.literal("Removed marker ${it.id}") }, true)
            return@executes 1
        },
    )

    fun clearNode(): LiteralArgumentBuilder<CommandSourceStack> = literal("clear").executes {
        MarkerService.clear(it.source.server)
        it.source.sendSuccess({ Component.literal("Cleared all markers") }, true)
        return@executes 1
    }

    fun listNode(): LiteralArgumentBuilder<CommandSourceStack> = literal("list").executes {
        val entries = ServerMarkers.of(it.source.server).all
        val text = entries.joinToString { m ->
            when (val t = m.marker.target) {
                is MarkerTarget.Fixed -> "${m.id} (fixed x${t.positions.size})"

                is MarkerTarget.Entities -> "${m.id} (entities x${t.entities.size}${m.entitySelector?.let { s ->
                    " $s"
                } ?: ""})"
            }
        }
        it.source.sendSuccess({ Component.literal("Markers (${entries.size}): $text") }, false)
        return@executes entries.size
    }
}