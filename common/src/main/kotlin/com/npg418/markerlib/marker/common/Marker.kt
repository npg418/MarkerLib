package com.npg418.markerlib.marker

import com.npg418.markerlib.network.readVec3
import com.npg418.markerlib.network.writeVec3
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.NbtUtils
import net.minecraft.nbt.Tag
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
    val size: Float = 1f,
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
            size = buf.readFloat(),
            color = buf.readInt(),
            label = buf.readNullable { it.readUtf() },
        )

        fun fromTag(t: CompoundTag) = Marker(
            id = t.getString("id"),
            target = Target.fromTag(t.getCompound("target")),
            icon = ResourceLocation(t.getString("icon")),
            arrowIcon = ResourceLocation(t.getString("arrow")),
            size = if (t.contains("size")) t.getFloat("size") else 1f,
            color = t.getInt("color"),
            label = if (t.contains("label")) t.getString("label") else null,
            visibleTo = t.getList("visibleTo", Tag.TAG_INT_ARRAY.toInt()).map { NbtUtils.loadUUID(it) }.toSet(),
        )
    }

    fun isVisibleTo(player: UUID) = visibleTo.isEmpty() || player in visibleTo

    fun write(buf: FriendlyByteBuf) {
        buf.writeUtf(id)
        target.write(buf)
        buf.writeResourceLocation(icon)
        buf.writeResourceLocation(arrowIcon)
        buf.writeFloat(size)
        buf.writeInt(color)
        buf.writeNullable(label) { b, s -> b.writeUtf(s) }
    }

    fun toTag() = CompoundTag().apply {
        putString("id", this@Marker.id)
        put("target", target.toTag())
        putString("icon", icon.toString())
        putString("arrow", arrowIcon.toString())
        putFloat("size", size)
        putInt("color", color)
        label?.let { putString("label", it) }
        put("visibleTo", ListTag().apply { visibleTo.forEach { add(NbtUtils.createUUID(it)) } })
    }

    sealed interface Target {
        companion object {
            fun read(buf: FriendlyByteBuf): Target = when (buf.readByte().toInt()) {
                0 -> Fixed(buf.readVec3())
                1 -> Entity(buf.readUUID(), buf.readDouble())
                2 -> Block(buf.readBlockPos())
                else -> error("unknown target type")
            }

            fun fromTag(t: CompoundTag): Target = when (t.getString("type")) {
                "fixed" -> Fixed(Vec3(t.getDouble("x"), t.getDouble("y"), t.getDouble("z")))
                "entity" -> Entity(t.getUUID("uuid"), t.getDouble("yOffset"))
                "block" -> Block(NbtUtils.readBlockPos(t.getCompound("pos")))
                else -> error("unknown target type")
            }
        }

        fun resolve(level: Level, partialTick: Float): Vec3?

        fun write(buf: FriendlyByteBuf)
        fun toTag(): CompoundTag

        data class Fixed(val pos: Vec3) : Target {
            override fun resolve(level: Level, partialTick: Float) = pos

            override fun write(buf: FriendlyByteBuf) {
                buf.writeByte(0)
                buf.writeVec3(pos)
            }

            override fun toTag() = CompoundTag().apply {
                putString("type", "fixed")
                putDouble("x", pos.x)
                putDouble("y", pos.y)
                putDouble("z", pos.z)
            }
        }

        data class Entity(val uuid: UUID, val yOffset: Double = 0.5) : Target {
            override fun resolve(level: Level, partialTick: Float): Vec3? {
                val e = (level as? ClientLevel)?.entitiesForRendering()?.firstOrNull { it.uuid == uuid } ?: return null
                return e.getPosition(partialTick).add(0.0, e.bbHeight + yOffset, 0.0)
            }

            override fun write(buf: FriendlyByteBuf) {
                buf.writeByte(1)
                buf.writeUUID(uuid)
                buf.writeDouble(yOffset)
            }

            override fun toTag() = CompoundTag().apply {
                putString("type", "entity")
                putUUID("uuid", uuid)
                putDouble("yOffset", yOffset)
            }
        }

        data class Block(val pos: BlockPos) : Target {
            override fun resolve(level: Level, partialTick: Float): Vec3 = Vec3.atCenterOf(pos)

            override fun write(buf: FriendlyByteBuf) {
                buf.writeByte(2)
                buf.writeBlockPos(pos)
            }

            override fun toTag() = CompoundTag().apply {
                putString("type", "block")
                put("pos", NbtUtils.writeBlockPos(pos))
            }
        }
    }
}