package com.npg418.markerlib.marker.client.render

import net.minecraft.client.Minecraft
import net.minecraft.world.phys.Vec3
import org.joml.Matrix4f
import org.joml.Vector4f
import kotlin.math.abs
import kotlin.math.tan

internal const val EPS = 1e-6f

internal class Projected(val x: Float, val y: Float, val behind: Boolean)

internal object Projector {
    private val viewProj = Matrix4f()
    private val scratch = Vector4f()

    var zoomFactor = 1f
        private set

    fun capture(projection: Matrix4f, view: Matrix4f) {
        viewProj.set(projection).mul(view)
        val baseFov = Math.toRadians(Minecraft.getInstance().options.fov().get().toDouble())
        zoomFactor = (projection.m11() * tan(baseFov / 2.0)).toFloat()
    }

    fun project(world: Vec3, camera: Vec3, w: Float, h: Float): Projected {
        val d = world.subtract(camera)
        val v = scratch.set(d.x.toFloat(), d.y.toFloat(), d.z.toFloat(), 1f).mul(viewProj)
        val behind = v.w < 0f
        val iw = 1f / (if (abs(v.w) < EPS) EPS else v.w)
        val flip = if (behind) -1f else 1f
        return Projected(
            (v.x * iw * flip * 0.5f + 0.5f) * w,
            (0.5f - v.y * iw * flip * 0.5f) * h,
            behind,
        )
    }
}