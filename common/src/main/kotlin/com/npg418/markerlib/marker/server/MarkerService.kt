package com.npg418.markerlib.marker.server

import com.npg418.markerlib.marker.common.Marker
import com.npg418.markerlib.marker.common.network.AddPacket
import com.npg418.markerlib.marker.common.network.PacketSender
import com.npg418.markerlib.marker.common.network.RemovePacket
import com.npg418.markerlib.marker.common.network.SyncPacket
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer

class MarkerService(private val sender: PacketSender) {
    fun syncTo(player: ServerPlayer) {
        val visible = ServerMarkers.get(player.server).all.filter { it.isVisibleTo(player.uuid) }
        sender.send(player, SyncPacket(visible))
    }

    fun add(server: MinecraftServer, marker: Marker) {
        ServerMarkers.get(server).put(marker)
        server.playerList.players.forEach {
            sender.send(it, if (marker.isVisibleTo(it.uuid)) AddPacket(marker) else RemovePacket(marker.id))
        }
    }

    fun remove(server: MinecraftServer, id: String): Boolean {
        if (ServerMarkers.get(server).remove(id) == null) return false
        server.playerList.players.forEach { sender.send(it, RemovePacket(id)) }
        return true
    }
}