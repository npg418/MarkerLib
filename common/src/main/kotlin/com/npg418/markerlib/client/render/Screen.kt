package com.npg418.markerlib.client.render

import com.npg418.markerlib.config.ClientConfig
import net.minecraft.client.gui.GuiGraphics
import kotlin.math.*

internal class Screen(val width: Float, val height: Float, private val margin: Float, extraBottomMargin: Float) {
    private val bottomMargin = margin + extraBottomMargin

    val centerX get() = width / 2f
    val centerY get() = height / 2f

    fun contains(x: Float, y: Float) = x in margin..(width - margin) && y in margin..(height - bottomMargin)

    fun edgePlacement(x: Float, y: Float, iconHalf: Float, labelAbove: Float, labelBelow: Float): Placement.AtEdge {
        val dx = x - centerX
        var dy = y - centerY
        if (abs(dx) < EPS && abs(dy) < EPS) dy = -1f
        val angle = atan2(dy, dx)

        val extent = iconHalf * (abs(cos(angle)) + abs(sin(angle)))
        val inset = if (dy > 0f) bottomMargin + max(extent, labelBelow) else margin + max(extent, labelAbove)
        val k = min(
            fit((centerX - margin - extent).coerceAtLeast(0f), abs(dx)),
            fit((centerY - inset).coerceAtLeast(0f), abs(dy)),
        )
        return Placement.AtEdge(centerX + dx * k, centerY + dy * k, angle)
    }

    private fun fit(room: Float, delta: Float) = if (delta < EPS) Float.POSITIVE_INFINITY else room / delta

    companion object {
        private const val EPS = 1e-6f

        fun of(g: GuiGraphics) = Screen(
            g.guiWidth().toFloat(),
            g.guiHeight().toFloat(),
            ClientConfig.edgeMargin,
            ClientConfig.extraBottomMargin,
        )
    }
}