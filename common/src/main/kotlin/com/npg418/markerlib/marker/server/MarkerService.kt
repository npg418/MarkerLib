package com.npg418.markerlib.marker.server

import com.npg418.markerlib.Platform
import com.npg418.markerlib.config.ServerConfig
import com.npg418.markerlib.marker.shared.AddPacket
import com.npg418.markerlib.marker.shared.MarkerTarget
import com.npg418.markerlib.marker.shared.RemovePacket
import com.npg418.markerlib.marker.shared.SyncPacket
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer
import java.util.*

object MarkerService {
    private val entityResolver = CachedEntityResolver()
    private val playerResolver = CachedEntityResolver(true)

    private val lastSent = HashMap<String, Set<UUID>>()

    private fun ServerMarker.isVisibleTo(player: ServerPlayer): Boolean {
        val selector = visibleTo ?: return true
        return player.uuid in (playerResolver.resolve(player.server, selector) ?: emptySet())
    }

    fun syncTo(player: ServerPlayer) {
        playerResolver.invalidate()
        val visible = ServerMarkers.of(player.server).all.filter { it.isVisibleTo(player) }.map { it.marker }
        Platform.sendPacket(player, SyncPacket(visible))
    }

    fun put(server: MinecraftServer, marker: ServerMarker) {
        ServerMarkers.of(server).put(marker)
        marker.visibleTo?.let { selector ->
            playerResolver.resolve(server, selector)?.let { lastSent[marker.id] = it }
        }
        server.playerList.players.forEach {
            Platform.sendPacket(
                it,
                if (marker.isVisibleTo(it)) AddPacket(marker.marker) else RemovePacket(marker.id),
            )
        }
    }

    fun remove(server: MinecraftServer, id: String): Boolean {
        ServerMarkers.of(server).remove(id) ?: return false
        lastSent.remove(id)
        server.playerList.players.forEach { Platform.sendPacket(it, RemovePacket(id)) }
        return true
    }

    fun clear(server: MinecraftServer) {
        ServerMarkers.of(server).clear()
        lastSent.clear()
        server.playerList.players.forEach { Platform.sendPacket(it, SyncPacket(emptyList())) }
    }

    private var dirty = false
    private var lastRefresh = 0

    fun markDirty() {
        dirty = true
    }

    fun tick(server: MinecraftServer) {
        val now = server.tickCount
        val periodic = now % ServerConfig.EntitySearch.interval == 0
        val onDemand = dirty && now - lastRefresh >= ServerConfig.EntitySearch.minGap
        if (!periodic && !onDemand) return
        dirty = false
        lastRefresh = now
        playerResolver.invalidate()
        entityResolver.invalidate()
        ServerMarkers.of(server).all.toList().forEach {
            refreshVisibleTo(server, it)
            refreshEntities(server, it)
        }
    }

    fun refreshVisibleTo(server: MinecraftServer, marker: ServerMarker) {
        val selector = marker.visibleTo ?: return
        val now = playerResolver.resolve(server, selector) ?: return
        val old = lastSent.put(marker.id, now) ?: return
        (now - old).forEach {
            server.playerList.getPlayer(it)?.let { p -> Platform.sendPacket(p, AddPacket(marker.marker)) }
        }
        (old - now).forEach {
            server.playerList.getPlayer(it)?.let { p -> Platform.sendPacket(p, RemovePacket(marker.id)) }
        }
    }

    fun refreshEntities(server: MinecraftServer, marker: ServerMarker) {
        val selector = marker.entitySelector ?: return
        val found = entityResolver.resolve(server, selector) ?: return
        val target = marker.marker.target as? MarkerTarget.Entities ?: return
        if (found == target.entities) return
        val updated = marker.copy(marker = marker.marker.copy(target = target.copy(entities = found)))
        ServerMarkers.of(server).put(updated)
        server.playerList.players.forEach {
            if (updated.isVisibleTo(it)) Platform.sendPacket(it, AddPacket(updated.marker))
        }
    }

    fun onServerStopped() {
        lastSent.clear()
        playerResolver.invalidate()
        entityResolver.invalidate()
        dirty = false
    }
}