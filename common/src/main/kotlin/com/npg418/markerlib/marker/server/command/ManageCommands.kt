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
        idArgumentNode().executes { ctx ->
            if (!MarkerService(ctx.source.server).remove(ctx.id)) {
                throw CommandErrors.NOT_FOUND.create(ctx.id)
            }
            ctx.source.sendSuccess({ Component.literal("Removed marker ${ctx.id}") }, true)
            return@executes 1
        },
    )

    fun clearNode(): LiteralArgumentBuilder<CommandSourceStack> = literal("clear").executes {
        MarkerService(it.source.server).clear()
        it.source.sendSuccess({ Component.literal("Cleared all markers") }, true)
        return@executes 1
    }

    fun listNode(): LiteralArgumentBuilder<CommandSourceStack> = literal("list").executes {
        val entries = ServerMarkers.of(it.source.server).all
        val text = entries.joinToString { m ->
        }
        it.source.sendSuccess({ Component.literal("Markers (${entries.size}): $text") }, false)
        return@executes entries.size
    }
}