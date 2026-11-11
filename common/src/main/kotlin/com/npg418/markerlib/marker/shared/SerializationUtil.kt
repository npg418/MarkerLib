package com.npg418.markerlib.marker.shared

import net.minecraft.nbt.CompoundTag
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.world.phys.Vec3

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

fun Vec3.toTag() = CompoundTag().apply {
    putDouble("x", x)
    putDouble("y", y)
    putDouble("z", z)
}

fun CompoundTag.toVec3() = Vec3(
    getDouble("x"),
    getDouble("y"),
    getDouble("z"),
)

inline fun <T> CompoundTag.getNullable(key: String, getter: CompoundTag.(String) -> T): T? =
    if (contains(key)) getter(key) else null

inline fun <T : Any> CompoundTag.putNullable(key: String, value: T?, putter: CompoundTag.(String, T) -> Unit) {
    if (value != null) putter(key, value)
}