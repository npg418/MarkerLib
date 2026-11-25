package com.npg418.markerlib.client

import com.npg418.markerlib.common.Marker
import java.util.*

object ClientMarkers {
    private val markers = mutableMapOf<String, Marker>()

    internal var trackedEntities = emptySet<UUID>()
        private set

    val all get() = markers.values

    operator fun get(id: String) = markers[id]

    fun put(marker: Marker) {
        markers[marker.id] = marker
        update()
    }

    fun remove(id: String) = markers.remove(id)?.also { update() }

    fun clear() {
        markers.clear()
        update()
    }

    fun replaceAll(collection: Collection<Marker>) {
        markers.clear()
        collection.forEach { markers[it.id] = it }
        update()
    }

    private fun update() {
        trackedEntities = markers.values.flatMapTo(HashSet()) { it.entities.keys }
    }
}