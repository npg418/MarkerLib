@file:Suppress("unused")

package com.npg418.markerlib.config.api

import com.mojang.logging.LogUtils
import org.slf4j.Logger
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

sealed class ConfigProperty<T : Any>(val default: T) {
    companion object {
        val LOGGER: Logger = LogUtils.getLogger()
    }

    private var localValue = default
    open fun isValid(v: T): Boolean = true

    var getter: () -> T = { localValue }
    var setter: (T) -> Unit = {
        if (isValid(it)) localValue = it
        else LOGGER.error("Validation for config value {} failed. The value was not set.", it)
    }

    operator fun getValue(thisRef: Any?, property: KProperty<*>): T = getter()
    operator fun setValue(thisRef: Any?, property: KProperty<*>, newValue: T) = setter(newValue)

    abstract fun <R> accept(name: String, visitor: ConfigPropertyVisitor<R>): R
}

open class NormalConfigProperty<T : Any> @PublishedApi internal constructor(
    default: T, val type: KClass<T>, var validator: ((T) -> Boolean)? = null
) : ConfigProperty<T>(default) {
    override fun isValid(v: T): Boolean = validator?.invoke(v) ?: true
    override fun <R> accept(name: String, visitor: ConfigPropertyVisitor<R>) = visitor.visit(name, this)
}

infix fun <T : Any> NormalConfigProperty<T>.withValidator(validator: (T) -> Boolean) = this.also {
    this.validator = validator
}

class EnumConfigProperty<T : Enum<T>> @PublishedApi internal constructor(default: T, type: KClass<T>) :
    NormalConfigProperty<T>(default, type) {
    override fun <R> accept(name: String, visitor: ConfigPropertyVisitor<R>): R = visitor.visit(name, this)
}

class OneOfConfigProperty<T : Any> internal constructor(
    default: T, type: KClass<T>, val allowedValues: Collection<T>
) : NormalConfigProperty<T>(default, type) {
    override fun isValid(v: T): Boolean = v in allowedValues
    override fun <R> accept(name: String, visitor: ConfigPropertyVisitor<R>): R = visitor.visit(name, this)
}

infix fun <T : Any> NormalConfigProperty<T>.oneOf(allowedValues: Collection<T>) =
    OneOfConfigProperty(default, type, allowedValues)

class RangedConfigProperty<T : Comparable<T>> internal constructor(
    default: T, type: KClass<T>, val range: ClosedRange<T>
) : NormalConfigProperty<T>(default, type) {
    override fun isValid(v: T): Boolean = v in range
    override fun <R> accept(name: String, visitor: ConfigPropertyVisitor<R>): R = visitor.visit(name, this)
}

infix fun <T : Comparable<T>> NormalConfigProperty<T>.ranged(range: ClosedRange<T>) =
    RangedConfigProperty(default, type, range)

class ListConfigProperty<E : Any> @PublishedApi internal constructor(
    default: List<E>,
    val elementType: KClass<E>,
    val newElement: () -> E,
    val elementValidator: (E) -> Boolean,
    val allowEmpty: Boolean
) : ConfigProperty<List<E>>(default) {
    override fun isValid(v: List<E>) = (allowEmpty || v.isNotEmpty()) && v.all(elementValidator)
    override fun <R> accept(name: String, visitor: ConfigPropertyVisitor<R>): R = visitor.visit(name, this)
}
