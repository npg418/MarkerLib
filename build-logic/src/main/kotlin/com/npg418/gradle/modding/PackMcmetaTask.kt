@file:Suppress("unused")

package com.npg418.gradle.modding

import groovy.json.JsonOutput
import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import javax.inject.Inject

abstract class PackMcmeta @Inject constructor() {
    @get:Input
    abstract val description: Property<String>

    @get:Input
    @get:Optional
    abstract val packFormat: Property<Int>

    @get:Input
    @get:Optional
    abstract val supportedFormatsMin: Property<Int>

    @get:Input
    @get:Optional
    abstract val supportedFormatsMax: Property<Int>

    @get:Input
    @get:Optional
    abstract val minFormat: Property<Int>

    @get:Input
    @get:Optional
    abstract val maxFormat: Property<Int>

    internal fun toMap(): Map<String, Any> {
        val pack = linkedMapOf<String, Any>("description" to description.get())
        packFormat.orNull?.let { pack["pack_format"] = it }
        val sMin = supportedFormatsMin.orNull
        val sMax = supportedFormatsMax.orNull
        if (sMin != null || sMax != null) {
            require(sMin != null && sMax != null) { "Both supportedFormatsMin and supportedFormatsMax are required" }
            pack["supported_formats"] = listOf(sMin, sMax)
        }
        minFormat.orNull?.let { pack["min_format"] = it }
        maxFormat.orNull?.let { pack["max_format"] = it }
        return mapOf("pack" to pack)
    }
}

@CacheableTask
abstract class PackMcmetaTask @Inject constructor(objects: ObjectFactory) : DefaultTask() {
    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @get:Nested
    val pack: PackMcmeta = objects.newInstance(PackMcmeta::class.java)

    fun pack(action: Action<in PackMcmeta>) = action.execute(pack)

    @TaskAction
    fun generate() {
        val json = JsonOutput.prettyPrint(JsonOutput.toJson(pack.toMap()))
        outputFile.get().asFile.apply { parentFile.mkdirs() }.writeText(json + "\n")
    }
}