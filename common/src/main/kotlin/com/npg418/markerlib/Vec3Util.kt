package com.npg418.markerlib

import net.minecraft.network.FriendlyByteBuf
import net.minecraft.world.phys.Vec3
import java.math.BigDecimal
import java.math.RoundingMode

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

fun Vec3.format(scale: Int = 3) = "${x.num(scale)} ${y.num(scale)} ${z.num(scale)}"

private fun Double.num(scale: Int) = BigDecimal.valueOf(this).setScale(scale, RoundingMode.HALF_UP).toPlainString()

private const val VEC3_EPSILON = 0.001
fun Vec3.isClose(other: Vec3, epsilon: Double = VEC3_EPSILON) = distanceToSqr(other) <= epsilon * epsilon