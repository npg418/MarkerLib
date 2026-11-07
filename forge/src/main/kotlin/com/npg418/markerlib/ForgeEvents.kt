package com.npg418.markerlib

import com.npg418.markerlib.marker.client.MarkerRenderer
import com.npg418.markerlib.marker.common.Marker
import net.minecraft.world.phys.Vec3
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent
import net.minecraftforge.client.event.RenderLevelStageEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod

@Mod.EventBusSubscriber(modid = MarkerLib.MODID, value = [Dist.CLIENT], bus = Mod.EventBusSubscriber.Bus.MOD)
object ClientModEvents {
    @SubscribeEvent
    fun onRegisterOverlays(e: RegisterGuiOverlaysEvent) {
        e.registerBelowAll("markers") { _, g, partialTick, _, _ ->
            val testMarker = Marker("test", Marker.Target.Fixed(Vec3(0.5, 80.0, 0.5)))
            MarkerRenderer.render(g, listOf(testMarker), partialTick)
        }
    }
}

@Mod.EventBusSubscriber(modid = MarkerLib.MODID, value = [Dist.CLIENT], bus = Mod.EventBusSubscriber.Bus.FORGE)
object ClientForgeEvents {
    @SubscribeEvent
    fun onRenderLevel(e: RenderLevelStageEvent) {
        if (e.stage == RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            MarkerRenderer.captureMatrices(e.projectionMatrix, e.poseStack.last().pose())
        }
    }
}