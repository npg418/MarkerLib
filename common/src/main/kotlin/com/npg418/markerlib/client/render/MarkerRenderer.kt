package com.npg418.markerlib.client.render

import com.npg418.markerlib.common.Marker
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import org.joml.Matrix4f

object MarkerRenderer {
    private val points = mutableListOf<WorldPoint>()
    private val marks = mutableListOf<Mark>()

    fun captureMatrices(projection: Matrix4f, view: Matrix4f) = Camera.capture(projection, view)

    fun render(g: GuiGraphics, markers: Collection<Marker>, partialTicks: Float) {
        val mc = Minecraft.getInstance()
        val player = mc.player ?: return
        val level = mc.level ?: return
        if (mc.options.hideGui) return

        val screen = Screen.of(g)
        MarkerLocator.beginFrame(level, player.uuid, partialTicks)

        collect(markers, MarkPlacer(screen, player, mc.font))
        marks.sortWith(Mark.DRAW_ORDER)

        val painter = MarkPainter(g, mc.font, screen)
        for (i in marks.indices) painter.paint(marks[i])
        marks.clear()
    }

    private fun collect(markers: Collection<Marker>, placer: MarkPlacer) {
        marks.clear()
        val cameraPos = Camera.position
        for (marker in markers) {
            points.clear()
            MarkerLocator.locate(marker, points)
            for (i in points.indices) {
                val point = points[i]
                val placement = placer.place(marker, point.iconPosition(marker.icon)) ?: continue
                marks += Mark(marker, point, cameraPos.distanceTo(point.position), placement)
            }
        }
    }
}