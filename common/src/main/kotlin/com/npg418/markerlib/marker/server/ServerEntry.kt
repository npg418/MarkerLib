package com.npg418.markerlib.marker.server

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import com.npg418.markerlib.marker.shared.Marker
import com.npg418.markerlib.marker.shared.nullableFieldOf

data class ServerEntry(val marker: Marker, val visibleTo: String? = null, val entitySelector: String? = null) {
    val id: String get() = marker.id

    companion object {
        val CODEC: Codec<ServerEntry> = RecordCodecBuilder.create { i ->
            i.group(
                Marker.CODEC.fieldOf("marker").forGetter { it.marker },
                Codec.STRING.nullableFieldOf("visibleTo").forGetter { it.visibleTo },
                Codec.STRING.nullableFieldOf("entitySelector").forGetter { it.entitySelector },
            ).apply(i) { marker, visibleTo, selector -> ServerEntry(marker, visibleTo, selector) }
        }
    }
}