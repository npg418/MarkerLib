package com.npg418.markerlib.datagen

import com.npg418.markerlib.MarkerLib
import net.minecraft.data.PackOutput
import net.minecraftforge.common.data.LanguageProvider

class ModLanguageProvider(output: PackOutput) : LanguageProvider(output, MarkerLib.MODID, "en_us") {
    override fun addTranslations() {}
}