package com.npg418.markerlib.marker.shared

import com.mojang.serialization.Codec
import com.mojang.serialization.MapCodec
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.world.phys.Vec3
import java.util.*
import kotlin.jvm.optionals.getOrNull

fun FriendlyByteBuf.writeVec3(v: Vec3): FriendlyByteBuf {
    writeDouble(v.x)
    writeDouble(v.y)
    writeDouble(v.z)
    return this
}

fun FriendlyByteBuf.readVec3(): Vec3 = Vec3(
    readDouble(),
    readDouble(),
    readDouble(),
)

fun <T : Any> Codec<T>.nullableFieldOf(name: String): MapCodec<T?> =
    optionalFieldOf(name).xmap({ it.getOrNull() }, { Optional.ofNullable(it) })