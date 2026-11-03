package com.npg418.examplemod.config

import com.npg418.examplemod.config.api.Comment
import com.npg418.examplemod.config.api.ConfigSpec
import com.npg418.examplemod.config.api.ConfigType
import com.npg418.examplemod.config.api.Name
import com.npg418.examplemod.items.GreetingLang

object CommonConfig : ConfigSpec(ConfigType.COMMON) {
    @Comment("Greeting log settings")
    @Name("greeting")
    object Greeting {
        @Comment("Whether log when title screen")
        var greetOnTitleScreen by defining(true)
    }

    @Name("item")
    object Item {
        @Comment("Greeting language when right-clicking Example Item")
        @Name("exampleItemGreetingLang")
        var greetingLang by definingEnum(GreetingLang.EN)
    }
}