@file:Suppress("unused")

package com.npg418.gradle.modding

import libs
import org.gradle.api.Project
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.invoke
import org.gradle.plugins.ide.idea.model.IdeaModel
import org.jetbrains.gradle.ext.settings
import org.jetbrains.gradle.ext.taskTriggers

fun Project.prop(propertyName: String) = providers.gradleProperty(propertyName)

val Provider<MinimalExternalModuleDependency>.version
    get() = map { it.versionConstraint.preferredVersion }
val Provider<MinimalExternalModuleDependency>.versionRange
    get() = map { it.versionConstraint.strictVersion }

fun Project.registerMetadataTask(task: TaskProvider<*>) {
    tasks {
        named("processResources") { dependsOn(task) }
        named("sourcesJar") { dependsOn(task) }
    }

    rootProject.plugins.withId(libs.plugins.idea.ext.get().pluginId) {
        rootProject.extensions.configure<IdeaModel> {
            project { settings { taskTriggers { afterSync(task) } } }
        }
    }
}