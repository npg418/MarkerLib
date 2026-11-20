package com.npg418.markerlib.marker.client.render

import net.minecraft.resources.ResourceLocation
import kotlin.math.*

internal sealed interface Placement {
    val x: Float
    val y: Float
    val texture: ResourceLocation

    class Icon(override val x: Float, override val y: Float, override val texture: ResourceLocation) : Placement
    class Arrow(
        override val x: Float,
        override val y: Float,
        override val texture: ResourceLocation,
        val angle: Float,
    ) : Placement
}

internal class ScreenBounds(val w: Float, val h: Float, private val margin: Float, private val bottom: Float) {
    fun contains(x: Float, y: Float) = x in margin..(w - margin) && y in margin..(h - bottom)

    fun toEdge(
        x: Float,
        y: Float,
        texture: ResourceLocation,
        half: Float,
        up: Float = 0f,
        down: Float = 0f,
    ): Placement.Arrow {
        val dx = x - w / 2f
        var dy = y - h / 2f
        if (abs(dx) < EPS && abs(dy) < EPS) dy = -1f
        val angle = atan2(dy, dx)

        val extent = half * (abs(cos(angle)) + abs(sin(angle)))
        val inset = if (dy > 0f) bottom + max(extent, down) else margin + max(extent, up)
        val k = min(
            (w / 2 - margin - extent).coerceAtLeast(0f) / abs(dx),
            (h / 2 - inset).coerceAtLeast(0f) / abs(dy),
        )
        return Placement.Arrow(w / 2 + dx * k, h / 2 + dy * k, texture, angle)
    }
}