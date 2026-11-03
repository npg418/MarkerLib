package com.npg418.examplemod.datagen

import com.npg418.examplemod.init.EXAMPLE_ITEM
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput
import net.fabricmc.fabric.api.datagen.v1.provider.FabricModelProvider
import net.minecraft.data.models.BlockModelGenerators
import net.minecraft.data.models.ItemModelGenerators
import net.minecraft.data.models.model.ModelTemplates
import net.minecraft.world.item.Items

class ModModelProvider(output: FabricDataOutput) : FabricModelProvider(output) {
    override fun generateBlockStateModels(blockStateModelGenerator: BlockModelGenerators) {
    }

    override fun generateItemModels(itemModelGenerator: ItemModelGenerators) {
        itemModelGenerator.generateFlatItem(EXAMPLE_ITEM.second.get(), Items.STICK, ModelTemplates.FLAT_ITEM)
    }

    override fun getName(): String {
        return "ModModelProvider"
    }
}