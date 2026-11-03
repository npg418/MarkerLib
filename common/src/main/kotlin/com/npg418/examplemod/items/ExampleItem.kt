package com.npg418.examplemod.items

import com.npg418.examplemod.config.CommonConfig
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResultHolder
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

class ExampleItem : Item(Properties().stacksTo(1).durability(64)) {
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
            stack.hurtAndBreak(1, player) { it.broadcastBreakEvent(usedHand) }
        }
        return InteractionResultHolder.success(stack)
    }
}