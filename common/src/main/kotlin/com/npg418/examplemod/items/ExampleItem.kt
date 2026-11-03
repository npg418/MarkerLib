package com.npg418.examplemod.items

import com.npg418.examplemod.config.CommonConfig
import com.npg418.examplemod.config.StartupConfig
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level

@Suppress("unused")
enum class GreetingLang(val greet: String) {
    EN("Hello, "),
    JA("こんにちは、"),
    CN("你好，"),
    KR("안녕하세요, ")
}

class ExampleItem : Item(Properties().stacksTo(1).durability(StartupConfig.Item.exampleItemDurability)) {
    override fun use(level: Level, player: Player, usedHand: InteractionHand): InteractionResultHolder<ItemStack?> {
        val stack = player.getItemInHand(usedHand)
        if (level.isClientSide) {
            player.displayClientMessage(
                Component.literal(CommonConfig.Item.greetingLang.greet)
                    .append(player.name)
                    .append(Component.literal("!")),
                false
            )
        } else {
            val slot = LivingEntity.getSlotForHand(usedHand)
            stack.hurtAndBreak(1, player, slot)
        }
        return InteractionResultHolder.success(stack)
    }
}