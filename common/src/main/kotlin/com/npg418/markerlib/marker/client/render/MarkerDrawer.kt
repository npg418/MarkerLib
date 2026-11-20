package com.npg418.markerlib.marker.client.render

import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import com.npg418.markerlib.config.ClientConfig
import com.npg418.markerlib.marker.client.RGB
import com.npg418.markerlib.marker.common.Marker
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphics
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.roundToInt

internal object MarkerDrawer {
    private const val TEXT_GAP = 2f

    fun reach(marker: Marker, font: Font): Float =
        marker.icon.size * (ClientConfig.baseIconSize / 2f + TEXT_GAP + font.lineHeight * ClientConfig.Scale.textScale)

    fun draw(g: GuiGraphics, font: Font, item: RenderItem) {
        val marker = item.marker
        val p = item.placement
        val onScreen = p is Placement.Icon
        val size = ClientConfig.baseIconSize
        val iconScale = marker.icon.size

        val distScale = if (onScreen) distanceScale(item.dist) else 1f
        val lift = if (onScreen) {
            distScale * if (marker.bottomText != null) reach(marker, font) else iconScale * size / 2f
        } else {
            0f
        }

        val color = RGB(0xFFFFFF).lerp(marker.blend, RGB(marker.color))
        val alpha = if (onScreen) crosshairAlpha(g, p.x, p.y - lift) else 1f
        val argb = ((alpha * 255f).roundToInt() shl 24) or color.rgb

        g.pose().withStack {
            g.pose().translate(p.x, p.y, 0f)
            if (onScreen) {
                g.pose().translate(0f, -lift, 0f)
                g.pose().scale(distScale, distScale, 1f)
            }
            g.pose().scale(iconScale, iconScale, 1f)

            marker.bottomText?.let {
                val t = LabelTemplate.expand(it, item)
                drawText(g, font, t, edgeShift(g, font, item, t), size / 2f + TEXT_GAP, argb, anchorBottom = false)
            }
            marker.topText?.let {
                val t = LabelTemplate.expand(it, item)
                drawText(g, font, t, edgeShift(g, font, item, t), -(size / 2f + TEXT_GAP), argb, anchorBottom = true)
            }

            if (p is Placement.Arrow) g.pose().mulPose(Axis.ZP.rotation(p.angle))
            drawIcon(g, p, color, alpha, size)
        }
    }

    private fun drawIcon(g: GuiGraphics, p: Placement, color: RGB, alpha: Float, size: Int) {
        RenderSystem.enableBlend()
        g.setColor(color.r / 255f, color.g / 255f, color.b / 255f, alpha)
        g.pose().withStack {
            g.pose().translate(-size / 2f, -size / 2f, 0f)
            g.blit(p.texture, 0, 0, size, size, 0f, 0f, 1, 1, 1, 1)
        }
        g.setColor(1f, 1f, 1f, 1f)
    }

    private fun drawText(
        g: GuiGraphics,
        font: Font,
        text: String,
        x: Float,
        y: Float,
        color: Int,
        anchorBottom: Boolean,
    ) {
        val s = ClientConfig.Scale.textScale
        g.pose().withStack {
            g.pose().translate(x, y, 0f)
            g.pose().scale(s, s, 1f)
            g.drawCenteredString(font, text, 0, if (anchorBottom) -font.lineHeight else 0, color)
        }
    }

    private fun edgeShift(g: GuiGraphics, font: Font, item: RenderItem, text: String): Float {
        val p = item.placement as? Placement.Arrow ?: return 0f
        val iconScale = item.marker.icon.size
        val half = font.width(text) * ClientConfig.Scale.textScale * iconScale / 2f
        val lo = half + ClientConfig.edgeMargin
        val hi = max(lo, g.guiWidth() - lo)
        return (p.x.coerceIn(lo, hi) - p.x) / iconScale
    }

    private fun distanceScale(dist: Double): Float = with(ClientConfig.Scale) {
        val base = (refDistance / dist.coerceAtLeast(0.1)).toFloat().coerceIn(minScale, maxScale)
        if (scaleWithZoom) base * Projector.zoomFactor.coerceIn(1f, maxZoomScale) else base
    }

    private fun crosshairAlpha(g: GuiGraphics, x: Float, y: Float): Float = with(ClientConfig.CrosshairFade) {
        if (radius <= 0f) return 1f
        val d = hypot(x - g.guiWidth() / 2f, y - g.guiHeight() / 2f)
        val t = (d / radius).coerceIn(0f, 1f)
        val s = t * t * (3f - 2f * t)
        minAlpha + (1f - minAlpha) * s
    }

    private inline fun PoseStack.withStack(block: () -> Unit) {
        pushPose()
        try {
            block()
        } finally {
            popPose()
        }
    }
}