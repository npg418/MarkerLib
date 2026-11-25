package com.npg418.markerlib.client.render

import com.npg418.markerlib.client.ClientMarkers
import com.npg418.markerlib.common.Marker
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.Entity
import java.util.*

internal object MarkerLocator {
    private val loaded = mutableMapOf<UUID, Entity>()
    private lateinit var dimension: ResourceLocation
    private var self: UUID? = null
    private var partialTicks = 0f

    fun beginFrame(level: ClientLevel, self: UUID, partialTicks: Float) {
        this.dimension = level.dimension().location()
        this.self = self
        this.partialTicks = partialTicks
        if (ClientMarkers.trackedEntities.isNotEmpty()) {
            for (entity in level.entitiesForRendering()) {
                if (entity.uuid in ClientMarkers.trackedEntities) loaded[entity.uuid] = entity
            }
        }
    }

    fun locate(marker: Marker, result: MutableList<WorldPoint>) {
        for ((pos, dim) in marker.positions) {
            if (dim == dimension) result += WorldPoint(pos)
        }
        for ((uuid, last) in marker.entities) {
            if (last.dimension != dimension || uuid == Minecraft.getInstance().player?.uuid) continue
            val entity = loaded[uuid]
            result += if (entity == null) {
                WorldPoint(last.pos, uuid)
            } else {
                WorldPoint(entity.headPosition, uuid)
            }
        }
    }

    private val Entity.headPosition get() = getPosition(partialTicks).add(0.0, bbHeight.toDouble(), 0.0)
}