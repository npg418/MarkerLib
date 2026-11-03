package com.npg418.examplemod

import com.npg418.examplemod.config.ForgeConfigBuilder
import com.npg418.examplemod.datagen.ModItemModelProvider
import com.npg418.examplemod.datagen.ModLanguageProvider
import com.npg418.examplemod.init.CONFIGS
import com.npg418.examplemod.init.ITEM_MAP
import net.minecraft.world.item.Item
import net.minecraftforge.data.event.GatherDataEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.registries.DeferredRegister
import net.minecraftforge.registries.ForgeRegistries
import thedarkcolour.kotlinforforge.forge.LOADING_CONTEXT
import thedarkcolour.kotlinforforge.forge.MOD_BUS


@Mod(ExampleMod.MODID)
class ForgeMain {
    companion object {
        val ITEMS: DeferredRegister<Item> = DeferredRegister.create(ForgeRegistries.ITEMS, ExampleMod.MODID)
    }

    init {
        ExampleMod.LOGGER.debug("Hello from Forge Mod!")

        MOD_BUS.register(this)

        CONFIGS.forEach { ForgeConfigBuilder(it).register(LOADING_CONTEXT) }

        ITEM_MAP.forEach(ITEMS::register)
        ITEMS.register(MOD_BUS)
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