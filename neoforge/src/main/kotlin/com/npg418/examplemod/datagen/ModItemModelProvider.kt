package com.npg418.examplemod.datagen

import com.npg418.examplemod.ExampleMod
import com.npg418.examplemod.init.EXAMPLE_ITEM
import net.minecraft.data.PackOutput
import net.minecraft.resources.ResourceLocation
import net.neoforged.neoforge.client.model.generators.ItemModelProvider
import net.neoforged.neoforge.common.data.ExistingFileHelper

class ModItemModelProvider(output: PackOutput, existingFileHelper: ExistingFileHelper) :
    ItemModelProvider(output, ExampleMod.MODID, existingFileHelper) {
    override fun registerModels() {
        withExistingParent(
            EXAMPLE_ITEM.first,
            ResourceLocation.fromNamespaceAndPath("minecraft", "item/generated")
        ).texture(
            "layer0",
            ResourceLocation.fromNamespaceAndPath("minecraft", "item/stick")
        )
    }
}