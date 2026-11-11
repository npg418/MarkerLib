package com.npg418.markerlib.marker.server

import com.npg418.markerlib.Platform
import com.npg418.markerlib.config.ServerConfig
import com.npg418.markerlib.marker.shared.AddPacket
import com.npg418.markerlib.marker.shared.RemovePacket
import com.npg418.markerlib.marker.shared.SyncPacket
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer
import java.util.*

object MarkerService {
    private val entityCache = EntityResolveCache()
    private val playerCache = EntityResolveCache(true)

    private fun invalidateCaches() {
        entityCache.invalidate()
        playerCache.invalidate()
    }

    private val viewers = HashMap<String, Set<UUID>>()

    fun syncTo(player: ServerPlayer) {
        val server = player.server
        invalidateCaches()
        val visible = ServerMarkers.of(server).all.filter { canSee(server, it, player) }.map { it.marker }
        Platform.sendPacket(player, SyncPacket(visible))
    }

    fun put(server: MinecraftServer, marker: ServerMarker) {
        invalidateCaches()
        store(server, marker)
    }

    fun remove(server: MinecraftServer, id: String): Boolean {
        ServerMarkers.of(server).remove(id) ?: return false
        viewers.remove(id)
        server.playerList.players.forEach { Platform.sendPacket(it, RemovePacket(id)) }
        return true
    }

    fun clear(server: MinecraftServer) {
        ServerMarkers.of(server).clear()
        viewers.clear()
        server.playerList.players.forEach { Platform.sendPacket(it, SyncPacket(emptyList())) }
    }

    private fun canSee(server: MinecraftServer, marker: ServerMarker, player: ServerPlayer): Boolean {
        val pred = marker.visibleTo ?: return true
        return player.uuid in (playerCache.get(server, pred) ?: emptySet())
    }

    private fun store(server: MinecraftServer, marker: ServerMarker) {
        ServerMarkers.of(server).put(marker)
        val pred = marker.visibleTo
        if (pred == null) {
            viewers.remove(marker.id)
        } else {
            viewers[marker.id] = playerCache.get(server, pred) ?: emptySet()
        }
        server.playerList.players.forEach {
            Platform.sendPacket(
                it,
                if (canSee(server, marker, it)) AddPacket(marker.marker) else RemovePacket(marker.id),
            )
        }
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
        invalidateCaches()
        ServerMarkers.of(server).all.toList().forEach { refresh(server, it) }
    }

    fun refresh(server: MinecraftServer, marker: ServerMarker) {
        val pred = marker.visibleTo ?: return
        val now = playerCache.get(server, pred) ?: return
        val old = viewers.put(marker.id, now) ?: return
        (now - old).forEach {
            server.playerList.getPlayer(it)?.let { p -> Platform.sendPacket(p, AddPacket(marker.marker)) }
        }
        (old - now).forEach {
            server.playerList.getPlayer(it)?.let { p -> Platform.sendPacket(p, RemovePacket(marker.id)) }
        }
    }

    fun onServerStopped() {
        viewers.clear()
        invalidateCaches()
        dirty = false
    }
}