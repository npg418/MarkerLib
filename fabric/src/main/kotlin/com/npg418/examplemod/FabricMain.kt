package com.npg418.examplemod

import com.npg418.examplemod.config.FabricConfigBuilder
import com.npg418.examplemod.init.CONFIGS
import com.npg418.examplemod.init.ITEM_MAP
import net.fabricmc.api.ModInitializer
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation

class FabricMain : ModInitializer {
    override fun onInitialize() {
        ExampleMod.LOGGER.debug("Hello from Fabric Mod!")

        CONFIGS.forEach { FabricConfigBuilder(it).register(ExampleMod.MODID) }

        ITEM_MAP.forEach { (name, item) ->
            Registry.register(
                BuiltInRegistries.ITEM,
                ResourceLocation.fromNamespaceAndPath(ExampleMod.MODID, name),
                item.get()
            )
        }
    }
}