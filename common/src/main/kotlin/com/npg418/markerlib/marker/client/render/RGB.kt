package com.npg418.markerlib.marker.client.render

import net.minecraft.util.Mth

@JvmInline
value class RGB(val rgb: Int) {
    init {
        require(rgb in 0x000000..0xFFFFFF) { "value 0x%X is invalid rgb color".format(rgb) }
    }

    constructor(r: Int, g: Int, b: Int) : this((r shl 16) or (g shl 8) or b)

    val r get() = rgb shr 16 and 0xFF
    val g get() = rgb shr 8 and 0xFF
    val b get() = rgb and 0xFF

    fun lerp(delta: Float, other: RGB): RGB {
        require(delta in 0f..1f) { "lerp delta must be in 0..1" }
        return RGB(Mth.lerpInt(delta, r, other.r), Mth.lerpInt(delta, g, other.g), Mth.lerpInt(delta, b, other.b))
    }
}