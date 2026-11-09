package com.npg418.markerlib.config

import com.npg418.markerlib.config.api.Comment
import com.npg418.markerlib.config.api.ConfigSpec
import com.npg418.markerlib.config.api.ConfigType
import com.npg418.markerlib.config.api.Name
import com.npg418.markerlib.config.api.ranged

object CommonConfig : ConfigSpec(ConfigType.COMMON) {
    @Name("rendering")
    object Rendering {
        @Comment("Base marker icon size. Actual rendered size is baseIconSize * size(per marker) px.")
        val baseIconSize by entry(8) ranged 0..Int.MAX_VALUE
    }
}