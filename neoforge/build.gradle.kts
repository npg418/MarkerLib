import com.npg418.gradle.modding.*
import org.slf4j.event.Level

plugins {
    alias(conventions.plugins.common.convention)
    alias(libs.plugins.moddev.gradle)
}

neoForge {
    version = neoforgeLibs.neoforge.version.get()

    parchment {
        minecraftVersion = libs.versions.parchment.minecraft
        mappingsVersion = libs.versions.parchment.mapping
    }

    runs {
        create("client") {
            client()
            systemProperty("neoforge.enabledGameTestNamespaces", prop("mod_id").get())
        }

        create("server") {
            server()
            systemProperty("neoforge.enabledGameTestNamespaces", prop("mod_id").get())
        }

        create("datagen") {
            data()
            programArguments.addAll(
                "--mod",
                prop("mod_id").get(),
                "--all",
                "--output",
                file("src/main/generated/").absolutePath,
                "--existing",
                file("src/main/resources/").absolutePath
            )
        }

        configureEach {
            systemProperty("forge.logging.markers", "REGISTRIES")
            logLevel = Level.DEBUG

            jvmArguments.addAll(hotswapJvmArgs)

            ideName = "NeoForge: Run $name"
        }
    }

    mods {
        register(prop("mod_id").get()) {
            sourceSet(sourceSets.main.get())
            sourceSet(commonSrc)
        }
    }
}

sourceSets.main {
    resources.srcDir("src/main/generated")
}

val generateModsToml = tasks.register<NeoForgeModsTomlTask>("generateModsToml") {
    description = "Generates neoforge mod metadata"
    outputFile = generatedResourcesDir.map { it.file("META-INF/neoforge.mods.toml") }

    toml {
        modLoader = "kotlinforforge"
        loaderVersion = neoforgeLibs.kotlinforforge.versionRange
        license = prop("mod_license")

        mod(prop("mod_id").get()) {
            version = prop("mod_version")
            displayName = prop("mod_name")
            authors = prop("mod_authors")
            description = prop("mod_description")
        }

        prop("mod_id").get().let {
            mixin("$it.mixins.json")
            mixin("$it.neoforge.mixins.json")
        }

        dependencies(prop("mod_id").get()) {
            depends("neoforge") {
                type = DependencyType.REQUIRED
                versionRange = neoforgeLibs.neoforge.versionRange
                ordering = Ordering.NONE
                side = Side.BOTH
            }
            depends("minecraft") {
                type = DependencyType.REQUIRED
                versionRange = libs.minecraft.versionRange
                ordering = Ordering.NONE
                side = Side.BOTH
            }
        }
    }
}
registerMetadataTask(generateModsToml)

repositories {
    maven("https://thedarkcolour.github.io/KotlinForForge") {
        name = "Kotlin for Forge"
    }
    maven("https://cursemaven.com") {
        name = "CurseMaven"
    }
}

dependencies {
    implementation(projects.common)

    implementation(neoforgeLibs.kotlinforforge)

    runtimeOnly(neoforgeLibs.configured)
}