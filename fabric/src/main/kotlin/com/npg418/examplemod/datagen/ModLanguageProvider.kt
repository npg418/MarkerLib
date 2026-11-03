package com.npg418.examplemod.datagen

import com.npg418.examplemod.init.EXAMPLE_ITEM
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput
import net.fabricmc.fabric.api.datagen.v1.provider.FabricLanguageProvider

class ModLanguageProvider(dataOutput: FabricDataOutput) : FabricLanguageProvider(dataOutput, "en_us") {
    override fun generateTranslations(translationBuilder: TranslationBuilder) {
        translationBuilder.add(EXAMPLE_ITEM.second.get(), "Example Item")
    }
}