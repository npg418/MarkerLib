package com.npg418.markerlib.marker.client.render

import com.npg418.markerlib.config.ClientConfig
import com.npg418.markerlib.marker.common.Marker
import net.minecraft.client.gui.Font

internal object MarkMetrics {
    const val LABEL_GAP = 2f

    fun iconSize(marker: Marker): Float = ClientConfig.baseIconSize * marker.icon.size

    fun labelHeight(font: Font): Float = font.lineHeight * ClientConfig.Scale.textScale

    fun labelReach(marker: Marker, font: Font): Float = iconSize(marker) / 2f + LABEL_GAP + labelHeight(font)
}