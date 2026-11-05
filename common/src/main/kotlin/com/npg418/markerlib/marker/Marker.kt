package com.npg418.markerlib.marker

import com.npg418.markerlib.network.readVec3
import com.npg418.markerlib.network.writeVec3
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.core.BlockPos
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.Level
import net.minecraft.world.phys.Vec3
import java.util.*

data class Marker(
    val id: String,
    val target: Target,
    val icon: ResourceLocation = DEFAULT_ICON,
    val arrowIcon: ResourceLocation = DEFAULT_ARROW,
    val color: Int = 0xFFFF3030.toInt(),
    val label: String? = null,
    val visibleTo: Set<UUID> = emptySet(),
) {
    companion object {
        val DEFAULT_ICON = ResourceLocation("markerlib", "textures/gui/marker.png")
        val DEFAULT_ARROW = ResourceLocation("markerlib", "textures/gui/arrow.png")

        fun read(buf: FriendlyByteBuf) = Marker(
            id = buf.readUtf(),
            target = Target.read(buf),
            icon = buf.readResourceLocation(),
            arrowIcon = buf.readResourceLocation(),
            color = buf.readInt(),
            label = buf.readNullable { it.readUtf() },
        )
    }

    fun isVisibleTo(player: UUID) = visibleTo.isEmpty() || player in visibleTo

    fun write(buf: FriendlyByteBuf) {
        buf.writeUtf(id)
        target.write(buf)
        buf.writeResourceLocation(icon)
        buf.writeResourceLocation(arrowIcon)
        buf.writeInt(color)
        buf.writeNullable(label) { b, s -> b.writeUtf(s) }
    }

    sealed interface Target {
        companion object {
            fun read(buf: FriendlyByteBuf): Target = when (buf.readByte().toInt()) {
                0 -> Fixed(buf.readVec3())
                1 -> Entity(buf.readUUID(), buf.readDouble())
                2 -> Block(buf.readBlockPos())
                else -> error("unknown target type")
            }
        }

        fun resolve(level: Level, partialTick: Float): Vec3?
        fun write(buf: FriendlyByteBuf)

        data class Fixed(val pos: Vec3) : Target {
            override fun resolve(level: Level, partialTick: Float) = pos
            override fun write(buf: FriendlyByteBuf) {
                buf.writeByte(0)
                buf.writeVec3(pos)
            }
        }

        data class Entity(val uuid: UUID, val yOffset: Double = 0.5) : Target {
            override fun resolve(level: Level, partialTick: Float): Vec3? {
                val e = (level as? ClientLevel)?.entitiesForRendering()
                    ?.firstOrNull { it.uuid == uuid } ?: return null
                return e.getPosition(partialTick).add(0.0, e.bbHeight + yOffset, 0.0)
            }

            override fun write(buf: FriendlyByteBuf) {
                buf.writeByte(1)
                buf.writeUUID(uuid)
                buf.writeDouble(yOffset)
            }
        }

        data class Block(val pos: BlockPos) : Target {
            override fun resolve(level: Level, partialTick: Float): Vec3 = Vec3.atCenterOf(pos)
            override fun write(buf: FriendlyByteBuf) {
                buf.writeByte(2); buf.writeBlockPos(pos)
            }
        }
    }
}