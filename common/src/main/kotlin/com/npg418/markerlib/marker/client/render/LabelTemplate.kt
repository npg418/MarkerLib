package com.npg418.markerlib.marker.client.render

import com.npg418.markerlib.marker.format
import kotlin.math.roundToInt

internal object LabelTemplate {
    private val TOKEN = Regex("""\{(\w+)}""")

    fun expand(template: String, item: RenderItem): String = TOKEN.replace(template) {
        when (it.groupValues[1]) {
            "name" -> item.anchor.entity?.name?.string ?: it.value
            "dist" -> item.dist.roundToInt().toString()
            "pos" -> item.anchor.pos.format(0)
            "position" -> item.anchor.pos.format(stripZeros = false)
            else -> it.value
        }
    }
}