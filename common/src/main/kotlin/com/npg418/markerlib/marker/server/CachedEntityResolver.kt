package com.npg418.markerlib.marker.server

import com.mojang.brigadier.StringReader
import com.npg418.markerlib.marker.common.DimensionalPosition
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.arguments.EntityArgument
import net.minecraft.commands.arguments.selector.EntitySelector
import net.minecraft.server.MinecraftServer
import net.minecraft.world.entity.Entity
import java.util.*

sealed class CachedEntityResolver<R>(val server: MinecraftServer) {
    private val parsed = mutableMapOf<String, EntitySelector>()
    private val results = mutableMapOf<String, R?>()

    fun invalidate() = results.clear()

    abstract fun argument(): EntityArgument
    abstract fun EntitySelector.find(source: CommandSourceStack): List<Entity>
    abstract fun collect(entities: List<Entity>): R

    fun resolve(selector: String): R? = if (results.containsKey(selector)) {
        results[selector]
    } else {
        runCatching {
            val s = parsed.getOrPut(selector) { argument().parse(StringReader(selector)) }
            collect(s.find(server.createCommandSourceStack()))
        }.getOrNull().also { results[selector] = it }
    }

    class Players(server: MinecraftServer) : CachedEntityResolver<Set<UUID>>(server) {
        override fun argument(): EntityArgument = EntityArgument.players()
        override fun EntitySelector.find(source: CommandSourceStack): List<Entity> = findPlayers(source)
        override fun collect(entities: List<Entity>): Set<UUID> = entities.map { it.uuid }.toSet()
    }

    class Entities(server: MinecraftServer) : CachedEntityResolver<Map<UUID, DimensionalPosition>>(server) {
        override fun argument(): EntityArgument = EntityArgument.entities()
        override fun EntitySelector.find(source: CommandSourceStack): List<Entity> = findEntities(source)
        override fun collect(entities: List<Entity>): Map<UUID, DimensionalPosition> = entities.associate {
            it.uuid to DimensionalPosition(
                it.position().add(0.0, it.bbHeight.toDouble(), 0.0),
                it.level().dimension().location(),
            )
        }
    }
}