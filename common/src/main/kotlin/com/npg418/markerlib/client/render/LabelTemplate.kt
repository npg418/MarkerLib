package com.npg418.markerlib.client.render

import com.npg418.markerlib.format
import net.minecraft.client.Minecraft
import java.util.*
import kotlin.math.roundToInt

internal object LabelTemplate {
    private val TOKEN = Regex("""\{(\w+)}""")

    private fun playerName(uuid: UUID) = Minecraft.getInstance().connection?.getPlayerInfo(uuid)?.profile?.name

    fun expand(template: String, mark: Mark): String {
        if ('{' !in template) return template
        return TOKEN.replace(template) {
            when (it.groupValues[1]) {
                "name" -> mark.target.uuid?.let(::playerName) ?: it.value
                "distance" -> mark.distance.roundToInt().toString()
                "position" -> mark.target.position.format(0)
                else -> it.value
            }
        }
    }
}