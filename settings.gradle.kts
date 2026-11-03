pluginManagement {
    repositories {
        maven("https://maven.fabricmc.net/") {
            name = "Fabric"
        }
        gradlePluginPortal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    versionCatalogs {
        register("forgeLibs") {
            from(files("gradle/forgeLibs.versions.toml"))
        }
        register("fabricLibs") {
            from(files("gradle/fabricLibs.versions.toml"))
        }
    }
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "ExampleMod"

includeBuild("build-logic")

include("common")
include("forge")
include("fabric")
