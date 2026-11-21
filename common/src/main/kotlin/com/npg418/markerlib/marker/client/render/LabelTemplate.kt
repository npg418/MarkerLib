package com.npg418.markerlib.marker.client.render

import com.npg418.markerlib.marker.format
import net.minecraft.client.Minecraft
import java.util.*
import kotlin.math.roundToInt

internal object LabelTemplate {
    private val TOKEN = Regex("""\{(\w+)}""")

    private fun playerName(uuid: UUID) = Minecraft.getInstance().connection?.getPlayerInfo(uuid)?.profile?.name

    fun expand(template: String, item: RenderItem): String = TOKEN.replace(template) {
        when (it.groupValues[1]) {
            "name" -> item.anchor.uuid?.let { uuid -> playerName(uuid) } ?: it.value
            "dist" -> item.dist.roundToInt().toString()
            "pos" -> item.anchor.position.format(0)
            "position" -> item.anchor.position.format(stripZeros = false)
            else -> it.value
        }
    }
}