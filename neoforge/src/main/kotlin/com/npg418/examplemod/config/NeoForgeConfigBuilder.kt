package com.npg418.examplemod.config

import com.mojang.logging.LogUtils
import com.npg418.examplemod.config.api.*
import net.neoforged.fml.ModContainer
import net.neoforged.fml.config.ModConfig
import net.neoforged.neoforge.common.ModConfigSpec
import kotlin.reflect.KAnnotatedElement
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.findAnnotation

class NeoForgeConfigBuilder(private val spec: ConfigSpec) : ConfigTreeWalker(LOGGER) {
    companion object {
        private val LOGGER = LogUtils.getLogger()
    }

    private val builder = ModConfigSpec.Builder()

    fun register(container: ModContainer) {
        walkRoot(spec::class).fold(
            onSuccess = {
                container.registerConfig(ModConfig.Type.valueOf(spec.configType.name), builder.build())
            },
            onFailure = {
                LOGGER.error("An error occurred while registering {}.", spec::class.simpleName, it)
            }
        )
    }

    private fun applyCommonMeta(annotated: KAnnotatedElement) {
        annotated.findAnnotation<Comment>()?.let { builder.comment(it.value) }
        annotated.findAnnotation<Translation>()?.let { builder.translation(it.value) }
    }

    override fun onSection(
        name: String,
        sectionClass: KClass<*>,
        visitChildren: () -> Unit
    ) {
        applyCommonMeta(sectionClass)
        builder.push(name)
        try {
            visitChildren()
        } finally {
            builder.pop()
        }
    }

    override fun onProperty(
        name: String,
        property: KProperty1<out Any, *>,
        delegate: ConfigProperty<*>
    ) {
        applyCommonMeta(property)
        property.findAnnotation<WorldRestart>()?.let { builder.worldRestart() }
        property.findAnnotation<GameRestart>()?.let { builder.gameRestart() }

        when (delegate) {
            is NormalConfigProperty<*> -> bind(name, delegate)
            is EnumConfigProperty<*> -> bind(name, delegate)
            is ListConfigProperty<*> -> bind(name, delegate)
            is RangedConfigProperty<*> -> bind(name, delegate)
            is OneOfConfigProperty<*> -> bind(name, delegate)
        }
    }

    private fun <T : Any> ModConfigSpec.ConfigValue<T>.bind(d: ConfigProperty<T>) {
        d.getter = this::get
        d.setter = this::set
    }

    private fun <T : Any> bind(name: String, d: NormalConfigProperty<T>) {
        val configValue = d.validator?.let {
            builder.define(name, d.default, it)
        } ?: builder.define(name, d.default)
        configValue.bind(d)
    }

    private fun <T : Enum<T>> bind(name: String, d: EnumConfigProperty<T>) {
        val configValue = d.validator?.let {
            builder.defineEnum(name, d.default, it)
        } ?: builder.defineEnum(name, d.default)
        configValue.bind(d)
    }

    private fun <E : Any> bind(name: String, d: ListConfigProperty<E>) {
        if (d.allowEmpty) {
            builder.defineListAllowEmpty(name, d.default, d.newElement, d.elementValidator)
        } else {
            builder.defineList(name, d.default, d.newElement, d.elementValidator)
        }.bind(d)
    }

    private fun <T : Comparable<T>> bind(name: String, d: RangedConfigProperty<T>) {
        @Suppress("UNCHECKED_CAST")
        builder.defineInRange(name, d.default, d.range.start, d.range.endInclusive, d.default::class.java as Class<T>)
            .bind(d)
    }

    private fun <T : Any> bind(name: String, d: OneOfConfigProperty<T>) {
        builder.defineInList(name, d.default, d.allowedValues).bind(d)
    }
}