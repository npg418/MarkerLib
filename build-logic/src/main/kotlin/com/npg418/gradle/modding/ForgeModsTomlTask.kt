@file:Suppress("unused")

package com.npg418.gradle.modding

import com.electronwill.nightconfig.core.Config
import com.electronwill.nightconfig.toml.TomlFormat
import com.electronwill.nightconfig.toml.TomlWriter
import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.Named
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.gradle.kotlin.dsl.newInstance
import javax.inject.Inject

abstract class ForgeModsToml @Inject constructor(private val objects: ObjectFactory) {
    @get:Input
    abstract val modLoader: Property<String>

    @get:Input
    abstract val loaderVersion: Property<String>

    @get:Input
    abstract val license: Property<String>

    @get:Input
    @get:Optional
    abstract val showAsResourcePack: Property<Boolean>

    @get:Input
    @get:Optional
    abstract val showAsDataPack: Property<Boolean>

    @get:Input
    @get:Optional
    abstract val services: ListProperty<String>

    @get:Input
    @get:Optional
    abstract val properties: MapProperty<String, String>

    @get:Input
    @get:Optional
    abstract val issueTrackerURL: Property<String>

    @get:Nested
    abstract val mods: ListProperty<ForgeModSpec>

    fun mod(modId: String, action: Action<in ForgeModSpec>? = null) = create(mods) {
        this.modId.set(modId)
        action?.execute(this)
    }

    @get:Nested
    abstract val features: NamedDomainObjectContainer<ForgeModFeaturesSpec>

    fun features(modId: String, action: Action<in ForgeModFeaturesSpec>) = features.register(modId, action)

    @get:Nested
    abstract val modproperties: NamedDomainObjectContainer<ForgeModPropertiesSpec>

    fun modproperties(modId: String, vararg properties: Pair<String, String>) = modproperties(modId, mapOf(*properties))

    fun modproperties(modId: String, properties: Map<String, String>) = modproperties.register(modId) {
        values.putAll(properties)
    }

    @get:Nested
    abstract val accessTransformers: ListProperty<ForgeAccessTransformerSpec>

    fun accessTransformer(file: String) = create(accessTransformers) { this.file.set(file) }

    @get:Nested
    abstract val mixins: ListProperty<ForgeMixinSpec>

    fun mixin(config: String) = create(mixins) { this.config.set(config) }

    private inline fun <reified T : Any> create(listProperty: ListProperty<T>, action: Action<T>) {
        val item = objects.newInstance<T>()
        action.execute(item)
        listProperty.add(item)
    }

    @get:Nested
    abstract val dependencies: NamedDomainObjectContainer<ForgeModDependenciesSpec>

    fun dependencies(modId: String, action: Action<ForgeModDependenciesSpec>) = dependencies.register(modId, action)

    internal fun toConfig() = config {
        put("modLoader", modLoader)
        put("loaderVersion", loaderVersion)
        put("license", license)
        put("showAsResourcePack", showAsResourcePack)
        put("showAsDataPack", showAsDataPack)
        put("services", services)
        put("properties", properties.map { props ->
            if (props.isNotEmpty()) {
                config {
                    props.forEach { (k, v) -> set(listOf(k), v) }
                }
            } else null
        })
        put("issueTrackerURL", issueTrackerURL)
        put("mods", mods.map { modSpecs -> modSpecs.map { it.toConfig() } })
        putNamed("features", features) { it.toConfig() }
        putNamed("modproperties", modproperties) { it.toConfig() }
        put("accessTransformers", accessTransformers.map { specs -> specs.map { it.toConfig() } })
        put("mixins", mixins.map { mixinSpecs -> mixinSpecs.map { it.toConfig() } })
        putNamed("dependencies", dependencies) { it.toConfig() }
    }
}

abstract class ForgeModSpec @Inject constructor() {
    @get:Input
    abstract val modId: Property<String>

    @get:Input
    @get:Optional
    abstract val namespace: Property<String>

    @get:Input
    @get:Optional
    abstract val version: Property<String>

    @get:Input
    @get:Optional
    abstract val displayName: Property<String>

    @get:Input
    @get:Optional
    abstract val description: Property<String>

    @get:Input
    @get:Optional
    abstract val logoFile: Property<String>

    @get:Input
    @get:Optional
    abstract val logoBlur: Property<Boolean>

    @get:Input
    @get:Optional
    abstract val updateJSONURL: Property<String>

