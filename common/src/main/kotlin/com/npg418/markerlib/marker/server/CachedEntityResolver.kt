package com.npg418.markerlib.marker.server

import com.mojang.brigadier.StringReader
import net.minecraft.commands.arguments.EntityArgument
import net.minecraft.commands.arguments.selector.EntitySelector
import net.minecraft.server.MinecraftServer
import java.util.*

class CachedEntityResolver internal constructor(val server: MinecraftServer, val playerOnly: Boolean) {
    private val parsed = HashMap<String, EntitySelector>()
    private val resolved = HashMap<String, Set<UUID>?>()

    fun invalidate() = resolved.clear()

    fun resolve(selector: String): Set<UUID>? {
        if (resolved.containsKey(selector)) return resolved[selector]
        return evaluate(selector).also { resolved[selector] = it }
    }

    private fun evaluate(selector: String): Set<UUID>? {
        val s = parsed[selector] ?: parse(selector)?.also { parsed[selector] = it } ?: return null
        return runCatching {
            val source = server.createCommandSourceStack()
            (if (playerOnly) s.findPlayers(source) else s.findEntities(source)).map { it.uuid }.toSet()
        }.getOrNull()
    }

    private fun parse(selector: String): EntitySelector? = runCatching {
        val argument = if (playerOnly) EntityArgument.players() else EntityArgument.entities()
        argument.parse(StringReader(selector))
    }.getOrNull()
}