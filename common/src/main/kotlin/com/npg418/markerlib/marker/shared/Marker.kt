package com.npg418.markerlib.marker.shared

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
    val target: MarkerTarget,
    val icon: ResourceLocation = DEFAULT_ICON,
    val arrowIcon: ResourceLocation? = DEFAULT_ARROW,
    val size: Float = 1f,
    val color: Int = 0xFFFF3030.toInt(),
    val label: String? = null,
    val showDistance: Boolean = true,
) {
    companion object {
        val DEFAULT_ICON = ResourceLocation("markerlib", "textures/gui/marker.png")
        val DEFAULT_ARROW = ResourceLocation("markerlib", "textures/gui/arrow.png")

        fun read(buf: FriendlyByteBuf) = Marker(
            id = buf.readUtf(),
            target = MarkerTarget.read(buf),
            icon = buf.readResourceLocation(),
            arrowIcon = buf.readNullable { it.readResourceLocation() },
            size = buf.readFloat(),
            color = buf.readInt(),
            label = buf.readNullable { it.readUtf() },
            showDistance = buf.readBoolean(),
        )

        fun fromTag(tag: CompoundTag) = Marker(
            id = tag.getString("id"),
            target = MarkerTarget.fromTag(tag.getCompound("target")),
            icon = ResourceLocation(tag.getString("icon")),
            arrowIcon = tag.getNullable("arrow") { ResourceLocation(getString(it)) },
            size = tag.getNullable("size", CompoundTag::getFloat) ?: 1f,
            color = tag.getInt("color"),
            label = tag.getNullable("label", CompoundTag::getString),
            showDistance = tag.getBoolean("showDistance"),
        )
    }

    fun write(buf: FriendlyByteBuf) {
        buf.writeUtf(id)
        target.write(buf)
        buf.writeResourceLocation(icon)
        buf.writeNullable(arrowIcon) { b, r -> b.writeResourceLocation(r) }
        buf.writeFloat(size)
        buf.writeInt(color)
        buf.writeNullable(label) { b, s -> b.writeUtf(s) }
        buf.writeBoolean(showDistance)
    }

    fun toTag() = CompoundTag().apply {
        putString("id", this@Marker.id)
        put("target", target.toTag())
        putString("icon", icon.toString())
        putNullable("arrow", arrowIcon) { k, v -> putString(k, v.toString()) }
        putFloat("size", size)
        putInt("color", color)
        putNullable("label", label, CompoundTag::putString)
        putBoolean("showDistance", showDistance)
    }
}

sealed interface MarkerTarget {
    companion object {
        fun read(buf: FriendlyByteBuf): MarkerTarget = when (buf.readByte().toInt()) {
            0 -> Fixed(buf.readList { it.readVec3() })
            1 -> Entities(buf.readCollection(::HashSet) { it.readUUID() }, buf.readDouble())
            else -> error("unknown target type")
        }

        fun fromTag(tag: CompoundTag): MarkerTarget = when (tag.getString("type")) {
            "fixed" -> Fixed(tag.getList("positions", Tag.TAG_COMPOUND.toInt()).map { (it as CompoundTag).toVec3() })

            "entity" -> Entities(
                tag.getList("entities", Tag.TAG_INT_ARRAY.toInt()).map { NbtUtils.loadUUID(it) }.toSet(),
                tag.getDouble("yOffset"),
            )

            else -> error("unknown target type")
        }
    }

    fun write(buf: FriendlyByteBuf)
    fun toTag(): CompoundTag

    data class Fixed(val positions: List<Vec3>) : MarkerTarget {
        override fun write(buf: FriendlyByteBuf) {
            buf.writeByte(0)
            buf.writeCollection(positions) { b, v -> b.writeVec3(v) }
        }

        override fun toTag() = CompoundTag().apply {
            putString("type", "fixed")
            put("positions", ListTag().apply { positions.forEach { add(it.toTag()) } })
        }
    }

    data class Entities(val entities: Set<UUID>, val yOffset: Double = 0.5) : MarkerTarget {
        override fun write(buf: FriendlyByteBuf) {
            buf.writeByte(1)
            buf.writeCollection(entities) { b, u -> b.writeUUID(u) }
            buf.writeDouble(yOffset)
        }

        override fun toTag() = CompoundTag().apply {
            putString("type", "entity")
            put("entities", ListTag().apply { entities.forEach { add(NbtUtils.createUUID(it)) } })
            putDouble("yOffset", yOffset)
        }
    }
}