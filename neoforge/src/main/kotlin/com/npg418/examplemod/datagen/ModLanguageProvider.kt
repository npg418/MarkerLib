package com.npg418.examplemod.datagen

import com.npg418.examplemod.ExampleMod
import com.npg418.examplemod.init.EXAMPLE_ITEM
import net.minecraft.data.PackOutput
import net.neoforged.neoforge.common.data.LanguageProvider

class ModLanguageProvider(output: PackOutput) : LanguageProvider(output, ExampleMod.MODID, "en_us") {
    override fun addTranslations() {
        addItem(EXAMPLE_ITEM.second, "Example Item")
    }
}