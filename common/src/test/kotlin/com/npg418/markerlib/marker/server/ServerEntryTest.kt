package com.npg418.markerlib.marker.server

import com.npg418.markerlib.common.Marker
import com.npg418.markerlib.server.ServerEntry
import net.minecraft.nbt.NbtOps
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ServerEntryTest {
    @Test
    fun nbtRoundTrip() {
        val nbt = ServerEntry.CODEC.encodeStart(NbtOps.INSTANCE, SAMPLE).getOrThrow(false, ::error)
        val parsed = ServerEntry.CODEC.parse(NbtOps.INSTANCE, nbt).getOrThrow(false, ::error)
        assertEquals(SAMPLE, parsed)
    }

    companion object {
        private val SAMPLE =
            ServerEntry(marker = Marker(id = "test"), visibleTo = "@a[team=red]", entitySelector = "@e[team=red]")
    }
}