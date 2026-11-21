package com.npg418.markerlib.marker.client.render

import com.npg418.markerlib.marker.common.Marker
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.world.entity.Entity
import net.minecraft.world.phys.Vec3
import java.util.*

class Anchor(val position: Vec3, val uuid: UUID? = null)

class AnchorResolver(level: ClientLevel, markers: Collection<Marker>) {
    private val dimension = level.dimension().location()
    private val entities: Map<UUID, Entity> = run {
        val wanted: Set<UUID> = markers.flatMapTo(HashSet()) { marker ->
            marker.entitiesInDim(dimension).keys
        }
        if (wanted.isEmpty()) return@run emptyMap()
        val self = Minecraft.getInstance().player
        buildMap {
            level.entitiesForRendering().forEach { if (it.uuid in wanted && it !== self) put(it.uuid, it) }
        }
    }

    fun resolve(marker: Marker, partialTicks: Float): List<Anchor> = buildList {
        marker.positionsInDim(dimension).mapTo(this) { Anchor(it.pos) }
        marker.entitiesInDim(dimension).mapTo(this) { (uuid, dPos) ->
            val e = entities[uuid]
            if (e == null) {
                Anchor(dPos.pos, uuid)
            } else {
                Anchor(e.getPosition(partialTicks).add(0.0, e.bbHeight.toDouble(), 0.0), uuid)
            }
        }
    }
}