    @get:Input
    @get:Optional
    abstract val credits: Property<String>

    @get:Input
    @get:Optional
    abstract val authors: Property<String>

    @get:Input
    @get:Optional
    abstract val displayURL: Property<String>

    @get:Input
    @get:Optional
    abstract val enumExtensions: Property<String>

    internal fun toConfig() = config {
        put("modId", modId)
        put("namespace", namespace)
        put("version", version)
        put("displayName", displayName)
        put("description", description)
        put("logoFile", logoFile)
        put("logoBlur", logoBlur)
        @Suppress("SpellCheckingInspection")
        put("updateJSONURL", updateJSONURL)
        put("credits", credits)
        put("authors", authors)
        put("displayURL", displayURL)
        put("enumExtensions", enumExtensions)
    }
}

abstract class ForgeModFeaturesSpec @Inject constructor(private val modId: String) : Named {
    @Internal
    override fun getName(): String = modId

    @get:Input
    @get:Optional
    abstract val javaVersion: Property<String>

    @get:Input
    @get:Optional
    abstract val openGLVersion: Property<String>

    internal fun toConfig() = config {
        put("javaVersion", javaVersion)
        put("openGLVersion", openGLVersion)
    }
}

abstract class ForgeModPropertiesSpec @Inject constructor(private val modId: String) : Named {
    @Internal
    override fun getName(): String = modId

    @get:Input
    abstract val values: MapProperty<String, String>

    internal fun toConfig() = config {
        values.get().forEach { (k, v) -> set(listOf(k), v) }
    }
}

abstract class ForgeAccessTransformerSpec @Inject constructor() {
    @get:Input
    @get:Optional
    abstract val file: Property<String>

    internal fun toConfig() = config {
        put("file", file)
    }
}

abstract class ForgeMixinSpec @Inject constructor() {
    @get:Input
    @get:Optional
    abstract val config: Property<String>

    internal fun toConfig() = config {
        put("config", config)
    }
}

enum class Ordering { NONE, BEFORE, AFTER }
enum class Side { BOTH, CLIENT, SERVER }
abstract class ForgeModDependencySpec @Inject constructor() {

    @get:Input
    abstract val modId: Property<String>

    @get:Input
    @get:Optional
    abstract val mandatory: Property<Boolean>

    @get:Input
    @get:Optional
    abstract val versionRange: Property<String>

    @get:Input
    @get:Optional
    abstract val ordering: Property<Ordering>

    @get:Input
    @get:Optional
    abstract val side: Property<Side>

    internal fun toConfig() = config {
        put("modId", modId)
        put("mandatory", mandatory)
        put("versionRange", versionRange)
        put("ordering", ordering.map { it.name })
        put("side", side.map { it.name })
    }
}

abstract class ForgeModDependenciesSpec @Inject constructor(
    private val modId: String,
    private val objects: ObjectFactory,
) : Named {
    @Internal
    override fun getName(): String = modId

    @get:Nested
    abstract val specs: ListProperty<ForgeModDependencySpec>

    fun depends(modId: String, action: Action<in ForgeModDependencySpec>? = null) {
        specs.add(objects.newInstance<ForgeModDependencySpec>().also {
            it.modId.set(modId)
            action?.execute(it)
        })
    }

    internal fun toConfig() = specs.get().map { it.toConfig() }
}

private fun config(block: Config.() -> Unit): Config = TomlFormat.newConfig().apply(block)
private fun <T : Any> Config.put(key: String, provider: Provider<T>) {
    val value = provider.orNull ?: return
    if (value is Collection<*> && value.isEmpty()) return
    if (value is Map<*, *> && value.isEmpty()) return
    set<T>(key, value)
}

private fun <T : Named> Config.putNamed(key: String, items: Iterable<T>, convert: (T) -> Any) {
    if (!items.iterator().hasNext()) return
    set<Any>(key, config {
        items.forEach { set(listOf(it.name), convert(it)) }
    })
}

@CacheableTask
abstract class ForgeModsTomlTask @Inject constructor(objects: ObjectFactory) : DefaultTask() {
    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @get:Nested
    val toml: ForgeModsToml = objects.newInstance(ForgeModsToml::class.java)

    fun toml(action: Action<in ForgeModsToml>) = action.execute(toml)

    @TaskAction
    fun generate() {
        outputFile.get().asFile.bufferedWriter().use {
            TomlWriter().write(toml.toConfig(), it)
        }
    }
}