package com.npg418.markerlib.marker.common

import io.netty.buffer.Unpooled
import net.minecraft.nbt.NbtOps
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.world.phys.Vec3
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.*

class MarkerTest {
    @Test
    fun nbtRoundTrip() {
        val nbt = Marker.CODEC.encodeStart(NbtOps.INSTANCE, SAMPLE).getOrThrow(false, ::error)
        val parsed = Marker.CODEC.parse(NbtOps.INSTANCE, nbt).getOrThrow(false, ::error)
        assertEquals(SAMPLE.copy(entities = emptySet()), parsed)
    }

    @Test
    fun packetRoundTrip() {
        val buf = FriendlyByteBuf(Unpooled.buffer())
        SAMPLE.write(buf)
        assertEquals(SAMPLE, Marker.read(buf))
        assertEquals(0, buf.readableBytes())
    }

    companion object {
        private val SAMPLE = Marker(
            id = "test",
            positions = listOf(Vec3(1.5, 64.0, -3.25)),
            entities = setOf(UUID.randomUUID()),
            icon = MarkerIcon(size = 1.5f, entityIconOffset = 1.0),
            color = 0x00FF00,
            topText = "{name}",
            bottomText = "{dist}m",
        )
    }
}