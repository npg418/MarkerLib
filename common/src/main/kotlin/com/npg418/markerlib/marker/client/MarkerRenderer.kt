package com.npg418.markerlib.marker.client

import com.mojang.math.Axis
import com.npg418.markerlib.config.ClientConfig
import com.npg418.markerlib.marker.shared.Marker
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphics
import org.joml.Matrix4f
import org.joml.Vector4f
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.min
import kotlin.math.roundToInt

object MarkerRenderer {
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

        val ctx = ResolveContext(level, markers)
        for (marker in markers) {
            val positions = marker.positions + ctx.getPositions(marker, partialTick)
            for (pos in positions) {
                val dist = cam.distanceTo(pos)

                val v = Vector4f(
                    (pos.x - cam.x).toFloat(),
                    (pos.y - cam.y).toFloat(),
                    (pos.z - cam.z).toFloat(),
                    1f,
                ).mul(viewProj)

                val behind = v.w <= 0f
                var nx = v.x / v.w
                var ny = v.y / v.w
                if (behind) {
                    nx = -nx
                    ny = -ny
                }

                val sx = (nx * 0.5f + 0.5f) * w
                val sy = (0.5f - ny * 0.5f) * h

                val margin = ClientConfig.Rendering.edgeMargin
                val onScreen =
                    !behind &&
                        (marker.icon.arrowTexture == null || (sx in margin..(w - margin) && sy in margin..(h - margin)))
                if (onScreen) {
                    drawIcon(g, marker, sx, sy, dist, mc.font)
                } else {
                    drawEdge(
                        g,
                        marker,
                        sx,
                        sy,
                        w,
                        h,
                        dist,
                        mc.font,
                    )
                }
            }
        }
    }

    private fun drawIcon(g: GuiGraphics, m: Marker, x: Float, y: Float, dist: Double, font: Font) {
        val s = (ClientConfig.Rendering.baseIconSize * m.icon.size).roundToInt().coerceAtLeast(1)
        g.pose().pushPose()
        g.pose().translate(x, y, 0f)
        if (m.showDistance) {
            g.drawCenteredString(font, "${dist.roundToInt()}m", 0, s + 2, blended(m))
        }
        tint(g, m) {
            g.blit(m.icon.iconTexture, -s / 2, -s / 2, s, s, 0f, 0f, 1, 1, 1, 1)
        }
        g.pose().popPose()
    }

    private fun drawEdge(
        g: GuiGraphics,
        m: Marker,
        sx: Float,
        sy: Float,
        w: Float,
        h: Float,
        dist: Double,
        font: Font,
    ) {
        if (m.icon.arrowTexture == null) return
        val cx = w / 2
        val cy = h / 2
        val dx = sx - cx
        val dy = sy - cy
        val k = min(
            (cx - ClientConfig.Rendering.edgeMargin) / abs(dx),
            (cy - ClientConfig.Rendering.edgeMargin) / abs(dy),
        )
        val s = (ClientConfig.Rendering.baseIconSize * m.icon.size).roundToInt().coerceAtLeast(1)
        g.pose().pushPose()
        g.pose().translate(cx + dx * k, cy + dy * k, 0f)
        if (m.showDistance) {
            g.drawCenteredString(font, "${dist.roundToInt()}m", 0, s + 2, blended(m))
        }
        tint(g, m) {
            g.pose().mulPose(Axis.ZP.rotation(atan2(dy, dx)))
            g.blit(m.icon.arrowTexture, -s / 2, -s / 2, s, s, 0f, 0f, 1, 1, 1, 1)
        }
        g.pose().popPose()
    }

    private fun blended(m: Marker): Int {
        fun mix(c: Int): Int = (255f + (c - 255f) * m.blend.coerceIn(0f, 1f)).roundToInt().coerceIn(0, 255)

        val r = mix(m.color shr 16 and 0xFF)
        val g = mix(m.color shr 8 and 0xFF)
        val b = mix(m.color and 0xFF)
        return (0xFF shl 24) or (r shl 16) or (g shl 8) or b
    }

    private fun tint(g: GuiGraphics, m: Marker, block: () -> Unit) {
        val argb = blended(m)
        g.setColor((argb shr 16 and 0xFF) / 255f, (argb shr 8 and 0xFF) / 255f, (argb and 0xFF) / 255f, 1f)
        block()
        g.setColor(1f, 1f, 1f, 1f)
    }
}