package com.npg418.examplemod.init

import com.google.common.base.Suppliers
import com.npg418.examplemod.items.ExampleItem
import net.minecraft.world.item.Item
import java.util.function.Supplier

val EXAMPLE_ITEM = "example_item" to Suppliers.memoize { ExampleItem() }

val ITEM_MAP: Map<String, Supplier<out Item>> = mapOf(EXAMPLE_ITEM)
