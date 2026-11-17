package com.npg418.markerlib.marker.client

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import com.npg418.markerlib.config.ClientConfig
import com.npg418.markerlib.marker.common.Marker
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.resources.ResourceLocation
import org.joml.Matrix4f
import org.joml.Vector4f
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.min
import kotlin.math.roundToInt

object MarkerRenderer {
    private const val EPS = 1e-6f

    private val viewProj = Matrix4f()

    fun captureMatrices(projection: Matrix4f, view: Matrix4f) {
        viewProj.set(projection).mul(view)
    }

    fun render(g: GuiGraphics, markers: Collection<Marker>, partialTick: Float) {
        val mc = Minecraft.getInstance()
        val level = mc.level ?: return
        if (mc.options.hideGui) return

        val cam = mc.gameRenderer.mainCamera.position
        val w = g.guiWidth().toFloat()
        val h = g.guiHeight().toFloat()
        val margin = ClientConfig.Rendering.edgeMargin
        val font = mc.font

        val ctx = ResolveContext(level, markers)
        val items = markers.flatMap { marker ->
            (marker.positions + ctx.getEntityPositions(
                marker, partialTick
            )).map { Triple(it, cam.distanceTo(it), marker) }
        }.sortedByDescending { it.second }

        for ((pos, dist, marker) in items) {
            val d = pos.subtract(cam)
            val v = Vector4f(d.x.toFloat(), d.y.toFloat(), d.z.toFloat(), 1f).mul(viewProj)
            val behind = v.w < 0f
            val iw = 1f / (if (abs(v.w) < EPS) EPS else v.w)
            val flip = if (behind) -1f else 1f
            val sx = (v.x * iw * flip * 0.5f + 0.5f) * w
            val sy = (0.5f - v.y * iw * flip * 0.5f) * h

            val arrow = marker.icon.arrowTexture

            if (!behind && (arrow == null || (sx in margin..(w - margin) && sy in margin..(h - margin)))) {
                drawMarker(g, sx, sy, dist, marker, font)
            } else if (arrow != null) {
                val dx = sx - w / 2
                val dy = sy - h / 2
                val k = min((w / 2 - margin) / abs(dx), (h / 2 - margin) / abs(dy))
                drawArrow(g, w / 2 + dx * k, h / 2 + dx * k, dist, marker, font, arrow, atan2(dx, dy))
            }
        }
    }

    private fun drawMarker(g: GuiGraphics, sx: Float, sy: Float, dist: Double, marker: Marker, font: Font) {
        val size = ClientConfig.Rendering.baseIconSize
        val textH = font.lineHeight
        val scale = marker.icon.size * distanceScale(dist)
        val c = RGB(0xFFFFFF).lerp(marker.blend, RGB(marker.color))

        g.pose().withStack {
            g.pose().translate(sx, sy, 0f)
            if (marker.showDistance) {
                g.pose().translate(0f, -textH / 2f, 0f)
                g.drawCenteredString(font, "${dist.roundToInt()}m", 0, 0, (0xFF shl 24) or c.rgb)
                g.pose().translate(0f, -textH / 2f, 0f)
            }
            g.pose().scale(scale, scale, 0f)
            g.pose().translate(-size / 2f, -size / 2f, 0f)
            g.setColor(c.r / 255f, c.g / 255f, c.b / 255f, 1f)
            g.blit(marker.icon.iconTexture, 0, 0, size, size, 0f, 0f, 1, 1, 1, 1)
            g.setColor(1f, 1f, 1f, 1f)
        }
    }

    private fun drawArrow(
        g: GuiGraphics,
        sx: Float,
        sy: Float,
        dist: Double,
        marker: Marker,
        font: Font,
        arrow: ResourceLocation,
        angle: Float
    ) {
        val size = ClientConfig.Rendering.baseIconSize
        val textH = font.lineHeight
        val scale = marker.icon.size * distanceScale(dist)
        val c = RGB(0xFFFFFF).lerp(marker.blend, RGB(marker.color))

        g.pose().withStack {
            g.pose().translate(sx, sy, 0f)
            if (marker.showDistance) {
                g.pose().translate(0f, -textH / 2f, 0f)
                g.drawCenteredString(font, "${dist.roundToInt()}m", 0, 0, (0xFF shl 24) or c.rgb)
                g.pose().translate(0f, -textH / 2f, 0f)
            }
            g.pose().scale(scale, scale, 0f)
            g.pose().translate(-size / 2f, -size / 2f, 0f)
            g.setColor(c.r / 255f, c.g / 255f, c.b / 255f, 1f)
            g.pose().mulPose(Axis.ZP.rotation(angle))
            g.blit(arrow, 0, 0, size, size, 0f, 0f, 1, 1, 1, 1)
            g.setColor(1f, 1f, 1f, 1f)
        }
    }

    private fun distanceScale(dist: Double): Float = with(ClientConfig.Rendering) {
        (scaleRefDistance / dist.coerceAtLeast(0.1)).toFloat().coerceIn(minScale, maxScale)
    }

    private fun PoseStack.withStack(block: () -> Unit) {
        pushPose()
        block()
        popPose()
    }
}