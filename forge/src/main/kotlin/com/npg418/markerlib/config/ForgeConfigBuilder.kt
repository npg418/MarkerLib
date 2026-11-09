package com.npg418.markerlib.config

import com.mojang.logging.LogUtils
import com.npg418.markerlib.config.api.*
import net.minecraftforge.common.ForgeConfigSpec
import net.minecraftforge.fml.ModLoadingContext
import net.minecraftforge.fml.config.ModConfig
import kotlin.reflect.KAnnotatedElement
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.safeCast

class ForgeConfigBuilder(private val spec: ConfigSpec) : ConfigTreeWalker(LOGGER), ConfigPropertyVisitor<Unit> {
    companion object {
        private val LOGGER = LogUtils.getLogger()
    }

    private val builder = ForgeConfigSpec.Builder()

    fun register(context: ModLoadingContext) {
        walkRoot(spec::class).fold(onSuccess = {
            context.registerConfig(ModConfig.Type.valueOf(spec.configType.name), builder.build())
        }, onFailure = {
            LOGGER.error("An error occurred while registering {}.", spec::class.simpleName, it)
        })
    }

    private fun applyMeta(annotated: KAnnotatedElement) {
        annotated.findAnnotation<Comment>()?.let { builder.comment(it.value) }
        annotated.findAnnotation<Translation>()?.let { builder.translation(it.value) }
    }

    override fun onSection(
        name: String, sectionClass: KClass<*>, visitChildren: () -> Unit
    ) {
        applyMeta(sectionClass)
        builder.push(name)
        try {
            visitChildren()
        } finally {
            builder.pop()
        }
    }

    override fun onProperty(name: String, property: KProperty1<out Any, *>, delegate: ConfigProperty<*>) {
        applyMeta(property)
        property.findAnnotation<WorldRestart>()?.let { builder.worldRestart() }
        delegate.accept(name, this)
    }

    private fun <T : Any> ForgeConfigSpec.ConfigValue<T>.bind(d: ConfigProperty<T>) {
        d.getter = this::get
        d.setter = {
            if (d.isValid(it)) set(it)
            else ConfigProperty.LOGGER.error("Validation for config value {} failed. The value was not set.", it)
        }
    }

    private fun <T : Any> validator(type: KClass<T>, d: ConfigProperty<T>): (Any?) -> Boolean =
        { type.safeCast(it)?.let(d::isValid) ?: false }

    override fun <T : Any> visit(name: String, p: NormalConfigProperty<T>) =
        builder.define(name, p.default, validator(p.type, p)).bind(p)

    override fun <E : Enum<E>> visit(name: String, p: EnumConfigProperty<E>) =
        builder.defineEnum(name, p.default).bind(p)

    override fun <T : Comparable<T>> visit(name: String, p: RangedConfigProperty<T>) =
        builder.defineInRange(name, p.default, p.range.start, p.range.endInclusive, p.type.javaObjectType).bind(p)

    override fun <T : Any> visit(name: String, p: OneOfConfigProperty<T>) =
        builder.defineInList(name, p.default, p.allowedValues).bind(p)

    override fun <E : Any> visit(name: String, p: ListConfigProperty<E>) {
        val v: (Any?) -> Boolean = { p.elementType.safeCast(it)?.let(p.elementValidator) ?: false }
        if (p.allowEmpty) {
            builder.defineListAllowEmpty(name, p.default, v)
        } else {
            builder.defineList(name, p.default, v)
        }.bind(p)
    }
}
