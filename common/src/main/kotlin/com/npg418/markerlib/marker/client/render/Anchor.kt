package com.npg418.markerlib.marker.client.render

import com.npg418.markerlib.marker.common.Marker
import com.npg418.markerlib.marker.common.MarkerIcon
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.world.entity.Entity
import net.minecraft.world.phys.Vec3
import java.util.*

class Anchor(val pos: Vec3, val entity: Entity? = null) {
    fun iconPos(icon: MarkerIcon): Vec3 = if (entity == null) pos else pos.add(0.0, icon.entityIconOffset, 0.0)
}

class AnchorResolver(private val level: ClientLevel, markers: Collection<Marker>) {
    private val entities: Map<UUID, Entity> = run {
        val wanted: Set<UUID> = markers.flatMapTo(HashSet()) { marker ->
            marker.entities.filter { it.value.dimension == level.dimension().location() }.map { it.key }
        }
        if (wanted.isEmpty()) return@run emptyMap()
        val self = Minecraft.getInstance().player
        buildMap {
            level.entitiesForRendering().forEach { if (it.uuid in wanted && it !== self) put(it.uuid, it) }
        }
    }

    fun resolve(marker: Marker, partialTicks: Float): List<Anchor> = buildList {
        marker.positions.filter { it.dimension == level.dimension().location() }.mapTo(this) { Anchor(it.pos) }
        marker.entities.mapNotNullTo(this) { (id, _) ->
            entities[id]?.let { Anchor(it.getPosition(partialTicks).add(0.0, it.bbHeight.toDouble(), 0.0), it) }
        }
    }
}