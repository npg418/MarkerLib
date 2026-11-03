plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    mavenCentral()
}

dependencies {
    implementation(libs.nightconfig.toml)
    implementation(libs.plugins.idea.ext.map { "${it.pluginId}:${it.pluginId}.gradle.plugin:${it.version}" })
}