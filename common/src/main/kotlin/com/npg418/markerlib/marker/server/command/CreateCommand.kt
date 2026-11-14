package com.npg418.markerlib.marker.server.command

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.npg418.markerlib.marker.server.MarkerService
import com.npg418.markerlib.marker.server.ServerEntry
import com.npg418.markerlib.marker.shared.Marker
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands.literal
import net.minecraft.network.chat.Component

object CreateCommand {
    fun node(): LiteralArgumentBuilder<CommandSourceStack> = literal("create").then(
        idArgumentNode().executes { ctx ->
            val server = ctx.source.server
            val service = MarkerService.of(server)
            if (service[ctx.id] != null) throw CommandErrors.EXISTS.create(ctx.id)
            val entry = ServerEntry(Marker(ctx.id))
            service.put(entry)
            service.refreshVisibleTo(entry)
            service.refreshEntities(entry)
            ctx.source.sendSuccess({ Component.literal("Created marker ${entry.id}") }, true)
            return@executes 1
        },
    )
}