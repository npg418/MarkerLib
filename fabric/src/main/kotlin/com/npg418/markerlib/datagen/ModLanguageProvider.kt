package com.npg418.markerlib.datagen

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput
import net.fabricmc.fabric.api.datagen.v1.provider.FabricLanguageProvider

class ModLanguageProvider(
    dataOutput: FabricDataOutput,
) : FabricLanguageProvider(dataOutput, "en_us") {
    override fun generateTranslations(translationBuilder: TranslationBuilder) {
    }
}