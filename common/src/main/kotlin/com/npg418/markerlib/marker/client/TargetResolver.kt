package com.npg418.markerlib.marker.client

import com.npg418.markerlib.marker.common.Marker
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.world.entity.Entity
import net.minecraft.world.phys.Vec3
import java.util.*

class ResolveContext(level: ClientLevel, markers: Collection<Marker>) {
    private val entities: Map<UUID, Entity>

    init {
        val wanted = HashSet<UUID>()
        markers.forEach { (it.target as? Marker.Target.Entity)?.let { t -> wanted += t.uuids } }
        val self = Minecraft.getInstance().player
        entities = if (wanted.isEmpty()) {
            emptyMap()
        } else {
            buildMap { level.entitiesForRendering().forEach { if (it.uuid in wanted && it !== self) put(it.uuid, it) } }
        }
    }

    fun entity(uuid: UUID): Entity? = entities[uuid]
}

fun Marker.Target.resolveAll(ctx: ResolveContext, partialTick: Float): List<Vec3> = when (this) {
    is Marker.Target.Fixed -> positions
    is Marker.Target.Entity -> uuids.mapNotNull(ctx::entity)
        .map { it.getPosition(partialTick).add(0.0, it.bbHeight + yOffset, 0.0) }
}