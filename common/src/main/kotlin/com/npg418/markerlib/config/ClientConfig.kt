package com.npg418.markerlib.config

import com.npg418.markerlib.config.api.*

object ClientConfig : ConfigSpec(ConfigType.CLIENT) {
    @Name("rendering")
    object Rendering {
        @Comment("Base marker icon size. Actual rendered size is baseIconSize * size(per marker) px.")
        val baseIconSize by entry(8) ranged 0..Int.MAX_VALUE

        val edgeMargin by entry(20f) ranged 0f..100f

        val scaleRefDistance by entry(16f) ranged 1f..1024f
        val minScale by entry(0.4f) ranged 0.1f..1f
        val maxScale by entry(2f) ranged 1f..8f
        val textScale by entry(0.6f) ranged 0.25f..2f
    }
}