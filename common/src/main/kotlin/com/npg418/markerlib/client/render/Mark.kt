package com.npg418.markerlib.client.render

import com.npg418.markerlib.common.Marker
import com.npg418.markerlib.common.MarkerIcon
import net.minecraft.world.phys.Vec3
import java.util.*

internal class WorldPoint(val position: Vec3, val uuid: UUID? = null) {
    fun iconPosition(icon: MarkerIcon): Vec3 = if (uuid == null) {
        position
    } else {
        position.add(
            0.0,
            icon.entityIconOffset,
            0.0,
        )
    }
}

internal sealed interface Placement {
    val x: Float
    val y: Float

    class OnScreen(override val x: Float, override val y: Float) : Placement
    class AtEdge(override val x: Float, override val y: Float, val angle: Float) : Placement
}

internal class Mark(val marker: Marker, val target: WorldPoint, val distance: Double, val placement: Placement) {
    val isOnScreen = placement is Placement.OnScreen

    companion object {
        val DRAW_ORDER = Comparator<Mark> { a, b ->
            when {
                a.isOnScreen != b.isOnScreen -> if (a.isOnScreen) 1 else -1
                else -> b.distance.compareTo(a.distance)
            }
        }
    }
}