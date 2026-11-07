package com.npg418.markerlib.marker.client

import com.mojang.math.Axis
import com.npg418.markerlib.config.CommonConfig
import com.npg418.markerlib.marker.common.Marker
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import org.joml.Matrix4f
import org.joml.Vector4f
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.min
import kotlin.math.roundToInt

object MarkerRenderer {
    private val viewProj = Matrix4f()
    private const val MARGIN = 20f

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
        for (m in markers) {
            val pos = m.target.resolve(ctx, partialTick) ?: continue
            val dist = cam.distanceTo(pos)

            val v =
                Vector4f((pos.x - cam.x).toFloat(), (pos.y - cam.y).toFloat(), (pos.z - cam.z).toFloat(), 1f).mul(
                    viewProj,
                )

            val behind = v.w <= 0f
            var nx = v.x / v.w
            var ny = v.y / v.w
            if (behind) {
                nx = -nx
                ny = -ny
            }

            val sx = (nx * 0.5f + 0.5f) * w
            val sy = (0.5f - ny * 0.5f) * h

            val onScreen = !behind && sx in MARGIN..(w - MARGIN) && sy in MARGIN..(h - MARGIN)
            if (onScreen) drawIcon(g, m, sx, sy, dist) else drawEdge(g, m, sx, sy, w, h)
        }
    }

    private fun drawIcon(g: GuiGraphics, m: Marker, x: Float, y: Float, dist: Double) {
        val s = (CommonConfig.Rendering.baseIconSize * m.size).roundToInt().coerceAtLeast(1)
        val font = Minecraft.getInstance().font
        g.pose().pushPose()
        g.pose().translate(x, y, 0f)
        tint(g, m.color) {
            g.blit(m.icon, -s / 2, -s / 2, s, s, 0f, 0f, 1, 1, 1, 1)
        }
        g.drawCenteredString(font, "${dist.roundToInt()}m", 0, 10, m.color)
        g.pose().popPose()
    }

    private fun drawEdge(g: GuiGraphics, m: Marker, sx: Float, sy: Float, w: Float, h: Float) {
        val cx = w / 2
        val cy = h / 2
        val dx = sx - cx
        val dy = sy - cy
        val k = min((cx - MARGIN) / abs(dx), (cy - MARGIN) / abs(dy))
        val s = (CommonConfig.Rendering.baseIconSize * m.size).roundToInt().coerceAtLeast(1)
        g.pose().pushPose()
        g.pose().translate(cx + dx * k, cy + dy * k, 0f)
        g.pose().mulPose(Axis.ZP.rotation(atan2(dy, dx)))
        tint(g, m.color) {
            g.blit(m.arrowIcon, -s / 2, -s / 2, s, s, 0f, 0f, 1, 1, 1, 1)
        }
        g.pose().popPose()
    }

    private fun tint(g: GuiGraphics, argb: Int, block: () -> Unit) {
        val a = (argb ushr 24 and 0xFF) / 255f
        val r = (argb shr 16 and 0xFF) / 255f
        val gr = (argb shr 8 and 0xFF) / 255f
        val b = (argb and 0xFF) / 255f
        g.setColor(r, gr, b, a)
        block()
        g.setColor(1f, 1f, 1f, 1f)
    }
}