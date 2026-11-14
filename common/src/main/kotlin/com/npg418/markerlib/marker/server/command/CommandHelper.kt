package com.npg418.markerlib.marker.server.command

import com.mojang.brigadier.arguments.StringArgumentType.getString
import com.mojang.brigadier.arguments.StringArgumentType.word
import com.mojang.brigadier.builder.RequiredArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType
import com.mojang.brigadier.suggestion.SuggestionProvider
import com.npg418.markerlib.marker.server.MarkerService
import com.npg418.markerlib.marker.server.ServerMarker
import com.npg418.markerlib.marker.server.ServerMarkers
import com.npg418.markerlib.marker.shared.Marker
import com.npg418.markerlib.marker.shared.MarkerTarget
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands.argument
import net.minecraft.commands.SharedSuggestionProvider
import net.minecraft.commands.arguments.coordinates.Coordinates
import net.minecraft.commands.arguments.coordinates.Vec3Argument.getVec3
import net.minecraft.commands.arguments.coordinates.Vec3Argument.vec3
import net.minecraft.network.chat.Component
import net.minecraft.world.phys.Vec3

internal typealias Ctx = CommandContext<CommandSourceStack>

internal object CommandErrors {
    val NOT_FOUND = DynamicCommandExceptionType { Component.literal("Marker not found: $it") }
    val EXISTS = DynamicCommandExceptionType { Component.literal("Marker already exists: $it") }
    val BAD_COLOR = SimpleCommandExceptionType(Component.literal("Color must be RRGGBB (hex)"))
    val NOT_FIXED = SimpleCommandExceptionType(Component.literal("Marker target is not fixed points"))
    val NOT_ENTITIES = SimpleCommandExceptionType(Component.literal("Marker target is not an entity selector"))
    val NO_SUCH_POINT = SimpleCommandExceptionType(Component.literal("No matching point"))
    val LAST_POINT =
        SimpleCommandExceptionType(Component.literal("Cannot remove the last point; use 'remove <id>' instead"))
}

private const val MAX_POINTS = 16

internal fun positionsNode(
    index: Int = 1,
    suggestions: ((Ctx, Int) -> Collection<String>)? = null,
    action: (Ctx, List<Vec3>) -> Int,
): RequiredArgumentBuilder<CommandSourceStack, Coordinates> {
    val node = argument("pos$index", vec3()).executes { ctx ->
        action(ctx, (1..index).map { getVec3(ctx, "pos$it") })
    }
    if (suggestions != null) {
        node.suggests { ctx, builder ->
            SharedSuggestionProvider.suggest(
                suggestions(ctx, index),
                builder,
            )
        }
    }
    if (index < MAX_POINTS) node.then(positionsNode(index + 1, suggestions, action))
    return node
}

internal fun Ctx.rawArgument(): String = this.nodes.last().range.get(this.input)

private val idSuggestions = SuggestionProvider<CommandSourceStack> { ctx, builder ->
    SharedSuggestionProvider.suggest(ServerMarkers.of(ctx.source.server).all.map { it.id }, builder)
}
internal fun idArgumentNode() = argument("id", word()).suggests(idSuggestions)

internal val Ctx.id get() = getString(this, "id")

internal fun updateServerMarker(ctx: Ctx, refresh: Boolean = false, transform: (ServerMarker) -> ServerMarker): Int {
    val id = ctx.id
    val server = ctx.source.server
    val marker = ServerMarkers.of(server)[id] ?: throw CommandErrors.NOT_FOUND.create(id)
    val transformed = transform(marker)
    MarkerService.put(server, transformed)
    if (refresh) {
        MarkerService.refreshVisibleTo(server, transformed)
        MarkerService.refreshEntities(server, transformed)
    }
    ctx.source.sendSuccess({ Component.literal("Updated marker $id") }, true)
    return 1
}

internal fun updateMarker(ctx: Ctx, transform: (Marker) -> Marker) =
    updateServerMarker(ctx) { it.copy(marker = transform(it.marker)) }

internal fun updateTarget(ctx: Ctx, transform: (MarkerTarget) -> MarkerTarget) =
    updateMarker(ctx) { it.copy(target = transform(it.target)) }

private const val POINT_EPSILON = 0.001
internal fun samePoint(a: Vec3, b: Vec3): Boolean = a.distanceToSqr(b) <= POINT_EPSILON * POINT_EPSILON

internal fun parseColor(s: String): Int {
    val hex = s.removePrefix("#")
    if (hex.length != 6 || !hex.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }) {
        throw CommandErrors.BAD_COLOR.create()
    }
    return hex.toInt(16)
}