package com.npg418.markerlib.network

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
    readDouble()
)