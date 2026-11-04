package com.npg418.markerlib.datagen

import com.npg418.markerlib.MarkerLib
import net.minecraft.data.PackOutput
import net.minecraftforge.client.model.generators.ItemModelProvider
import net.minecraftforge.common.data.ExistingFileHelper

class ModItemModelProvider(output: PackOutput, existingFileHelper: ExistingFileHelper) :
    ItemModelProvider(output, MarkerLib.MODID, existingFileHelper) {
    override fun registerModels() {}
}