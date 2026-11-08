package com.npg418.markerlib.marker.server

import com.npg418.markerlib.marker.common.Marker
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.Tag
import net.minecraft.server.MinecraftServer
import net.minecraft.world.level.saveddata.SavedData

class ServerMarkers : SavedData() {
    companion object {
        private const val NAME = "markerlib_markers"

        fun load(tag: CompoundTag) = ServerMarkers().also { d ->
            tag.getList("markers", Tag.TAG_COMPOUND.toInt()).forEach {
                val m = Marker.fromTag(it as CompoundTag)
                d.markers[m.id] = m
            }
        }

        fun get(server: MinecraftServer): ServerMarkers =
            server.overworld().dataStorage.computeIfAbsent(::load, ::ServerMarkers, NAME)
    }

    val markers = LinkedHashMap<String, Marker>()

    val all: Collection<Marker> get() = markers.values
    operator fun get(id: String) = markers[id]

    fun put(m: Marker) {
        markers[m.id] = m
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