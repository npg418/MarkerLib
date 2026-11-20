package com.npg418.markerlib.marker

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

fun Vec3.format(scale: Int = 3, stripZeros: Boolean = true) =
    "${num(x, scale, stripZeros)} ${num(y, scale, stripZeros)} ${num(z, scale, stripZeros)}"

private fun num(d: Double, scale: Int, stripZeros: Boolean) =
    BigDecimal.valueOf(d).setScale(scale, RoundingMode.HALF_UP).let { if (stripZeros) it.stripTrailingZeros() else it }
        .toPlainString()

private const val VEC3_EPSILON = 0.001
fun Vec3.isClose(other: Vec3, epsilon: Double = VEC3_EPSILON) = distanceToSqr(other) <= epsilon * epsilon
