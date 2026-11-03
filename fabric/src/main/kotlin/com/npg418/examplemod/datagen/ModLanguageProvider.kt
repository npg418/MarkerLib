package com.npg418.examplemod.datagen

import com.npg418.examplemod.init.EXAMPLE_ITEM
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput
import net.fabricmc.fabric.api.datagen.v1.provider.FabricLanguageProvider
import net.minecraft.core.HolderLookup
import java.util.concurrent.CompletableFuture

class ModLanguageProvider(dataOutput: FabricDataOutput, registryLookup: CompletableFuture<HolderLookup.Provider>) : FabricLanguageProvider(dataOutput, "en_us", registryLookup) {
    override fun generateTranslations(holderLookup: HolderLookup.Provider, translationBuilder: TranslationBuilder) {
        translationBuilder.add(EXAMPLE_ITEM.second.get(), "Example Item")
    }
}