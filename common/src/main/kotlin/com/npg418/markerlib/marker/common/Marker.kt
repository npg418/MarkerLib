package com.npg418.markerlib.marker.common

import com.npg418.markerlib.marker.common.network.readVec3
import com.npg418.markerlib.marker.common.network.writeVec3
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.NbtUtils
import net.minecraft.nbt.Tag
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.resources.ResourceLocation
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
                0 -> Fixed(positions = buf.readList { it.readVec3() })
                1 -> Entity(
                    uuids = buf.readCollection({ HashSet<UUID>(it) }) { b -> b.readUUID() },
                    yOffset = buf.readDouble()
                )

                else -> error("unknown target type")
            }

            fun fromTag(t: CompoundTag): Target = when (t.getString("type")) {
                "fixed" -> Fixed(
                    positions = t.getList("positions", Tag.TAG_COMPOUND.toInt()).map {
                        it as CompoundTag
                        Vec3(it.getDouble("x"), it.getDouble("y"), it.getDouble("z"))
                    },
                )

                "entity" -> Entity(
                    uuids = t.getList("uuids", Tag.TAG_INT_ARRAY.toInt()).map { NbtUtils.loadUUID(it) }.toSet(),
                    selector = t.getString("selector"),
                    yOffset = t.getDouble("yOffset")
                )

                else -> error("unknown target type")
            }
        }

        fun write(buf: FriendlyByteBuf)
        fun toTag(): CompoundTag

        data class Fixed(val positions: List<Vec3>) : Target {
            override fun write(buf: FriendlyByteBuf) {
                buf.writeByte(0)
                buf.writeCollection(positions) { b, v -> b.writeVec3(v) }
            }

            override fun toTag() = CompoundTag().apply {
                putString("type", "fixed")
                put("positions", ListTag().apply {
                    positions.forEach {
                        add(CompoundTag().apply {
                            putDouble("x", it.x)
                            putDouble("y", it.y)
                            putDouble("z", it.z)
                        })
                    }
                })
            }
        }

        data class Entity(val uuids: Set<UUID>, val selector: String = "", val yOffset: Double = 0.5) :
            Target {
            override fun write(buf: FriendlyByteBuf) {
                buf.writeByte(1)
                buf.writeCollection(uuids) { b, v -> b.writeUUID(v) }
                buf.writeDouble(yOffset)
            }

            override fun toTag() = CompoundTag().apply {
                putString("type", "entity")
                putString("selector", selector)
                put("uuids", ListTag().apply { uuids.forEach { add(NbtUtils.createUUID(it)) } })
                putDouble("yOffset", yOffset)
            }
        }
    }
}