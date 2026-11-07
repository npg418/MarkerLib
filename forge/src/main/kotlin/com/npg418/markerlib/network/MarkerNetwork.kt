package com.npg418.markerlib.network

import com.npg418.markerlib.MarkerLib
import com.npg418.markerlib.marker.client.ClientMarkers
import com.npg418.markerlib.marker.common.network.AddPacket
import com.npg418.markerlib.marker.common.network.MarkerPacket
import com.npg418.markerlib.marker.common.network.RemovePacket
import com.npg418.markerlib.marker.common.network.SyncPacket
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import net.minecraftforge.network.NetworkDirection
import net.minecraftforge.network.NetworkRegistry
import net.minecraftforge.network.PacketDistributor
import net.minecraftforge.network.simple.SimpleChannel

object MarkerNetwork {
    private const val PROTOCOL = "1"

    val CHANNEL: SimpleChannel = NetworkRegistry.newSimpleChannel(
        ResourceLocation.fromNamespaceAndPath(MarkerLib.MODID, "main"),
        { PROTOCOL },
        PROTOCOL::equals,
        PROTOCOL::equals,
    )

    fun register() {
        CHANNEL.messageBuilder(SyncPacket::class.java, 0, NetworkDirection.PLAY_TO_CLIENT)
            .encoder(SyncPacket::encode)
            .decoder(SyncPacket::decode)
            .consumerMainThread { msg, _ -> ClientMarkers.replaceAll(msg.markers) }
            .add()
        CHANNEL.messageBuilder(AddPacket::class.java, 1, NetworkDirection.PLAY_TO_CLIENT)
            .encoder(AddPacket::encode)
            .decoder(AddPacket::decode)
            .consumerMainThread { msg, _ -> ClientMarkers.put(msg.marker) }
            .add()
        CHANNEL.messageBuilder(RemovePacket::class.java, 2, NetworkDirection.PLAY_TO_CLIENT)
            .encoder(RemovePacket::encode)
            .decoder(RemovePacket::decode)
            .consumerMainThread { msg, _ -> ClientMarkers.remove(msg.id) }
            .add()
    }

    fun send(player: ServerPlayer, packet: MarkerPacket) = CHANNEL.send(
        PacketDistributor.PLAYER.with {
            player
        },
        packet,
    )
}