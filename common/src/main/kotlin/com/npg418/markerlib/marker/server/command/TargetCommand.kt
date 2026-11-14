package com.npg418.markerlib.marker.server.command

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.npg418.markerlib.marker.server.MarkerService
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands.argument
import net.minecraft.commands.Commands.literal
import net.minecraft.commands.arguments.EntityArgument.entities
import net.minecraft.world.phys.Vec3
import java.math.BigDecimal
import java.math.RoundingMode

object TargetCommand {
    fun node(): LiteralArgumentBuilder<CommandSourceStack> = literal("target").then(
        idArgumentNode().suggests(idSuggestions).then(addNode()).then(removeNode()),
    )

    private fun addNode() = literal("add").then(
        argument("selector", entities()).executes { ctx ->
            updateServerEntry(ctx) { it.copy(entitySelector = ctx.rawArgument("selector")) }
        },
    ).then(
        positionsAugmentNode { ctx, i ->
            updateMarker(ctx) { copy(positions = positions + ctx.positions(i)) }
        },
    )

    private fun removeNode() = literal("remove").then(
        literal("selector").executes { ctx ->
            updateServerEntry(ctx) { it.copy(entitySelector = null) }
        },
    ).then(
        positionsAugmentNode(suggestions = ::existingPoints) { ctx, i ->
            updateMarker(ctx) {
                val rest = positions.filterNot { existing ->
                    ctx.positions(i).any { specified -> samePoint(existing, specified) }
                }
                if (rest.size == positions.size) throw CommandErrors.NO_SUCH_POINT.create()
                copy(positions = rest)
            }
        },
    )

    private fun existingPoints(ctx: Ctx, i: Int): List<String> {
        val entry = MarkerService.of(ctx.source.server)[ctx.id] ?: return emptyList()
        return entry.marker.positions.filter { p -> ctx.positions(i - 1).none { samePoint(p, it) } }.map(::format)
    }

    private fun format(v: Vec3) = "${num(v.x)} ${num(v.y)} ${num(v.z)}"

    private fun num(d: Double) =
        BigDecimal.valueOf(d).setScale(3, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
}