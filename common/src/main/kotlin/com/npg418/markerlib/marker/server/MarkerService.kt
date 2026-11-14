package com.npg418.markerlib.marker.server

import com.npg418.markerlib.Platform
import com.npg418.markerlib.config.ServerConfig
import com.npg418.markerlib.marker.shared.AddPacket
import com.npg418.markerlib.marker.shared.RemovePacket
import com.npg418.markerlib.marker.shared.SyncPacket
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer
import java.util.*

class MarkerService private constructor(val server: MinecraftServer) {
    private val markers = ServerMarkers.of(server)

    private val entityResolver = CachedEntityResolver(server, false)
    private val playerResolver = CachedEntityResolver(server, true)

    private val lastSent = HashMap<String, Set<UUID>>()

    private var dirty = false
    private var lastRefresh = 0

    private fun ServerEntry.isVisibleTo(player: ServerPlayer): Boolean {
        val selector = visibleTo ?: return true
        return player.uuid in (playerResolver.resolve(selector) ?: emptySet())
    }

    private fun ServerEntry.resolveEntities(): ServerEntry {
        val selector = entitySelector ?: return this
        val found = entityResolver.resolve(selector) ?: return this
        return if (found == marker.entities) this else copy(marker = marker.copy(entities = found))
    }

    fun syncTo(player: ServerPlayer) {
        playerResolver.invalidate()
        val visible = markers.all.filter { it.isVisibleTo(player) }.map { it.marker }
        Platform.sendPacket(player, SyncPacket(visible))
    }

    operator fun get(id: String) = markers[id]
    fun put(entry: ServerEntry) {
        val resolved = entry.resolveEntities()
        markers.put(resolved)
        val now = resolved.visibleTo?.let { playerResolver.resolve(it) }
        if (now != null) lastSent[resolved.id] = now else lastSent.remove(resolved.id)
        server.playerList.players.forEach {
            Platform.sendPacket(
                it,
                if (entry.isVisibleTo(it)) AddPacket(resolved.marker) else RemovePacket(resolved.id),
            )
        }
    }

    val all: Collection<ServerEntry> = markers.all

    fun remove(id: String): Boolean {
        markers.remove(id) ?: return false
        lastSent.remove(id)
        server.playerList.players.forEach { Platform.sendPacket(it, RemovePacket(id)) }
        return true
    }

    fun clear() {
        markers.clear()
        lastSent.clear()
        server.playerList.players.forEach { Platform.sendPacket(it, SyncPacket(emptyList())) }
    }

    fun markDirty() {
        dirty = true
    }

    fun tick() {
        val now = server.tickCount
        val periodic = now % ServerConfig.EntitySearch.interval == 0
        val onDemand = dirty && now - lastRefresh >= ServerConfig.EntitySearch.minGap
        if (!periodic && !onDemand) return
        dirty = false
        lastRefresh = now
        playerResolver.invalidate()
        entityResolver.invalidate()
        markers.all.toList().forEach {
            refreshVisibleTo(it)
            refreshEntities(it)
        }
    }

    fun refreshVisibleTo(entry: ServerEntry) {
        val selector = entry.visibleTo ?: return
        val now = playerResolver.resolve(selector) ?: return
        val old = lastSent.put(entry.id, now) ?: return
        (now - old).forEach {
            server.playerList.getPlayer(it)?.let { p -> Platform.sendPacket(p, AddPacket(entry.marker)) }
        }
        (old - now).forEach {
            server.playerList.getPlayer(it)?.let { p -> Platform.sendPacket(p, RemovePacket(entry.id)) }
        }
    }

    fun refreshEntities(entry: ServerEntry) {
        val selector = entry.entitySelector ?: return
        val found = entityResolver.resolve(selector) ?: return
        val updated = entry.copy(marker = entry.marker.copy(entities = found))
        markers.put(updated)
        server.playerList.players.forEach {
            if (updated.isVisibleTo(it)) Platform.sendPacket(it, AddPacket(updated.marker))
        }
    }

    private fun onServerStopped() {
        lastSent.clear()
        playerResolver.invalidate()
        entityResolver.invalidate()
        dirty = false
    }

    companion object {
        private var instance: MarkerService? = null

        fun of(server: MinecraftServer): MarkerService =
            instance?.takeIf { it.server === server } ?: MarkerService(server).also { instance = it }

        fun release() {
            instance?.onServerStopped()
            instance = null
        }
    }
}