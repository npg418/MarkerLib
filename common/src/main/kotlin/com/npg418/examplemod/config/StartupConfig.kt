package com.npg418.examplemod.config

import com.npg418.examplemod.config.api.*

object StartupConfig : ConfigSpec(ConfigType.STARTUP) {
    @Name("item")
    object Item {
        @Comment("Durability of Example Item")
        @GameRestart
        var exampleItemDurability by definingInRange(64, 1..Int.MAX_VALUE)
    }
}