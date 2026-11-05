package com.npg418.markerlib

import com.npg418.markerlib.config.ForgeConfigBuilder
import com.npg418.markerlib.datagen.ModItemModelProvider
import com.npg418.markerlib.datagen.ModLanguageProvider
import com.npg418.markerlib.init.CONFIGS
import net.minecraftforge.data.event.GatherDataEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod
import thedarkcolour.kotlinforforge.forge.LOADING_CONTEXT
import thedarkcolour.kotlinforforge.forge.MOD_BUS


@Mod(MarkerLib.MODID)
class ForgeMain {
    init {
        MarkerLib.LOGGER.debug("Hello from Forge Mod!")

        MOD_BUS.register(this)

        CONFIGS.forEach { ForgeConfigBuilder(it).register(LOADING_CONTEXT) }
    }

    @SubscribeEvent
    fun gatherData(event: GatherDataEvent) {
        val generator = event.generator
        val output = generator.packOutput
        val existingFileHelper = event.existingFileHelper

        generator.addProvider(
            event.includeClient(),
            ModItemModelProvider(output, existingFileHelper)
        )
        generator.addProvider(
            event.includeClient(),
            ModLanguageProvider(output)
        )
    }
}