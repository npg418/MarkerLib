package com.npg418.markerlib.marker.common

import io.netty.buffer.Unpooled
import net.minecraft.network.FriendlyByteBuf
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class MarkerPacketTest {
    @Test
    fun syncPacketRoundTrip() {
        val syncPacket = SyncPacket(listOf(SAMPLE))
        assertPacketRoundTrip(syncPacket, SyncPacket::encode, SyncPacket::decode)
    }

    @Test
    fun addPacketRoundTrip() {
        val addPacket = AddPacket(SAMPLE)
        assertPacketRoundTrip(addPacket, AddPacket::encode, AddPacket::decode)
    }

    @Test
    fun removePacketRoundTrip() {
        val removePacket = RemovePacket(SAMPLE.id)
        assertPacketRoundTrip(removePacket, RemovePacket::encode, RemovePacket::decode)
    }

    private fun <T : MarkerPacket> assertPacketRoundTrip(
        packet: T,
        encode: T.(FriendlyByteBuf) -> Unit,
        decode: (FriendlyByteBuf) -> T,
    ) {
        val buf = FriendlyByteBuf(Unpooled.buffer())
        packet.encode(buf)
        assertEquals(packet, decode(buf))
        assertEquals(0, buf.readableBytes())
    }

    companion object {
        private val SAMPLE = Marker(id = "test")
    }
}