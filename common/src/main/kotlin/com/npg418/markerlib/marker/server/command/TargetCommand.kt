package com.npg418.markerlib.marker.server.command

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.npg418.markerlib.marker.common.DimensionalPosition
import com.npg418.markerlib.marker.format
import com.npg418.markerlib.marker.isClose
import com.npg418.markerlib.marker.server.MarkerService
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands.argument
import net.minecraft.commands.Commands.literal
import net.minecraft.commands.arguments.EntityArgument.entities

object TargetCommand {
    fun node(): LiteralArgumentBuilder<CommandSourceStack> = literal("target").then(
        idArgumentNode().suggests(idSuggestions).then(addNode()).then(removeNode()),
    )

    private val Ctx.dimension get() = source.level.dimension().location()

    private fun addNode() = literal("add").then(
        argument("selector", entities()).executes { ctx ->
            updateServerEntry(ctx) { it.copy(entitySelector = ctx.rawArgument("selector")) }
        },
    ).then(
        positionsAugmentNode { ctx, i ->
            updateMarker(ctx) {
                copy(
                    positions =
                    positions + ctx.positions(i).map { DimensionalPosition(it, ctx.dimension) },
                )
            }
        },
    )

    private fun removeNode() = literal("remove").then(
        literal("selector").executes { ctx ->
            updateServerEntry(ctx, refresh = true) { it.copy(entitySelector = null) }
        },
    ).then(
        positionsAugmentNode(suggestions = ::existingPoints) { ctx, i ->
            updateMarker(ctx) {
                val rest = positions.filterNot { existing ->
                    ctx.positions(i).any { specified ->
                        existing.isClose(DimensionalPosition(specified, ctx.dimension))
                    }
                }
                if (rest.size == positions.size) throw CommandErrors.NO_SUCH_POINT.create()
                copy(positions = rest)
            }
        },
    )

    private fun existingPoints(ctx: Ctx, i: Int): List<String> {
        val entry = MarkerService.of(ctx.source.server)[ctx.id] ?: return emptyList()
        return entry.marker.positions.filter { p ->
            p.dimension == ctx.dimension && ctx.positions(i - 1).none { it.isClose(p.pos) }
        }.map { it.pos.format() }
    }
}