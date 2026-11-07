package com.npg418.markerlib.marker.common

open class MarkerStore {
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