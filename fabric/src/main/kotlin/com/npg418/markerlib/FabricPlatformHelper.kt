package com.npg418.markerlib

import com.npg418.markerlib.marker.common.network.MarkerPacket
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.server.level.ServerPlayer

class FabricPlatformHelper : PlatformHelper {
    override val isDevEnvironment: Boolean get() = FabricLoader.getInstance().isDevelopmentEnvironment

    override fun isModLoaded(id: String): Boolean = FabricLoader.getInstance().isModLoaded(id)

    override fun sendPacket(
        player: ServerPlayer,
        packet: MarkerPacket
    ) {
        TODO("Not yet implemented")
    }
}