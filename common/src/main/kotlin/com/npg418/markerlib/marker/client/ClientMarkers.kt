package com.npg418.markerlib.marker.client

import com.npg418.markerlib.marker.shared.Marker

object ClientMarkers {
    private val markers = LinkedHashMap<String, Marker>()

    val all get() = markers.values

    operator fun get(id: String) = markers[id]

    fun put(marker: Marker) {
        markers[marker.id] = marker
    }

    fun remove(id: String) = markers.remove(id)

    fun clear() = markers.clear()

    fun replaceAll(collection: Collection<Marker>) {
        markers.clear()
        collection.forEach(::put)
    }
}