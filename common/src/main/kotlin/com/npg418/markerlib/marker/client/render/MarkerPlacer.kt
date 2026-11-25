package com.npg418.markerlib.marker.client.render

import com.npg418.markerlib.marker.common.Marker
import net.minecraft.client.gui.Font
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.ClipContext
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3

internal class MarkPlacer(private val screen: Screen, private val viewer: Player, private val font: Font) {
    fun place(marker: Marker, target: Vec3): Placement? {
        val p = Camera.project(target, screen)
        val hasArrow = marker.icon.arrowTexture != null && !marker.hideByBlock
        val fitsScreen = !hasArrow || screen.contains(p.x, p.y)

        return when {
            !p.behind && fitsScreen && isVisible(marker, target) -> Placement.OnScreen(p.x, p.y)

            hasArrow -> {
                val reach = MarkMetrics.labelReach(marker, font)
                screen.edgePlacement(
                    p.x,
                    p.y,
                    iconHalf = MarkMetrics.iconSize(marker) / 2f,
                    labelAbove = if (marker.topText != null) reach else 0f,
                    labelBelow = if (marker.bottomText != null) reach else 0f,
                )
            }

            else -> null
        }
    }

    private fun isVisible(marker: Marker, target: Vec3) = !marker.hideByBlock || !isBlocked(target)

    private fun isBlocked(target: Vec3): Boolean {
        val hit = viewer.level().clip(
            ClipContext(Camera.position, target, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, viewer),
        )
        return hit.type != HitResult.Type.MISS && hit.blockPos != BlockPos.containing(target)
    }
}