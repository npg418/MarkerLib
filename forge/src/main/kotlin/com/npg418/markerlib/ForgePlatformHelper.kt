package com.npg418.markerlib

import com.npg418.markerlib.marker.shared.MarkerPacket
import com.npg418.markerlib.network.MarkerNetwork
import net.minecraft.server.level.ServerPlayer
import net.minecraftforge.fml.ModList
import net.minecraftforge.fml.loading.FMLLoader

class ForgePlatformHelper : PlatformHelper {
    override val isDevEnvironment: Boolean get() = !FMLLoader.isProduction()

    override fun isModLoaded(id: String): Boolean = ModList.get().isLoaded(id)

    override fun sendPacket(player: ServerPlayer, packet: MarkerPacket) = MarkerNetwork.send(player, packet)
}