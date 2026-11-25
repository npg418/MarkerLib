package com.npg418.markerlib.marker.common

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.network.FriendlyByteBuf
import java.util.*
import kotlin.jvm.optionals.getOrNull

data class Marker(
    val id: String,
    val positions: List<DimensionalPosition> = emptyList(),
    val entities: Map<UUID, DimensionalPosition> = emptyMap(),
    val icon: MarkerIcon = MarkerIcon(),
    val color: Int = 0xFF3030,
    val blend: Float = 1f,
    val topText: String? = null,
    val bottomText: String? = null,
    val hideByBlock: Boolean = false,
) {
    init {
        require(blend in 0f..1f) { "blend must be in 0..1 but was $blend" }
    }

    fun write(buf: FriendlyByteBuf) {
        buf.writeUtf(id)
        buf.writeCollection(positions) { b, p -> p.write(b) }
        buf.writeMap(entities, { b, k -> b.writeUUID(k) }, { b, v -> v.write(b) })
        icon.write(buf)
        buf.writeInt(color)
        buf.writeFloat(blend)
        buf.writeNullable(topText) { b, s -> b.writeUtf(s) }
        buf.writeNullable(bottomText) { b, s -> b.writeUtf(s) }
        buf.writeBoolean(hideByBlock)
    }

    companion object {
        val CODEC: Codec<Marker> = RecordCodecBuilder.create { i ->
            i.group(
                Codec.STRING.fieldOf("id").forGetter { it.id },
                DimensionalPosition.CODEC.listOf().fieldOf("positions").forGetter { it.positions },
                MarkerIcon.CODEC.fieldOf("icon").forGetter { it.icon },
                Codec.intRange(0x000000, 0xFFFFFF).fieldOf("color").forGetter { it.color },
                Codec.floatRange(0f, 1f).fieldOf("blend").forGetter { it.blend },
                Codec.STRING.optionalFieldOf("topText").forGetter { Optional.ofNullable(it.topText) },
                Codec.STRING.optionalFieldOf("bottomText").forGetter { Optional.ofNullable(it.bottomText) },
                Codec.BOOL.fieldOf("hideByBlock").forGetter { it.hideByBlock },
            ).apply(i) { id, positions, icon, color, blend, topText, bottomText, hideByBlock ->
                Marker(
                    id,
                    positions,
                    emptyMap(),
                    icon,
                    color,
                    blend,
                    topText.getOrNull(),
                    bottomText.getOrNull(),
                    hideByBlock,
                )
            }
        }

        fun read(buf: FriendlyByteBuf) = Marker(
            id = buf.readUtf(),
            positions = buf.readList { DimensionalPosition.read(it) },
            entities = buf.readMap({ it.readUUID() }, { DimensionalPosition.read(it) }),
            icon = MarkerIcon.read(buf),
            color = buf.readInt() and 0xFFFFFF,
            blend = buf.readFloat(),
            topText = buf.readNullable { it.readUtf() },
            bottomText = buf.readNullable { it.readUtf() },
            hideByBlock = buf.readBoolean(),
        )
    }
}