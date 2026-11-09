package com.npg418.markerlib.marker.common.network

import com.npg418.markerlib.marker.common.Marker
import net.minecraft.network.FriendlyByteBuf

sealed interface MarkerPacket

data class SyncPacket(val markers: List<Marker>) : MarkerPacket {
    companion object {
        fun decode(buf: FriendlyByteBuf) = SyncPacket(buf.readList { Marker.read(it) })
    }

    fun encode(buf: FriendlyByteBuf) = buf.writeCollection(markers) { b, m -> m.write(b) }
}

data class AddPacket(val marker: Marker) : MarkerPacket {
    companion object {
        fun decode(buf: FriendlyByteBuf) = AddPacket(Marker.read(buf))
    }

    fun encode(buf: FriendlyByteBuf) = marker.write(buf)
}

data class RemovePacket(val id: String) : MarkerPacket {
    companion object {
        fun decode(buf: FriendlyByteBuf) = RemovePacket(buf.readUtf())
    }

    fun encode(buf: FriendlyByteBuf) {
        buf.writeUtf(id)
    }
}