package com.npg418.markerlib.marker.client

import com.npg418.markerlib.marker.shared.Marker
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.world.entity.Entity
import java.util.*

class ResolveContext(level: ClientLevel, markers: Collection<Marker>) {
    private val entities: Map<UUID, Entity>

    init {
        val wanted = markers.flatMap { it.entities }.toSet()
        val self = Minecraft.getInstance().player
        entities = if (wanted.isEmpty()) {
            emptyMap()
        } else {
            buildMap { level.entitiesForRendering().forEach { if (it.uuid in wanted && it !== self) put(it.uuid, it) } }
        }
    }

    fun getPositions(marker: Marker, partialTick: Float) = marker.entities.mapNotNull(entities::get)
        .map { it.getPosition(partialTick).add(0.0, it.bbHeight + marker.icon.entityIconOffset, 0.0) }
}