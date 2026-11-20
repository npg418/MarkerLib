package com.npg418.markerlib.marker.client.render

import com.npg418.markerlib.config.ClientConfig
import com.npg418.markerlib.marker.common.Marker
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.ClipContext
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import org.joml.Matrix4f

internal class RenderItem(
    val marker: Marker,
    val anchor: Anchor,
    val placement: Placement,
    val dist: Double,
)

object MarkerRenderer {
    fun captureMatrices(projection: Matrix4f, view: Matrix4f) = Projector.capture(projection, view)

    fun render(g: GuiGraphics, markers: Collection<Marker>, partialTicks: Float) {
        val mc = Minecraft.getInstance()
        val player = mc.player ?: return
        val level = mc.level ?: return
        if (mc.options.hideGui) return

        val camera = mc.gameRenderer.mainCamera.position
        val margin = ClientConfig.edgeMargin
        val screen = ScreenBounds(
            g.guiWidth().toFloat(), g.guiHeight().toFloat(),
            margin, margin + ClientConfig.extraBottomMargin,
        )
        val resolver = AnchorResolver(level, markers)
        val playerPos = player.getPosition(partialTicks)

        val items = buildList {
            for (marker in markers) {
                for (anchor in resolver.resolve(marker, partialTicks)) {
                    val iconPos = anchor.iconPos(marker.icon)
                    val projected = Projector.project(iconPos, camera, screen.w, screen.h)
                    val placement = place(marker, projected, screen) { isOccluded(player, camera, iconPos) }
                        ?: continue
                    add(RenderItem(marker, anchor, placement, playerPos.distanceTo(anchor.pos)))
                }
            }
        }.sortedWith(
            compareBy<RenderItem> { it.placement is Placement.Icon }.thenByDescending { it.dist },
        )

        items.forEach { MarkerDrawer.draw(g, mc.font, it) }
    }

    private inline fun place(
        marker: Marker,
        p: Projected,
        screen: ScreenBounds,
        occluded: () -> Boolean,
    ): Placement? {
        val icon = marker.icon
        val arrow = if (marker.hideByBlock) null else icon.arrowTexture
        val showIcon = !p.behind &&
            (arrow == null || screen.contains(p.x, p.y)) &&
            (!marker.hideByBlock || !occluded())
        return when {
            showIcon -> Placement.Icon(p.x, p.y, icon.iconTexture)
            arrow != null -> screen.toEdge(p.x, p.y, arrow, ClientConfig.baseIconSize * icon.size / 2f)
            else -> null
        }
    }

    private fun isOccluded(player: Player, from: Vec3, to: Vec3): Boolean {
        val hit = player.level().clip(
            ClipContext(from, to, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, player),
        )
        return hit.type != HitResult.Type.MISS && hit.blockPos != BlockPos.containing(to)
    }
}