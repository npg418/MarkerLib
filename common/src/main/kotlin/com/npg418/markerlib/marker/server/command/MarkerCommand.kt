package com.npg418.markerlib.marker.server.command

import com.mojang.brigadier.CommandDispatcher
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands.literal

object MarkerCommand {
    fun register(dispatcher: CommandDispatcher<CommandSourceStack>) {
        dispatcher.register(
            literal("marker").requires { it.hasPermission(2) }
                .then(CreateCommand.node())
                .then(TargetCommand.node())
                .then(ModifyCommand.node())
                .then(ManageCommands.removeNode())
                .then(ManageCommands.clearNode())
                .then(ManageCommands.listNode()),
        )
    }
}