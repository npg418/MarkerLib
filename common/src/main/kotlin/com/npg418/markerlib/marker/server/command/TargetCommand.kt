package com.npg418.markerlib.marker.server.command

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.npg418.markerlib.marker.server.ServerMarkers
import com.npg418.markerlib.marker.shared.MarkerTarget
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands.literal
import net.minecraft.commands.arguments.coordinates.Vec3Argument.getVec3
import net.minecraft.world.phys.Vec3
import java.math.BigDecimal
import java.math.RoundingMode

object TargetCommand {
    val node: LiteralArgumentBuilder<CommandSourceStack> = literal("target").then(
        literal("add").then(
            idArgumentNode.then(
                positionsNode { ctx, positions ->
                    updateTarget(ctx) {
                        if (it !is MarkerTarget.Fixed) throw CommandErrors.NOT_FIXED.create()
                        it.copy(positions = it.positions + positions)
                    }
                },
            ),
        ),
    ).then(
        literal("remove").then(
            idArgumentNode.then(
                positionsNode(suggestions = ::existingPoints) { ctx, positions ->
                    updateTarget(ctx) {
                        if (it !is MarkerTarget.Fixed) throw CommandErrors.NOT_FIXED.create()
                        val rest = it.positions.filterNot { existing ->
                            positions.any { specified -> samePoint(existing, specified) }
                        }
                        if (rest.size == it.positions.size) throw CommandErrors.NO_SUCH_POINT.create()
                        if (rest.isEmpty()) throw CommandErrors.LAST_POINT.create()
                        it.copy(positions = rest)
                    }
                },
            ),
        ),
    )

    private fun existingPoints(ctx: Ctx, index: Int): List<String> {
        val entry = ServerMarkers.of(ctx.source.server)[ctx.id] ?: return emptyList()
        val fixed = entry.marker.target as? MarkerTarget.Fixed ?: return emptyList()
        val chosen = (1 until index).map { getVec3(ctx, "pos$it") }
        return fixed.positions.filter { p -> chosen.none { samePoint(p, it) } }.map(::format)
    }

    private fun format(v: Vec3) = "${num(v.x)} ${num(v.y)} ${num(v.z)}"

    private fun num(d: Double) =
        BigDecimal.valueOf(d).setScale(3, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
}