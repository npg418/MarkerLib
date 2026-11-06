import com.npg418.gradle.modding.*

plugins {
    alias(conventions.plugins.common.convention)
    alias(libs.plugins.moddev.gradle.legacy)
}

legacyForge {
    version = "${libs.minecraft.version.get()}-${forgeLibs.forge.version.get()}"

    parchment {
        minecraftVersion = libs.versions.parchment.minecraft
        mappingsVersion = libs.versions.parchment.mapping
    }

    runs {
        create("client") {
            client()
            systemProperty("forge.enabledGameTestNamespaces", prop("mod_id").get())
        }

        create("server") {
            server()
            systemProperty("forge.enabledGameTestNamespaces", prop("mod_id").get())
        }

        create("datagen") {
            data()
            programArguments.addAll(
                "--mod",
                prop("mod_id").get(),
                "--all",
                "--output",
                file("src/main/generated").absolutePath,
                "--existing",
                file("src/main/resources").absolutePath,
            )
        }

        configureEach {
            systemProperty("forge.logging.markers", "REGISTRIES")
            systemProperty("forge.logging.console.level", "debug")

            jvmArguments.addAll(hotswapJvmArgs)

            ideName = "Forge: Run $name"
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

val generateModsToml =
    tasks.register<ForgeModsTomlTask>("generateModsToml") {
        description = "Generates forge mod metadata"
        outputFile = generatedResourcesDir.map { it.file("META-INF/mods.toml") }

        toml {
            modLoader = "kotlinforforge"
            loaderVersion = forgeLibs.kotlinforforge.versionRange
            license = prop("mod_license")

            mod(prop("mod_id").get()) {
                version = prop("mod_version")
                displayName = prop("mod_name")
                authors = prop("mod_authors")
                description = prop("mod_description")
            }

            dependencies(prop("mod_id").get()) {
                depends("forge") {
                    mandatory = true
                    versionRange = forgeLibs.forge.versionRange
                    ordering = Ordering.NONE
                    side = Side.BOTH
                }
                depends("minecraft") {
                    mandatory = true
                    versionRange = libs.minecraft.versionRange
                    ordering = Ordering.NONE
                    side = Side.BOTH
                }
            }
        }
    }
val generatePackMcmeta =
    tasks.register<PackMcmetaTask>("generatePackMcmeta") {
        description = "Generates resource pack metadata"
        outputFile = generatedResourcesDir.map { it.file("pack.mcmeta") }

        pack {
            description = "${prop("mod_name").get()} resource"
            packFormat = 15
        }
    }
registerMetadataTask(generateModsToml, generatePackMcmeta)
prop("mod_id").get().let {
    mixin {
        add(sourceSets.main.get(), "$it.refmap.json")
        config("$it.mixins.json")
        config("$it.forge.mixins.json")
    }
    tasks.jar {
        manifest.attributes(
            "MixinConfigs" to "$it.mixins.json,$it.forge.mixins.json",
        )
    }
}

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

    implementation(forgeLibs.kotlinforforge)
    annotationProcessor(variantOf(forgeLibs.mixin) { classifier("processor") })

    modRuntimeOnly(forgeLibs.configured)
}