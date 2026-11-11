package com.npg418.markerlib.marker.server

import com.npg418.markerlib.marker.shared.Marker
import com.npg418.markerlib.marker.shared.MarkerTarget
import com.npg418.markerlib.marker.shared.getNullable
import com.npg418.markerlib.marker.shared.putNullable
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.Tag
import net.minecraft.server.MinecraftServer
import net.minecraft.world.level.saveddata.SavedData

data class ServerMarker(val marker: Marker, val visibleTo: String? = null, val entitySelector: String? = null) {
    companion object {
        fun fromTag(tag: CompoundTag) = ServerMarker(
            marker = Marker.fromTag(tag.getCompound("marker")),
            visibleTo = tag.getNullable("visibleTo", CompoundTag::getString),
            entitySelector = tag.getNullable("entitySelector", CompoundTag::getString),
        )
    }

    init {
        require(entitySelector == null || marker.target is MarkerTarget.Entities) {
            "entitySelector requires an Entities target"
        }
    }

    val id: String get() = marker.id

    fun toTag(): CompoundTag = CompoundTag().apply {
        put("marker", marker.toTag())
        putNullable("visibleTo", visibleTo, CompoundTag::putString)
        putNullable("entitySelector", entitySelector, CompoundTag::putString)
    }
}

class ServerMarkers : SavedData() {
    companion object {
        private const val NAME = "markerlib_markers"

        fun load(tag: CompoundTag) = ServerMarkers().also { d ->
            tag.getList("markers", Tag.TAG_COMPOUND.toInt()).forEach {
                val marker = ServerMarker.fromTag(it as CompoundTag)
                d.markers[marker.id] = marker
            }
        }

        fun of(server: MinecraftServer): ServerMarkers =
            server.overworld().dataStorage.computeIfAbsent(::load, ::ServerMarkers, NAME)
    }

    private val markers = LinkedHashMap<String, ServerMarker>()

    val all: Collection<ServerMarker> get() = markers.values
    operator fun get(id: String) = markers[id]

    fun put(marker: ServerMarker) {
        markers[marker.id] = marker
        setDirty()
    }

    fun remove(id: String) = markers.remove(id).also { if (it != null) setDirty() }

    fun clear() {
        markers.clear()
        setDirty()
    }

    override fun save(tag: CompoundTag): CompoundTag {
        tag.put("markers", ListTag().apply { all.forEach { add(it.toTag()) } })
        return tag
    }
}