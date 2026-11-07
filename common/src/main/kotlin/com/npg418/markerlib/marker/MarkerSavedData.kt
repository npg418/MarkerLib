package com.npg418.markerlib.marker

import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.Tag
import net.minecraft.server.MinecraftServer
import net.minecraft.world.level.saveddata.SavedData

class MarkerSavedData : SavedData() {
    companion object {
        private const val NAME = "markerlib_markers"

        fun load(tag: CompoundTag) = MarkerSavedData().also { d ->
            tag.getList("markers", Tag.TAG_COMPOUND.toInt()).forEach { d.store.put(Marker.fromTag(it as CompoundTag)) }
        }

        fun get(server: MinecraftServer): MarkerSavedData =
            server.overworld().dataStorage.computeIfAbsent(::load, ::MarkerSavedData, NAME)
    }

    val store = MarkerStore()

    val all: Collection<Marker> get() = store.all
    operator fun get(id: String) = store[id]

    fun put(m: Marker) {
        store.put(m)
        setDirty()
    }
    fun remove(id: String): Boolean = store.remove(id).also { if (it) setDirty() }
    fun clear() {
        store.clear()
        setDirty()
    }

    override fun save(tag: CompoundTag): CompoundTag {
        tag.put("markers", ListTag().apply { store.all.forEach { add(it.toTag()) } })
        return tag
    }
}