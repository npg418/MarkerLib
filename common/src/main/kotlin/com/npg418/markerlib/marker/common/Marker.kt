package com.npg418.markerlib.marker.common

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.phys.Vec3
import java.util.*
import kotlin.jvm.optionals.getOrNull

data class Marker(
    val id: String,
    val positions: List<Vec3> = emptyList(),
    val entities: Set<UUID> = emptySet(),
    val icon: MarkerIcon = MarkerIcon(),
    val color: Int = 0xFF3030,
    val blend: Float = 1f,
    val label: String? = null,
    val showDistance: Boolean = true,
) {
    init {
        require(blend in 0f..1f) { "blend must be in 0..1 but was $blend" }
    }

    fun write(buf: FriendlyByteBuf) {
        buf.writeUtf(id)
        buf.writeCollection(positions) { b, p -> b.writeVec3(p) }
        buf.writeCollection(entities) { b, e -> b.writeUUID(e) }
        icon.write(buf)
        buf.writeInt(color)
        buf.writeFloat(blend)
        buf.writeNullable(label) { b, s -> b.writeUtf(s) }
        buf.writeBoolean(showDistance)
    }

    companion object {
        const val RGB_MASK = 0xFFFFFF

        val CODEC: Codec<Marker> = RecordCodecBuilder.create { i ->
            i.group(
                Codec.STRING.fieldOf("id").forGetter { it.id },
                Vec3.CODEC.listOf().fieldOf("positions").forGetter { it.positions },
                MarkerIcon.CODEC.fieldOf("icon").forGetter { it.icon },
                Codec.intRange(0x000000, 0xFFFFFF).fieldOf("color").forGetter { it.color },
                Codec.floatRange(0f, 1f).fieldOf("blend").forGetter { it.blend },
                Codec.STRING.optionalFieldOf("label").forGetter { Optional.ofNullable(it.label) },
                Codec.BOOL.fieldOf("showDistance").forGetter { it.showDistance },
            ).apply(i) { id, positions, icon, color, blend, label, showDistance ->
                Marker(id, positions, emptySet(), icon, color, blend, label.getOrNull(), showDistance)
            }
        }

        fun read(buf: FriendlyByteBuf) = Marker(
            id = buf.readUtf(),
            positions = buf.readList { it.readVec3() },
            entities = buf.readCollection(::HashSet) { it.readUUID() },
            icon = MarkerIcon.read(buf),
            color = buf.readInt() and RGB_MASK,
            blend = buf.readFloat(),
            label = buf.readNullable { it.readUtf() },
            showDistance = buf.readBoolean(),
        )
    }
}

data class MarkerIcon(
    val iconTexture: ResourceLocation = DEFAULT_ICON,
    val arrowTexture: ResourceLocation? = DEFAULT_ARROW,
    val size: Float = 1f,
    val entityIconOffset: Double = 0.5,
) {
    init {
        require(size in MIN_SIZE..MAX_SIZE) { "size must be in $MIN_SIZE..$MAX_SIZE but was $size" }
    }

    fun write(buf: FriendlyByteBuf) {
        buf.writeResourceLocation(iconTexture)
        buf.writeNullable(arrowTexture) { b, a -> b.writeResourceLocation(a) }
        buf.writeFloat(size)
        buf.writeDouble(entityIconOffset)
    }

    companion object {
        val DEFAULT_ICON = ResourceLocation("markerlib", "textures/gui/marker.png")
        val DEFAULT_ARROW = ResourceLocation("markerlib", "textures/gui/arrow.png")

        const val MIN_SIZE = 0.25f
        const val MAX_SIZE = 4f

        val CODEC: Codec<MarkerIcon> = RecordCodecBuilder.create { i ->
            i.group(
                ResourceLocation.CODEC.fieldOf("icon").forGetter { it.iconTexture },
                ResourceLocation.CODEC.optionalFieldOf("arrowIcon").forGetter { Optional.ofNullable(it.arrowTexture) },
                Codec.floatRange(MIN_SIZE, MAX_SIZE).fieldOf("size").forGetter { it.size },
                Codec.DOUBLE.fieldOf("entityIconOffset").forGetter { it.entityIconOffset },
            ).apply(i) { icon, arrowIcon, size, entityIconOffset ->
                MarkerIcon(icon, arrowIcon.getOrNull(), size, entityIconOffset)
            }
        }

        fun read(buf: FriendlyByteBuf) = MarkerIcon(
            iconTexture = buf.readResourceLocation(),
            arrowTexture = buf.readNullable { it.readResourceLocation() },
            size = buf.readFloat(),
            entityIconOffset = buf.readDouble(),
        )
    }
}