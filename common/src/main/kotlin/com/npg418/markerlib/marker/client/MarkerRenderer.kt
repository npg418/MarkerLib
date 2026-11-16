package com.npg418.markerlib.marker.client

import com.mojang.math.Axis
import com.npg418.markerlib.config.ClientConfig
import com.npg418.markerlib.marker.common.Marker
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
    private class RenderingItem(
        val marker: Marker,
        val x: Float,
        val y: Float,
        val dist: Double,
        val edge: Boolean,
        val angle: Float,
    )

    private val viewProj = Matrix4f()

    private const val EPS = 1e-6f

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

        val items = mutableListOf<RenderingItem>()
        val ctx = ResolveContext(level, markers)
        for (marker in markers) {
            for (pos in marker.positions + ctx.getEntityPositions(marker, partialTick)) {
                val v = Vector4f((pos.x - cam.x).toFloat(), (pos.y - cam.y).toFloat(), (pos.z - cam.z).toFloat(), 1f)
                    .mul(viewProj)
                val behind = v.w <= 0f
                val iw = 1f / (if (abs(v.w) < EPS) EPS else v.w)
                val flip = if (behind) -1f else 1f
                val sx = (v.x * iw * flip * 0.5f + 0.5f) * w
                val sy = (0.5f - v.y * iw * flip * 0.5f) * h
                val dist = cam.distanceTo(pos)

                val arrow = marker.icon.arrowTexture
                if (!behind && (arrow == null || (sx in margin..(w - margin) && sy in margin..(h - margin)))) {
                    items += RenderingItem(marker, sx, sy, dist, false, 0f)
                } else if (arrow != null) {
                    val dx = sx - w / 2
                    var dy = sy - h / 2
                    if (abs(dx) < EPS && abs(dy) < EPS) dy = -1f
                    val k = min((w / 2 - margin) / abs(dx), (h / 2 - margin) / abs(dy))
                    items += RenderingItem(marker, w / 2 + dx * k, h / 2 + dy * k, dist, true, atan2(dy, dx))
                }
            }
        }

        items.sortWith(compareBy<RenderingItem> { !it.edge }.thenByDescending { it.dist })
        items.forEach { draw(g, it, mc.font) }
    }

    private fun draw(g: GuiGraphics, item: RenderingItem, font: Font) {
        val m = item.marker
        val s = (ClientConfig.Rendering.baseIconSize * m.icon.size).roundToInt().coerceAtLeast(1)
        val scale = if (item.edge) 1f else distanceScale(item.dist)

        fun mix(c: Int) = (255f + (c - 255f) * m.blend).roundToInt().coerceIn(0, 255)
        val red = mix(m.color shr 16 and 0xFF)
        val green = mix(m.color shr 8 and 0xFF)
        val blue = mix(m.color and 0xFF)

        val iconH = s * scale
        val ts = ClientConfig.Rendering.textScale * scale
        val textH = if (m.showDistance) font.lineHeight * ts + 2f else 0f
        val lift = if (item.edge) 0f else iconH / 2f + textH

        g.pose().pushPose()
        g.pose().translate(item.x, item.y - lift, 0f)
        if (m.showDistance) {
            val argb = (0xFF shl 24) or (red shl 16) or (green shl 8) or blue
            g.pose().pushPose()
            g.pose().translate(0f, iconH / 2f + 2f, 0f)
            g.pose().scale(ts, ts, 1f)
            g.drawCenteredString(font, "${item.dist.roundToInt()}m", 0, 0, argb)
            g.pose().popPose()
        }
        g.pose().translate(0f, 0f, 0.5f)
        g.pose().scale(scale, scale, 1f)
        if (item.edge) g.pose().mulPose(Axis.ZP.rotation(item.angle))
        g.setColor(red / 255f, green / 255f, blue / 255f, 1f)
        g.blit(if (item.edge) m.icon.arrowTexture!! else m.icon.iconTexture, -s / 2, -s / 2, s, s, 0f, 0f, 1, 1, 1, 1)
        g.setColor(1f, 1f, 1f, 1f)
        g.pose().popPose()
    }

    private fun distanceScale(dist: Double): Float = with(ClientConfig.Rendering) {
        (scaleRefDistance / dist.coerceAtLeast(0.1)).toFloat().coerceIn(minScale, maxScale)
    }
}