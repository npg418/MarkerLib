package com.npg418.markerlib.marker.server.command

import com.mojang.brigadier.CommandDispatcher
import com.npg418.markerlib.config.ServerConfig
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands.literal

object MarkerCommand {
    fun register(dispatcher: CommandDispatcher<CommandSourceStack>) {
        dispatcher.register(
            literal("marker").requires { it.hasPermission(ServerConfig.MarkerCommand.requiredPermission) }
                .then(CreateCommand.node)
                .then(TargetCommand.node)
                .then(SetCommand.node)
                .then(ManageCommand.removeNode)
                .then(ManageCommand.clearNode)
                .then(ManageCommand.listNode),
        )
    }
}