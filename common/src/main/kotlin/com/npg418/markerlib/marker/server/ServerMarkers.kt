package com.npg418.markerlib.marker.server

import com.npg418.markerlib.marker.common.Marker
import com.npg418.markerlib.marker.common.MarkerStore
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.Tag
import net.minecraft.server.MinecraftServer
import net.minecraft.world.level.saveddata.SavedData

class ServerMarkers : SavedData() {
    companion object {
        private const val NAME = "markerlib_markers"

        fun load(tag: CompoundTag) = ServerMarkers().also { d ->
            tag.getList("markers", Tag.TAG_COMPOUND.toInt()).forEach { d.store.put(Marker.fromTag(it as CompoundTag)) }
        }

        fun get(server: MinecraftServer): ServerMarkers =
            server.overworld().dataStorage.computeIfAbsent(::load, ::ServerMarkers, NAME)
    }

    val store = MarkerStore()

    val all: Collection<Marker> get() = store.all
    operator fun get(id: String) = store[id]

    fun put(m: Marker) {
        store.put(m)
        setDirty()
    }
    fun remove(id: String) = store.remove(id).also { if (it != null) setDirty() }
    fun clear() {
        store.clear()
        setDirty()
    }

    override fun save(tag: CompoundTag): CompoundTag {
        tag.put("markers", ListTag().apply { store.all.forEach { add(it.toTag()) } })
        return tag
    }
}