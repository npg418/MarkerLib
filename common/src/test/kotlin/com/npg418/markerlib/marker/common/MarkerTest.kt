package com.npg418.markerlib.marker.common

import io.netty.buffer.Unpooled
import net.minecraft.core.BlockPos
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.world.phys.Vec3
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.*

class MarkerTest {
    companion object {
        private val SAMPLE = Marker(
            id = "test",
            target = Marker.Target.Fixed(Vec3(1.5, 64.0, -3.25)),
            color = 0xFF00FF00.toInt(),
            label = "base",
            size = 1.5f,
            visibleTo = setOf(UUID.randomUUID()),
        )
    }

    @Test
    fun nbtRoundTrip() {
        assertEquals(SAMPLE, Marker.fromTag(SAMPLE.toTag()))
    }

    @Test
    fun nbtRoundTripAllTargets() {
        val targets = listOf(
            Marker.Target.Fixed(Vec3(0.0, 1.0, 2.0)),
            Marker.Target.Entity(UUID.randomUUID(), 0.5),
            Marker.Target.Block(BlockPos(1, 2, 3)),
        )
        targets.forEach { assertEquals(it, Marker.Target.fromTag(it.toTag())) }
    }

    @Test
    fun packetRoundTrip() {
        val buf = FriendlyByteBuf(Unpooled.buffer())
        SAMPLE.write(buf)
        assertEquals(SAMPLE.copy(visibleTo = emptySet()), Marker.read(buf))
    }

    @Test
    fun storeReplacesById() {
        val store = MarkerStore()
        store.put(SAMPLE)
        store.put(SAMPLE.copy(color = 0xFFFF0000.toInt()))
        assertEquals(1, store.all.size)
        assertEquals(0xFFFF0000.toInt(), store["test"]!!.color)
    }

    @Test
    fun storeRemove() {
        val store = MarkerStore().apply { put(SAMPLE) }
        assertNotNull(store.remove("test"))
        assertNull(store.remove("test"))
    }
}