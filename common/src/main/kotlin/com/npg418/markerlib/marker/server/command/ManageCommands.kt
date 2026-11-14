package com.npg418.markerlib.marker.server.command

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.npg418.markerlib.marker.server.MarkerService
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands.literal
import net.minecraft.network.chat.Component

object ManageCommands {
    fun removeNode(): LiteralArgumentBuilder<CommandSourceStack> = literal("remove").then(
        idArgumentNode().suggests(idSuggestions).executes { ctx ->
            if (!MarkerService.of(ctx.source.server).remove(ctx.id)) {
                throw CommandErrors.NOT_FOUND.create(ctx.id)
            }
            ctx.source.sendSuccess({ Component.literal("Removed marker ${ctx.id}") }, true)
            return@executes 1
        },
    )

    fun clearNode(): LiteralArgumentBuilder<CommandSourceStack> = literal("clear").executes {
        MarkerService.of(it.source.server).clear()
        it.source.sendSuccess({ Component.literal("Cleared all markers") }, true)
        return@executes 1
    }

    fun listNode(): LiteralArgumentBuilder<CommandSourceStack> = literal("list").executes { ctx ->
        val entries = MarkerService.of(ctx.source.server).all
        val text = entries.joinToString {
            "${it.id} (positions x${it.marker.positions.size}, entities x${it.marker.entities.size}${
                it.entitySelector?.let { s ->
                    " $s"
                } ?: ""
            })"
        }
        ctx.source.sendSuccess({ Component.literal("Markers (${entries.size}): $text") }, false)
        return@executes entries.size
    }
}