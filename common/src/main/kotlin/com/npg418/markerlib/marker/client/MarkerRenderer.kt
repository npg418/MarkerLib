package com.npg418.markerlib.marker.client

import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import com.npg418.markerlib.config.ClientConfig
import com.npg418.markerlib.marker.common.Marker
import com.npg418.markerlib.marker.format
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.core.BlockPos
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.ClipContext
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import org.joml.Matrix4f
import org.joml.Vector4f
import kotlin.math.*

object MarkerRenderer {
    private class RenderItem(
        val sx: Float,
        val sy: Float,
        val pos: Vec3,
        val dist: Double,
        val atlas: ResourceLocation,
        val marker: Marker,
        val entity: Entity? = null,
        val angle: Float? = null,
    ) {
        fun expand(template: String) = TOKEN.replace(template) {
            when (it.groupValues[1]) {
                "name" -> entity?.name?.string ?: it.value
                "dist" -> dist.roundToInt().toString()
                "pos" -> pos.format(0)
                "position" -> pos.format()
                else -> it.value
            }
        }

        companion object {
            private val TOKEN = Regex("""\{(\w+)}""")
        }
    }

    private const val EPS = 1e-6f

    private val viewProj = Matrix4f()
    private var zoomFactor = 1f

    fun captureMatrices(projection: Matrix4f, view: Matrix4f) {
        viewProj.set(projection).mul(view)
        val baseFov = Math.toRadians(Minecraft.getInstance().options.fov().get().toDouble())
        zoomFactor = (projection.m11() * tan(baseFov / 2.0)).toFloat()
    }

    fun render(g: GuiGraphics, markers: Collection<Marker>, partialTicks: Float) {
        val mc = Minecraft.getInstance()
        val player = mc.player ?: return
        val level = mc.level ?: return
        if (mc.options.hideGui) return

        val cam = mc.gameRenderer.mainCamera.position
        val w = g.guiWidth().toFloat()
        val h = g.guiHeight().toFloat()
        val margin = ClientConfig.edgeMargin
        val bottom = margin + ClientConfig.extraBottomMargin

        val ctx = ResolveContext(level, markers)
        val items = buildList {
            for (marker in markers) {
                for ((pos, entity) in marker.positions.map { it to null } + ctx.resolve(marker, partialTicks)) {
                    val renderPos =
                        pos.add(0.0, (entity?.bbHeight?.toDouble() ?: 0.0) + marker.icon.entityIconOffset, 0.0)
                    val d = renderPos.subtract(cam)
                    val v = Vector4f(d.x.toFloat(), d.y.toFloat(), d.z.toFloat(), 1f).mul(viewProj)
                    val behind = v.w < 0f
                    val iw = 1f / (if (abs(v.w) < EPS) EPS else v.w)
                    val flip = if (behind) -1f else 1f
                    val sx = (v.x * iw * flip * 0.5f + 0.5f) * w
                    val sy = (0.5f - v.y * iw * flip * 0.5f) * h

                    val dist = player.getPosition(partialTicks).distanceTo(pos)
                    val arrow = if (marker.hideByBlock) null else marker.icon.arrowTexture

                    val inside = sx in margin..(w - margin) && sy in margin..(h - bottom)
                    if (!behind && (arrow == null || inside) && (!marker.hideByBlock || !isOccluded(
                            player,
                            cam,
                            renderPos,
                        ))
                    ) {
                        add(RenderItem(sx, sy, pos, dist, marker.icon.iconTexture, marker, entity))
                    } else if (arrow != null) {
                        val dx = sx - w / 2f
                        var dy = sy - h / 2f
                        if (abs(dx) < EPS && abs(dy) < EPS) dy = -1f
                        val k = min(
                            (w / 2 - margin) / abs(dx),
                            (h / 2 - (if (dy > 0f) bottom else margin)) / abs(dy),
                        )
                        add(
                            RenderItem(
                                w / 2 + dx * k, h / 2 + dy * k, pos, dist, arrow, marker, entity, atan2(dy, dx)
                            )
                        )
                    }
                }
            }
        }.sortedWith(compareBy<RenderItem> { it.angle == null }.thenByDescending { it.dist })

        items.forEach { draw(g, mc.font, it) }
    }

    private fun isOccluded(player: Player, from: Vec3, to: Vec3): Boolean {
        val hit = player.level().clip(
            ClipContext(from, to, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, player),
        )
        if (hit.type == HitResult.Type.MISS) return false
        return hit.blockPos != BlockPos.containing(to)
    }

    private fun draw(g: GuiGraphics, font: Font, item: RenderItem) = with(item) {
        val edge = angle != null
        val size = ClientConfig.baseIconSize
        val distScale = distanceScale(dist)
        val lift =
            marker.icon.size * distScale * (size / 2f + if (marker.bottomText != null) 2f + font.lineHeight * ClientConfig.Scale.textScale else 0f)
        val color = RGB(0xFFFFFF).lerp(marker.blend, RGB(marker.color))
        val alpha = if (edge) 1f else crosshairAlpha(g, sx, sy - lift)
        val argb = ((alpha * 255f).roundToInt() shl 24) or color.rgb

        g.pose().withStack {
            g.pose().translate(sx, sy, 0f)
            if (!edge) {
                g.pose().translate(0f, -lift, 0f)
                g.pose().scale(distScale, distScale, 1f)
            }
            g.pose().scale(marker.icon.size, marker.icon.size, 1f)

            marker.bottomText?.let {
                drawText(g, font, expand(it), size / 2f + 2f, argb)
            }
            marker.topText?.let {
                drawText(g, font, expand(it), -(size / 2f + 2f), argb, true)
            }

            if (edge) g.pose().mulPose(Axis.ZP.rotation(angle))
            RenderSystem.enableBlend()
            g.setColor(color.r / 255f, color.g / 255f, color.b / 255f, alpha)
            g.pose().withStack {
                g.pose().translate(-size / 2f, -size / 2f, 0f)
                g.blit(atlas, 0, 0, size, size, 0f, 0f, 1, 1, 1, 1)
            }
            g.setColor(1f, 1f, 1f, 1f)
        }
    }

    private fun drawText(
        g: GuiGraphics, font: Font, text: String, y: Float, color: Int, bottomAnchor: Boolean = false
    ) {
        g.pose().withStack {
            g.pose().translate(0f, y, 0f)
            g.pose().scale(ClientConfig.Scale.textScale, ClientConfig.Scale.textScale, 1f)
            g.drawCenteredString(font, text, 0, if (bottomAnchor) -font.lineHeight else 0, color)
        }
    }

    private fun distanceScale(dist: Double): Float = with(ClientConfig.Scale) {
        val base = (refDistance / dist.coerceAtLeast(0.1)).toFloat().coerceIn(minScale, maxScale)
        if (scaleWithZoom) base * zoomFactor.coerceIn(1f, maxZoomScale) else base
    }

    private fun crosshairAlpha(g: GuiGraphics, sx: Float, sy: Float): Float = with(ClientConfig.CrosshairFade) {
        if (radius <= 0f) return 1f
        val d = hypot(sx - g.guiWidth() / 2f, sy - g.guiHeight() / 2f)
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