package com.npg418.markerlib

import com.npg418.markerlib.marker.common.network.MarkerPacket
import net.minecraft.server.level.ServerPlayer
import java.util.*

interface PlatformHelper {
    val isDevEnvironment: Boolean

    fun isModLoaded(id: String): Boolean

    fun sendPacket(player: ServerPlayer, packet: MarkerPacket)
}

object Platform : PlatformHelper by loadService()

inline fun <reified T : Any> loadService(): T =
    ServiceLoader.load(T::class.java, T::class.java.classLoader).firstOrNull()
        ?: error("No implementation of ${T::class.qualifiedName}")