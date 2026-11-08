package com.npg418.markerlib.marker.server

import com.mojang.brigadier.StringReader
import com.npg418.markerlib.marker.common.Marker
import com.npg418.markerlib.marker.common.network.AddPacket
import com.npg418.markerlib.marker.common.network.PacketSender
import com.npg418.markerlib.marker.common.network.RemovePacket
import com.npg418.markerlib.marker.common.network.SyncPacket
import net.minecraft.commands.arguments.EntityArgument
import net.minecraft.commands.arguments.selector.EntitySelector
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer
import java.util.*

class MarkerService(private val sender: PacketSender) {
    private val parsed = HashMap<String, EntitySelector>()

    fun evaluate(server: MinecraftServer, selector: String): Set<UUID>? = runCatching {
        parsed.getOrPut(selector) { EntityArgument.entities().parse(StringReader(selector)) }
            .findEntities(server.createCommandSourceStack())
            .map { it.uuid }.toSet()
    }.getOrNull()

    fun refresh(server: MinecraftServer, marker: Marker) {
        val target = marker.target as? Marker.Target.Entity ?: return
        if (target.selector.isEmpty()) return
        val uuids = evaluate(server, target.selector) ?: return
        if (uuids == target.uuids) return
        add(server, marker.copy(target = target.copy(uuids = uuids)))
    }

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

    fun clear(server: MinecraftServer) {
        ServerMarkers.get(server).clear()
        server.playerList.players.forEach { sender.send(it, SyncPacket(emptyList())) }
    }
}