import com.npg418.gradle.modding.prop
import com.npg418.gradle.modding.registerMetadataTask
import com.npg418.gradle.modding.versionRange
import net.fabricmc.loom.task.FabricModJsonV1Task

plugins {
    alias(conventions.plugins.common.convention)
    alias(libs.plugins.fabric.loom)
}

fabricApi {
    configureDataGeneration {
        client = true
    }
}

loom {
    log4jConfigs.from("log4j-dev.xml")

    runs {
        configureEach {
            jvmArguments.addAll(hotswapJvmArgs)

            displayName = "Fabric: Run $name"
            appendProjectPathToDisplayName = false

            generateRunConfig = true
        }
    }

    mods {
        register(prop("mod_id").get()) {
            sourceSet(sourceSets.main.get())
            sourceSet(commonSrc)
        }
    }
}

fun mavenToFabric(range: String): List<String> {
    if (range.replace(" ", "") in setOf("[0,)", "(,)", "*")) return listOf("*")
    val parts = Regex("""[\[(][^\[\]()]*[])]""").findAll(range).map { it.value }.toList()
    if (parts.isEmpty()) return listOf(">=$range")

    return parts.map { s ->
        val body = s.substring(1, s.length - 1)
        if (',' !in body) return@map "=${body.trim()}"
        val (min, max) = body.split(',', limit = 2).map { it.trim() }
        listOfNotNull(
            min.takeIf { it.isNotEmpty() }?.let { (if (s.first() == '[') ">=" else ">") + it },
            max.takeIf { it.isNotEmpty() }?.let { (if (s.last() == ']') "<=" else "<") + it },
        ).joinToString(" ")
    }
}

val generateModJson = tasks.register<FabricModJsonV1Task>("generateModJson") {
    description = "Generates fabric mod metadata"
    outputFile = generatedResourcesDir.map { it.file("fabric.mod.json") }

    json {
        modId = prop("mod_id")
        version = prop("mod_version")
        name = prop("mod_name")
        description = prop("mod_description")
        prop("mod_authors").get().split(",").forEach { name ->
            author(name)
        }
        licenses.add(prop("mod_license"))
        entrypoint("main") {
            adapter = "kotlin"
            value = "com.npg418.markerlib.FabricMain"
        }
        entrypoint("fabric-datagen") {
            adapter = "kotlin"
            value = "com.npg418.markerlib.datagen.MarkerLibDataGenerator"
        }
        modId.get().let {
            mixin("$it.mixins.json")
            mixin("$it.fabric.mixins.json")
        }
        depends("fabricloader", fabricLibs.fabric.loader.versionRange.map(::mavenToFabric).get())
        depends("minecraft", libs.minecraft.versionRange.map(::mavenToFabric).get())
        depends("java", java.toolchain.languageVersion.map { ">=$it" }.get())
        depends("fabric-api", fabricLibs.fabric.api.versionRange.map(::mavenToFabric).get())
        depends("fabric-language-kotlin", fabricLibs.fabric.kotlin.versionRange.map(::mavenToFabric).get())
    }
}
registerMetadataTask(generateModJson)

repositories {
    maven("https://maven.parchmentmc.org") {
        name = "ParchmentMC"
    }
    maven("https://maven.terraformersmc.com") {
        name = "Terraformers"
    }
}

dependencies {
    implementation(projects.common)

    minecraft(libs.minecraft) {
        version { require(preferredVersion) }
    }
    @Suppress("UnstableApiUsage")
    mappings(loom.layered {
        officialMojangMappings()
        parchment("org.parchmentmc.data:parchment-${libs.versions.parchment.minecraft.get()}:${libs.versions.parchment.mapping.get()}@zip")
    })
    modImplementation(fabricLibs.fabric.loader)
    modImplementation(fabricLibs.fabric.api)
    modImplementation(fabricLibs.fabric.kotlin)

    modRuntimeOnly(fabricLibs.modmenu)
}
