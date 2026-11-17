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
    private class RenderItem(
        val sx: Float,
        val sy: Float,
        val dist: Double,
        val atlas: ResourceLocation,
        val marker: Marker,
        val angle: Float? = null,
    )

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
        val bottom = margin + ClientConfig.Rendering.extraBottomMargin

        val ctx = ResolveContext(level, markers)
        val items = buildList {
            for (marker in markers) {
                for (pos in marker.positions + ctx.getEntityPositions(marker, partialTick)) {
                    val d = pos.subtract(cam)
                    val v = Vector4f(d.x.toFloat(), d.y.toFloat(), d.z.toFloat(), 1f).mul(viewProj)
                    val behind = v.w < 0f
                    val iw = 1f / (if (abs(v.w) < EPS) EPS else v.w)
                    val flip = if (behind) -1f else 1f
                    val sx = (v.x * iw * flip * 0.5f + 0.5f) * w
                    val sy = (0.5f - v.y * iw * flip * 0.5f) * h

                    val dist = cam.distanceTo(pos)
                    val arrow = marker.icon.arrowTexture

                    val inside = sx in margin..(w - margin) && sy in margin..(h - bottom)
                    if (!behind && (arrow == null || inside)) {
                        add(RenderItem(sx, sy, dist, marker.icon.iconTexture, marker))
                    } else if (arrow != null) {
                        val dx = sx - w / 2f
                        var dy = sy - h / 2f
                        if (abs(dx) < EPS && abs(dy) < EPS) dy = -1f
                        val k = min(
                            (w / 2 - margin) / abs(dx),
                            (h / 2 - (if (dy > 0f) bottom else margin)) / abs(dy),
                        )
                        add(RenderItem(w / 2 + dx * k, h / 2 + dy * k, dist, arrow, marker, atan2(dy, dx)))
                    }
                }
            }
        }.sortedWith(compareBy<RenderItem> { it.angle == null }.thenByDescending { it.dist })

        items.forEach { draw(g, mc.font, it) }
    }

    private fun draw(g: GuiGraphics, font: Font, item: RenderItem) = with(item) {
        val color = RGB(0xFFFFFF).lerp(marker.blend, RGB(marker.color))
        val baseSize = ClientConfig.Rendering.baseIconSize
        val markerSize = marker.icon.size
        val distScale = distanceScale(dist)
        val edge = angle != null

        g.pose().withStack {
            g.pose().translate(sx, sy, 0f)
            g.pose().scale(markerSize, markerSize, 1f)
            if (!edge) g.pose().scale(distScale, distScale, 1f)
            if (marker.showDistance) {
                val textScale = ClientConfig.Rendering.textScale
                val textH = font.lineHeight * textScale
                if (!edge) g.pose().translate(0f, -textH, 0f)
                g.pose().withStack {
                    g.pose().scale(textScale, textScale, 1f)
                    g.drawCenteredString(font, "${dist.roundToInt()}m", 0, 0, (0xFF shl 24) or color.rgb)
                }
                g.pose().translate(0f, -(textH / 2f + 2f), 0f)
            }
            if (edge) g.pose().mulPose(Axis.ZP.rotation(angle))
            g.pose().translate(-baseSize / 2f, -baseSize / 2f, 0f)
            g.setColor(color.r / 255f, color.g / 255f, color.b / 255f, 1f)
            g.blit(atlas, 0, 0, baseSize, baseSize, 0f, 0f, 1, 1, 1, 1)
            g.setColor(1f, 1f, 1f, 1f)
        }
    }

    private fun distanceScale(dist: Double): Float = with(ClientConfig.Rendering) {
        (scaleRefDistance / dist.coerceAtLeast(0.1)).toFloat().coerceIn(minScale, maxScale)
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