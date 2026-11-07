package com.npg418.markerlib

import com.npg418.markerlib.config.ForgeConfigBuilder
import com.npg418.markerlib.datagen.ModItemModelProvider
import com.npg418.markerlib.datagen.ModLanguageProvider
import com.npg418.markerlib.init.CONFIGS
import com.npg418.markerlib.marker.server.MarkerService
import com.npg418.markerlib.network.MarkerNetwork
import net.minecraft.server.level.ServerPlayer
import net.minecraftforge.data.event.GatherDataEvent
import net.minecraftforge.event.entity.player.PlayerEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod
import thedarkcolour.kotlinforforge.forge.LOADING_CONTEXT
import thedarkcolour.kotlinforforge.forge.MOD_BUS

@Mod(MarkerLib.MODID)
object ForgeMain {
    val service = MarkerService(MarkerNetwork::send)

    init {
        MOD_BUS.register(this)

        CONFIGS.forEach { ForgeConfigBuilder(it).register(LOADING_CONTEXT) }
    }

    @SubscribeEvent
    fun onLogin(event: PlayerEvent.PlayerLoggedInEvent) {
        (event.entity as? ServerPlayer)?.let(service::syncTo)
    }

    @SubscribeEvent
    fun gatherData(event: GatherDataEvent) {
        val generator = event.generator
        val output = generator.packOutput
        val existingFileHelper = event.existingFileHelper

        generator.addProvider(
            event.includeClient(),
            ModItemModelProvider(output, existingFileHelper),
        )
        generator.addProvider(
            event.includeClient(),
            ModLanguageProvider(output),
        )
    }
}