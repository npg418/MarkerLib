package com.npg418.markerlib

import com.npg418.markerlib.datagen.ModItemModelProvider
import com.npg418.markerlib.datagen.ModLanguageProvider
import com.npg418.markerlib.marker.client.ClientMarkers
import com.npg418.markerlib.marker.client.MarkerRenderer
import com.npg418.markerlib.marker.server.MarkerService
import com.npg418.markerlib.marker.server.command.MarkerCommand
import net.minecraft.server.level.ServerPlayer
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.client.event.ClientPlayerNetworkEvent
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent
import net.minecraftforge.client.event.RenderLevelStageEvent
import net.minecraftforge.data.event.GatherDataEvent
import net.minecraftforge.event.RegisterCommandsEvent
import net.minecraftforge.event.TickEvent
import net.minecraftforge.event.entity.EntityJoinLevelEvent
import net.minecraftforge.event.entity.EntityLeaveLevelEvent
import net.minecraftforge.event.entity.living.LivingDeathEvent
import net.minecraftforge.event.entity.player.PlayerEvent
import net.minecraftforge.event.server.ServerStartedEvent
import net.minecraftforge.event.server.ServerStoppedEvent
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
    fun onServerTick(e: TickEvent.ServerTickEvent) {
        if (e.phase == TickEvent.Phase.END) MarkerService.tick(e.server)
    }

    @SubscribeEvent
    fun onServerStarted(@Suppress("UNUSED_PARAMETER") e: ServerStartedEvent) {
        MarkerService.markDirty()
    }

    @SubscribeEvent
    fun onServerStopped(@Suppress("UNUSED_PARAMETER") e: ServerStoppedEvent) {
        MarkerService.onServerStopped()
    }

    @SubscribeEvent
    fun onEntityJoin(e: EntityJoinLevelEvent) {
        if (!e.level.isClientSide) MarkerService.markDirty()
    }

    @SubscribeEvent
    fun onEntityLeave(e: EntityLeaveLevelEvent) {
        if (!e.level.isClientSide) MarkerService.markDirty()
    }

    @SubscribeEvent
    fun onLivingDeath(e: LivingDeathEvent) {
        if (!e.entity.level().isClientSide) MarkerService.markDirty()
    }

    @SubscribeEvent
    fun onLogin(e: PlayerEvent.PlayerLoggedInEvent) {
        (e.entity as? ServerPlayer)?.let {
            MarkerService.markDirty()
            MarkerService.syncTo(it)
        }
    }

    @SubscribeEvent
    fun onLogout(e: PlayerEvent.PlayerLoggedOutEvent) {
        if (!e.entity.level().isClientSide) MarkerService.markDirty()
    }

    @SubscribeEvent
    fun onRegisterCommands(e: RegisterCommandsEvent) {
        MarkerCommand.register(e.dispatcher)
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
    fun onLoggingOut(@Suppress("UNUSED_PARAMETER") e: ClientPlayerNetworkEvent.LoggingOut) {
        ClientMarkers.clear()
    }
}