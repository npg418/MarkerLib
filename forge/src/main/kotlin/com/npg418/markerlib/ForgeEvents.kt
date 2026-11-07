package com.npg418.markerlib

import com.npg418.markerlib.datagen.ModItemModelProvider
import com.npg418.markerlib.datagen.ModLanguageProvider
import com.npg418.markerlib.marker.client.ClientMarkers
import com.npg418.markerlib.marker.client.MarkerRenderer
import net.minecraft.server.level.ServerPlayer
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.client.event.ClientPlayerNetworkEvent
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent
import net.minecraftforge.client.event.RenderLevelStageEvent
import net.minecraftforge.data.event.GatherDataEvent
import net.minecraftforge.event.entity.player.PlayerEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod

@Mod.EventBusSubscriber(modid = MarkerLib.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
object CommonModEvents {
    @SubscribeEvent
    fun gatherData(e: GatherDataEvent) {
        val generator = e.generator
        val output = generator.packOutput
        val existingFileHelper = e.existingFileHelper

        generator.addProvider(e.includeClient(), ModItemModelProvider(output, existingFileHelper))
        generator.addProvider(e.includeClient(), ModLanguageProvider(output))
    }
}

@Mod.EventBusSubscriber(modid = MarkerLib.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
object CommonForgeEvents {
    @SubscribeEvent
    fun onLogin(e: PlayerEvent.PlayerLoggedInEvent) {
        (e.entity as? ServerPlayer)?.let(MarkerLibForge.service::syncTo)
    }
}

@Mod.EventBusSubscriber(modid = MarkerLib.MODID, value = [Dist.CLIENT], bus = Mod.EventBusSubscriber.Bus.MOD)
object ClientModEvents {
    @SubscribeEvent
    fun onRegisterOverlays(e: RegisterGuiOverlaysEvent) {
        e.registerBelowAll("markers") { _, g, partialTick, _, _ ->
            MarkerRenderer.render(g, ClientMarkers.all, partialTick)
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

    @SubscribeEvent
    @Suppress("unused")
    fun onLoggingOut(e: ClientPlayerNetworkEvent.LoggingOut) {
        ClientMarkers.clear()
    }
}