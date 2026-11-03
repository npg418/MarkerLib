package com.npg418.gradle.modding

import libs
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.kotlin.jvm)
    idea
}

group = prop("mod_group_id").get()
version = prop("mod_version").get()

val projectName = project.name

base.archivesName = "${prop("mod_id").get()}-$projectName"

java {
    toolchain {
        languageVersion = prop("java_version").map { JavaLanguageVersion.of(it) }
        @Suppress("UnstableApiUsage")
        vendor = JvmVendorSpec.JETBRAINS
    }
    withSourcesJar()
    withJavadocJar()
}

val hotswapJvmArgs = provider {
    val mixinJar = configurations.runtimeClasspath.get()
        .copyRecursive()
        .resolvedConfiguration
        .resolvedArtifacts
        .firstOrNull { it.moduleVersion.id.group == "net.fabricmc" && it.moduleVersion.id.name == "sponge-mixin" }

    listOfNotNull(
        "-XX:+AllowEnhancedClassRedefinition",
        mixinJar?.let { "-javaagent:${it.file.absolutePath}" }
    )
}
extensions.add<Provider<List<String>>>("hotswapJvmArgs", hotswapJvmArgs)

val generatedResourcesDir = layout.buildDirectory.dir("generated/resources")
sourceSets.main {
    resources.srcDir(generatedResourcesDir)
}
extensions.add<Provider<Directory>>("generatedResourcesDir", generatedResourcesDir)

if (project.path != ":common") {
    val commonSrc = project(":common").sourceSets.main.get()
    tasks {
        named<Jar>("jar") {
            from(commonSrc.output)
        }
        named<Jar>("sourcesJar") {
            from(commonSrc.allSource)
        }
    }
    extensions.add<SourceSet>("commonSrc", commonSrc)
}

idea {
    module {
        isDownloadJavadoc = true
        isDownloadSources = true
    }
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions.moduleName.set("${projectName}_$name")
}