import com.npg418.gradle.modding.version

plugins {
    alias(conventions.plugins.common.convention)
    alias(libs.plugins.moddev.gradle.legacy)
    alias(libs.plugins.fabric.loom.companion)
}

legacyForge {
    mcpVersion = libs.minecraft.version.get()

    parchment {
        minecraftVersion = libs.versions.parchment.minecraft
        mappingsVersion = libs.versions.parchment.mapping
    }
}

dependencies {
    compileOnly(kotlin("reflect"))
    compileOnly(libs.sponge.mixin)
}