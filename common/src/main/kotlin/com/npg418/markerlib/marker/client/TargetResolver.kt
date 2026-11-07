package com.npg418.markerlib.marker.client

import com.npg418.markerlib.marker.common.Marker
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.world.entity.Entity
import net.minecraft.world.phys.Vec3
import java.util.*

class ResolveContext(level: ClientLevel, markers: Collection<Marker>) {
    private val entities: Map<UUID, Entity>

    init {
        val wanted = markers.mapNotNullTo(HashSet()) { (it.target as? Marker.Target.Entity)?.uuid }
        entities = if (wanted.isEmpty()) {
            emptyMap()
        } else {
            buildMap { level.entitiesForRendering().forEach { if (it.uuid in wanted) put(it.uuid, it) } }
        }
    }

    fun entity(uuid: UUID): Entity? = entities[uuid]
}

fun Marker.Target.resolve(ctx: ResolveContext, partialTick: Float): Vec3? = when (this) {
    is Marker.Target.Fixed -> pos

    is Marker.Target.Entity -> ctx.entity(uuid)?.let {
        it.getPosition(partialTick).add(0.0, it.bbHeight + yOffset, 0.0)
    }

    is Marker.Target.Block -> Vec3.atCenterOf(pos)
}