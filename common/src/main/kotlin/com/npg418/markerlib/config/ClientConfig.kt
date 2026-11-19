package com.npg418.markerlib.config

import com.npg418.markerlib.config.api.*

object ClientConfig : ConfigSpec(ConfigType.CLIENT) {
    @Name("rendering")
    object Rendering {
        @Comment(
            "Base marker icon size in GUI pixels.",
            "Actual rendered size is baseIconSize * size(per marker) * distanceScale px.",
        )
        val baseIconSize by entry(8) ranged 0..Int.MAX_VALUE

        @Comment(
            "Minimum margin (in GUI pixels) kept between off-screen indicators and the screen edges.",
            "Higher values push edge-clamped markers further inward toward the center.",
        )
        val edgeMargin by entry(24f) ranged 0f..100f

        @Comment(
            "Additional margin (in GUI pixels) added to 'edgeMargin' at the bottom of the screen.",
            "Prevents edge-clamped markers from overlapping the hotbar, health, and armor bars.",
        )
        val extraBottomMargin by entry(24f) ranged 0f..100f

        @Name("scale")
        object Scale {
            @Comment(
                "Reference distance (in blocks) at which a marker's distanceScale equals 1.0.",
                "Markers farther than this shrink down to 'minScale'; closer ones grow up to 'maxScale'.",
            )
            val refDistance by entry(8f) ranged 1f..1024f

            @Comment(
                "Minimum distance-based scale multiplier applied to distant markers.",
                "Prevents far-away markers from shrinking too small to see.",
            )
            val minScale by entry(0.4f) ranged 0.1f..1f

            @Comment(
                "Maximum distance-based scale multiplier applied to nearby markers.",
                "Prevents close-range markers from becoming overly large and obstructing the view.",
            )
            val maxScale by entry(1f) ranged 1f..4f

            @Comment(
                "Scale multiplier applied to marker label and distance text.",
                "Affects text size independently of the marker icon size.",
            )
            val textScale by entry(1f) ranged 0.25f..2f

            @Comment(
                "Enlarges on-screen markers when the camera FOV is narrowed below the 'FOV' video setting.",
                "Keeps markers readable while using a spyglass or a zoom mod. Edge arrows are not affected.",
            )
            val scaleWithZoom by entry(true)

            @Comment(
                "Upper bound of the zoom-based scale multiplier (1.0 = no enlargement).",
                "A spyglass narrows the FOV to roughly 1/10, so the multiplier is capped to keep markers from becoming huge.",
            )
            val maxZoomScale by entry(4f) ranged 1f..16f
        }

        @Name("crosshairFade")
        object CrosshairFade {
            @Comment(
                "Radius (in GUI pixels) around the crosshair within which markers gradually fade out.",
                "Helps keep the center of the screen clear while aiming. Set to 0 to disable fading.",
            )
            val radius by entry(48f) ranged 0f..400f

            @Comment(
                "Opacity (alpha) of a marker positioned directly on the crosshair.",
                "Markers fade smoothly from 1.0 at the edge of 'radius' down to this value at the center.",
            )
            val minAlpha by entry(0.25f) ranged 0.05f..1f
        }
    }
}