package com.npg418.markerlib.marker.common

import com.npg418.markerlib.common.DimensionalPosition
import com.npg418.markerlib.common.Marker
import com.npg418.markerlib.common.MarkerIcon
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
        assertEquals(SAMPLE.copy(entities = emptyMap()), parsed)
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
            positions = listOf(DimensionalPosition(Vec3(1.5, 64.0, -3.25))),
            entities = mapOf(UUID.randomUUID() to DimensionalPosition(Vec3(2.0, 63.0, -26.0))),
            icon = MarkerIcon(size = 1.5f, entityIconOffset = 1.0),
            color = 0x00FF00,
            topText = "{name}",
            bottomText = "{distance}m",
        )
    }
}