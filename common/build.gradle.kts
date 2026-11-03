plugins {
    alias(conventions.plugins.common.convention)
    alias(libs.plugins.moddev.gradle)
    alias(libs.plugins.fabric.loom.companion)
}

neoForge {
    neoFormVersion = libs.versions.neoform.get()

    parchment {
        minecraftVersion = libs.versions.parchment.minecraft
        mappingsVersion = libs.versions.parchment.mapping
    }
}

dependencies {
    compileOnly(kotlin("reflect"))
    compileOnly(libs.sponge.mixin)
}