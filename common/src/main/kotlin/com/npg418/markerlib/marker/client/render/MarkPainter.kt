package com.npg418.markerlib.marker.client.render

import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import com.npg418.markerlib.config.ClientConfig
import com.npg418.markerlib.marker.common.Marker
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.resources.ResourceLocation
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.roundToInt

internal class MarkPainter(private val g: GuiGraphics, private val font: Font, private val screen: Screen) {
    fun paint(mark: Mark) {
        when (val p = mark.placement) {
            is Placement.OnScreen -> paintOnScreen(mark, p)
            is Placement.AtEdge -> paintAtEdge(mark, p)
        }
    }

    private fun paintOnScreen(mark: Mark, p: Placement.OnScreen) {
        val marker = mark.marker
        val distScale = distanceScale(mark.distance)
        val lift = distScale * if (marker.bottomText != null) {
            MarkMetrics.labelReach(marker, font)
        } else {
            MarkMetrics.iconSize(marker) / 2f
        }
        val alpha = crosshairAlpha(p.x, p.y - lift)

        withPose {
            translate(p.x, p.y - lift, 0f)
            scale(distScale, distScale, 1f)
            drawLabels(mark, alpha, clampToScreen = false)
            drawIcon(marker, marker.icon.iconTexture, alpha)
        }
    }

    private fun paintAtEdge(mark: Mark, p: Placement.AtEdge) {
        val marker = mark.marker
        val texture = marker.icon.arrowTexture ?: return
        withPose {
            translate(p.x, p.y, 0f)
            drawLabels(mark, 1f, clampToScreen = true, centerX = p.x)
            drawIcon(marker, texture, 1f, p.angle)
        }
    }

    private fun drawLabels(mark: Mark, alpha: Float, clampToScreen: Boolean, centerX: Float = 0f) {
        val marker = mark.marker
        val color = (((alpha * 255f).roundToInt()) shl 24) or marker.color
        val offset = MarkMetrics.iconSize(marker) / 2f + MarkMetrics.LABEL_GAP

        marker.bottomText?.let {
            drawLabel(LabelTemplate.expand(it, mark), offset, false, color, clampToScreen, centerX)
        }
        marker.topText?.let {
            drawLabel(LabelTemplate.expand(it, mark), -offset, true, color, clampToScreen, centerX)
        }
    }

    private fun drawLabel(
        text: String,
        y: Float,
        aboveY: Boolean,
        color: Int,
        clampToScreen: Boolean,
        centerX: Float,
    ) {
        val textScale = ClientConfig.Scale.textScale
        val x = if (clampToScreen) horizontalShift(text, centerX) else 0f
        withPose {
            translate(x, y, 0f)
            scale(textScale, textScale, 1f)
            g.drawCenteredString(font, text, 0, if (aboveY) -font.lineHeight else 0, color)
        }
    }

    private fun horizontalShift(text: String, centerX: Float): Float {
        val half = font.width(text) * ClientConfig.Scale.textScale / 2f
        val lo = half + ClientConfig.edgeMargin
        val hi = max(lo, screen.width - lo)
        return centerX.coerceIn(lo, hi) - centerX
    }

    private fun drawIcon(marker: Marker, texture: ResourceLocation, alpha: Float, angle: Float = 0f) {
        val size = ClientConfig.baseIconSize
        val tint = RGB(0xFFFFFF).lerp(marker.blend, RGB(marker.color))

        RenderSystem.enableBlend()
        g.setColor(tint.r / 255f, tint.g / 255f, tint.b / 255f, alpha)
        withPose {
            scale(marker.icon.size, marker.icon.size, 1f)
            if (angle != 0f) mulPose(Axis.ZP.rotation(angle))
            translate(-size / 2f, -size / 2f, 0f)
            g.blit(texture, 0, 0, size, size, 0f, 0f, 1, 1, 1, 1)
        }
        g.setColor(1f, 1f, 1f, 1f)
    }

    private fun distanceScale(distance: Double): Float = with(ClientConfig.Scale) {
        val base = (refDistance / distance.coerceAtLeast(0.1)).toFloat().coerceIn(minScale, maxScale)
        if (scaleWithZoom) base * Camera.zoom.coerceIn(1f, maxZoomScale) else base
    }

    private fun crosshairAlpha(x: Float, y: Float): Float = with(ClientConfig.CrosshairFade) {
        if (radius <= 0f) return 1f
        val t = (hypot(x - screen.centerX, y - screen.centerY) / radius).coerceIn(0f, 1f)
        val smooth = t * t * (3f - 2f * t)
        minAlpha + (1f - minAlpha) * smooth
    }

    private inline fun withPose(block: PoseStack.() -> Unit) {
        val pose = g.pose()
        pose.pushPose()
        try {
            pose.block()
        } finally {
            pose.popPose()
        }
    }
}