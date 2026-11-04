package com.npg418.examplemod

import net.fabricmc.loader.api.FabricLoader

class FabricPlatformHelper : PlatformHelper {
    override val isDevEnvironment: Boolean get() = FabricLoader.getInstance().isDevelopmentEnvironment
    override fun isModLoaded(id: String): Boolean = FabricLoader.getInstance().isModLoaded(id)
}