package com.npg418.markerlib

import com.npg418.markerlib.config.ForgeConfigBuilder
import com.npg418.markerlib.init.CONFIGS
import com.npg418.markerlib.network.MarkerNetwork
import net.minecraftforge.fml.common.Mod
import thedarkcolour.kotlinforforge.forge.LOADING_CONTEXT

@Mod(MarkerLib.MODID)
object MarkerLibForge {
    init {
        MarkerNetwork.register()
        CONFIGS.forEach { ForgeConfigBuilder(it).register(LOADING_CONTEXT) }
    }
}