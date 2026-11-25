package com.npg418.markerlib.common

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.resources.ResourceLocation
import java.util.*
import kotlin.jvm.optionals.getOrNull

data class MarkerIcon(
    val iconTexture: ResourceLocation = DEFAULT_ICON,
    val arrowTexture: ResourceLocation? = null,
    val size: Float = 1f,
    val entityIconOffset: Double = 0.2,
    val playerFace: Boolean = false,
    val arrowFace: Boolean = false,
) {
    init {
        require(size in MIN_SIZE..MAX_SIZE) { "size must be in $MIN_SIZE..$MAX_SIZE but was $size" }
    }

    val hasArrow get() = arrowTexture != null || arrowFace

    fun write(buf: FriendlyByteBuf) {
        buf.writeResourceLocation(iconTexture)
        buf.writeNullable(arrowTexture) { b, a -> b.writeResourceLocation(a) }
        buf.writeFloat(size)
        buf.writeDouble(entityIconOffset)
        buf.writeBoolean(playerFace)
        buf.writeBoolean(arrowFace)
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
                Codec.BOOL.fieldOf("playerFace").forGetter { it.playerFace },
                Codec.BOOL.fieldOf("arrowFace").forGetter { it.arrowFace },
            ).apply(i) { icon, arrowIcon, size, entityIconOffset, playerFace, arrowFace ->
                MarkerIcon(icon, arrowIcon.getOrNull(), size, entityIconOffset, playerFace, arrowFace)
            }
        }

        fun read(buf: FriendlyByteBuf) = MarkerIcon(
            iconTexture = buf.readResourceLocation(),
            arrowTexture = buf.readNullable { it.readResourceLocation() },
            size = buf.readFloat(),
            entityIconOffset = buf.readDouble(),
            playerFace = buf.readBoolean(),
            arrowFace = buf.readBoolean(),
        )
    }
}