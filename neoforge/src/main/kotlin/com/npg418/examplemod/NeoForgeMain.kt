package com.npg418.examplemod

import com.npg418.examplemod.config.NeoForgeConfigBuilder
import com.npg418.examplemod.datagen.ModItemModelProvider
import com.npg418.examplemod.datagen.ModLanguageProvider
import com.npg418.examplemod.init.CONFIGS
import com.npg418.examplemod.init.ITEM_MAP
import net.neoforged.bus.api.IEventBus
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.ModContainer
import net.neoforged.fml.common.Mod
import net.neoforged.neoforge.data.event.GatherDataEvent
import net.neoforged.neoforge.registries.DeferredRegister


@Mod(ExampleMod.MODID)
class NeoForgeMain(eventBus: IEventBus, container: ModContainer) {
    companion object {
        val ITEMS: DeferredRegister.Items = DeferredRegister.createItems(ExampleMod.MODID)
    }

    init {
        ExampleMod.LOGGER.debug("Hello from Neoforge Mod!")

        eventBus.register(this)

        CONFIGS.forEach { NeoForgeConfigBuilder(it).register(container) }

        ITEM_MAP.forEach(ITEMS::register)
        ITEMS.register(eventBus)
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