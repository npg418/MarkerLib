package com.npg418.markerlib.marker.common

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import com.npg418.markerlib.marker.isClose
import com.npg418.markerlib.marker.readVec3
import com.npg418.markerlib.marker.writeVec3
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.Level
import net.minecraft.world.phys.Vec3

data class DimensionPos(val pos: Vec3, val dimension: ResourceLocation = OVERWORLD) {
    fun write(buf: FriendlyByteBuf) {
        buf.writeVec3(pos)
        buf.writeNullable(dimension.takeIf { it != OVERWORLD }) { b, d -> b.writeResourceLocation(d) }
    }

    fun isClose(other: DimensionPos) = dimension == other.dimension && pos.isClose(other.pos)

    companion object {
        private val OVERWORLD = Level.OVERWORLD.location()

        val CODEC: Codec<DimensionPos> = RecordCodecBuilder.create { i ->
            i.group(
                Vec3.CODEC.fieldOf("pos").forGetter { it.pos },
                ResourceLocation.CODEC.optionalFieldOf("dimension", OVERWORLD).forGetter { it.dimension },
            ).apply(i) { pos, dimension ->
                DimensionPos(pos, dimension)
            }
        }

        fun read(buf: FriendlyByteBuf) = DimensionPos(
            pos = buf.readVec3(),
            dimension = buf.readNullable { it.readResourceLocation() } ?: OVERWORLD,
        )
    }
}