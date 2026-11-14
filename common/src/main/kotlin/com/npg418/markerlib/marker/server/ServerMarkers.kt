package com.npg418.markerlib.marker.server

import com.npg418.markerlib.MarkerLib
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.NbtOps
import net.minecraft.server.MinecraftServer
import net.minecraft.world.level.saveddata.SavedData

internal class ServerMarkers private constructor() : SavedData() {
    private val entries = LinkedHashMap<String, ServerEntry>()

    val all: Collection<ServerEntry> get() = entries.values

    operator fun get(id: String) = entries[id]
    fun put(entry: ServerEntry) {
        entries[entry.id] = entry
        setDirty()
    }

    fun remove(id: String) = entries.remove(id)?.also { setDirty() }

    fun clear() {
        entries.clear()
        setDirty()
    }

    override fun save(tag: CompoundTag): CompoundTag = tag.apply {
        put(
            "markers",
            ServerEntry.CODEC.listOf().encodeStart(NbtOps.INSTANCE, entries.values.toList()).getOrThrow(false, ::error),
        )
    }

    companion object {
        private const val NAME = "markerlib_markers"

        private fun load(tag: CompoundTag) = ServerMarkers().also { d ->
            val rawMarkers = tag.get("markers") as? ListTag ?: return@also
            rawMarkers.forEach { raw ->
                ServerEntry.CODEC.parse(NbtOps.INSTANCE, raw)
                    .resultOrPartial { MarkerLib.LOGGER.error("Failed to load marker: $it") }
                    .ifPresent { d.entries[it.id] = it }
            }
        }

        internal fun of(server: MinecraftServer): ServerMarkers =
            server.overworld().dataStorage.computeIfAbsent(::load, ::ServerMarkers, NAME)
    }
}