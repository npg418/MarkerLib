package com.npg418.markerlib

import com.npg418.markerlib.config.FabricConfigBuilder
import com.npg418.markerlib.init.CONFIGS
import com.npg418.markerlib.init.ITEM_MAP
import net.fabricmc.api.ModInitializer
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation

class FabricMain : ModInitializer {
    override fun onInitialize() {
        MarkerLib.LOGGER.debug("Hello from Fabric Mod!")

        CONFIGS.forEach { FabricConfigBuilder(it).register(MarkerLib.MODID) }

        ITEM_MAP.forEach { (name, item) ->
            Registry.register(
                BuiltInRegistries.ITEM,
                ResourceLocation(MarkerLib.MODID, name),
                item.get(),
            )
        }
    }
}