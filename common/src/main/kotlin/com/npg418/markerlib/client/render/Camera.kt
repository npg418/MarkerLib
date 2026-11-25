package com.npg418.markerlib.client.render

import net.minecraft.client.Minecraft
import net.minecraft.world.phys.Vec3
import org.joml.Matrix4f
import org.joml.Vector4f
import kotlin.math.abs
import kotlin.math.tan

internal class ScreenPoint(val x: Float, val y: Float, val behind: Boolean)

internal object Camera {
    private const val MIN_W = 1e-6f

    private val viewProjection = Matrix4f()
    private val scratch = Vector4f()

    var zoom = 1f
        private set

    val position: Vec3 get() = Minecraft.getInstance().gameRenderer.mainCamera.position

    fun capture(projection: Matrix4f, view: Matrix4f) {
        viewProjection.set(projection).mul(view)
        val baseFov = Math.toRadians(Minecraft.getInstance().options.fov().get().toDouble())
        zoom = (projection.m11() * tan(baseFov / 2.0)).toFloat()
    }

    fun project(world: Vec3, screen: Screen): ScreenPoint {
        val cam = position
        val v = scratch.set((world.x - cam.x).toFloat(), (world.y - cam.y).toFloat(), (world.z - cam.z).toFloat(), 1f)
            .mul(viewProjection)
        val behind = v.w < 0f
        val invW = 1f / (if (abs(v.w) < MIN_W) MIN_W else v.w)
        val flip = if (behind) -1f else 1f
        return ScreenPoint(
            (v.x * invW * flip * 0.5f + 0.5f) * screen.width,
            (0.5f - v.y * invW * flip * 0.5f) * screen.height,
            behind,
        )
    }
}