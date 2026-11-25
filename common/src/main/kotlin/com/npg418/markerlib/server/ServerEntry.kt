package com.npg418.markerlib.server

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import com.npg418.markerlib.common.Marker
import java.util.*
import kotlin.jvm.optionals.getOrNull

data class ServerEntry(val marker: Marker, val visibleTo: String? = null, val entitySelector: String? = null) {
    val id: String get() = marker.id

    companion object {
        val CODEC: Codec<ServerEntry> = RecordCodecBuilder.create { i ->
            i.group(
                Marker.CODEC.fieldOf("marker").forGetter { it.marker },
                Codec.STRING.optionalFieldOf("visibleTo").forGetter { Optional.ofNullable(it.visibleTo) },
                Codec.STRING.optionalFieldOf("entitySelector").forGetter { Optional.ofNullable(it.entitySelector) },
            ).apply(i) { marker, visibleTo, selector ->
                ServerEntry(marker, visibleTo.getOrNull(), selector.getOrNull())
            }
        }
    }
}