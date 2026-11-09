package com.npg418.markerlib.config

import com.npg418.markerlib.config.api.*

object ServerConfig : ConfigSpec(ConfigType.SERVER) {
    @Name("entitySelector")
    object Selector {
        @Comment(
            "How often (in ticks) entity selectors of markers are re-evaluated.\n" +
                "Lower values reflect changes such as tags, teams and scores faster, at a higher server cost.",
        )
        var interval by entry(20) ranged 1..Int.MAX_VALUE

        @Comment(
            "Minimum ticks between re-evaluations triggered by entities spawning, despawning or dying.\n" +
                "These events trigger an extra evaluation sooner than 'interval'. Keep this at or below 'interval'.",
        )
        var minGap by entry(5) ranged 1..Int.MAX_VALUE
    }
}