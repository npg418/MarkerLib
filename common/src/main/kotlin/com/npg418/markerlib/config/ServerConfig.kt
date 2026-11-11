package com.npg418.markerlib.config

import com.npg418.markerlib.config.api.*

object ServerConfig : ConfigSpec(ConfigType.SERVER) {
    @Name("entitySearch")
    object EntitySearch {
        @Comment(
            "How often (in ticks) entity selectors of markers are re-evaluated.",
            "Lower values reflect changes such as tags, teams and scores faster, at a higher server cost.",
        )
        var interval by entry(20) ranged 1..Int.MAX_VALUE

        @Comment(
            "Minimum ticks between re-evaluations triggered by entities spawning, despawning or dying.",
            "These events trigger an extra evaluation sooner than 'interval'. Keep this at or below 'interval'.",
        )
        var minGap by entry(5) ranged 1..Int.MAX_VALUE
    }

    @Name("markerCommand")
    object MarkerCommand {
        var requiredPermission by entry(2) ranged 2..4

        var maxCreatePoints by entry(16) ranged 1..64
    }
}