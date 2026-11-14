package com.npg418.markerlib.marker.server.command

import com.mojang.brigadier.arguments.StringArgumentType.getString
import com.mojang.brigadier.arguments.StringArgumentType.word
import com.mojang.brigadier.builder.RequiredArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType
import com.mojang.brigadier.suggestion.SuggestionProvider
import com.npg418.markerlib.marker.server.MarkerService
import com.npg418.markerlib.marker.server.ServerEntry
import com.npg418.markerlib.marker.shared.Marker
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
    val NO_SUCH_POINT = SimpleCommandExceptionType(Component.literal("No matching point"))
}

internal fun Ctx.rawArgument(name: String): String? = nodes.lastOrNull { it.node.name == name }?.range?.get(input)

internal val idSuggestions = SuggestionProvider<CommandSourceStack> { ctx, builder ->
    SharedSuggestionProvider.suggest(MarkerService.of(ctx.source.server).all.map { it.id }, builder)
}

internal fun idArgumentNode() = argument("id", word())
internal val Ctx.id get() = getString(this, "id")

private const val MAX_POINTS = 16
internal fun positionsAugmentNode(
    index: Int = 1,
    suggestions: ((Ctx, Int) -> Collection<String>)? = null,
    action: ((Ctx, Int) -> Int),
): RequiredArgumentBuilder<CommandSourceStack, Coordinates> {
    val node = argument("pos$index", vec3()).executes { action(it, index) }
    if (suggestions != null) {
        node.suggests { ctx, builder ->
            SharedSuggestionProvider.suggest(
                suggestions(ctx, index),
                builder,
            )
        }
    }
    if (index < MAX_POINTS) node.then(positionsAugmentNode(index + 1, suggestions, action))
    return node
}

internal fun Ctx.positions(index: Int): List<Vec3> = (1..index).map { getVec3(this, "pos$it") }

internal fun updateServerEntry(ctx: Ctx, refresh: Boolean = false, transform: (ServerEntry) -> ServerEntry): Int {
    val id = ctx.id
    val server = ctx.source.server
    val service = MarkerService.of(server)
    val marker = service[id] ?: throw CommandErrors.NOT_FOUND.create(id)
    val transformed = transform(marker)
    service.put(transformed)
    if (refresh) {
        service.refreshVisibleTo(transformed)
        service.refreshEntities(transformed)
    }
    ctx.source.sendSuccess({ Component.literal("Updated marker $id") }, true)
    return 1
}

internal fun updateMarker(ctx: Ctx, transform: Marker.() -> Marker) =
    updateServerEntry(ctx) { it.copy(marker = transform(it.marker)) }

private const val POINT_EPSILON = 0.001
internal fun samePoint(a: Vec3, b: Vec3): Boolean = a.distanceToSqr(b) <= POINT_EPSILON * POINT_EPSILON

internal fun parseColor(s: String): Int {
    val hex = s.removePrefix("#")
    if (hex.length != 6 || !hex.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }) {
        throw CommandErrors.BAD_COLOR.create()
    }
    return hex.toInt(16)
}