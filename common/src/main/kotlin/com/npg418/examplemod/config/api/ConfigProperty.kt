@file:Suppress("unused")

package com.npg418.examplemod.config.api

import com.mojang.logging.LogUtils
import org.slf4j.Logger
import kotlin.reflect.KProperty

sealed class ConfigProperty<T : Any>(val default: T) {
    companion object {
        val LOGGER: Logger = LogUtils.getLogger()
    }

    protected var localValue = default
    var getter: () -> T = { localValue }
    var setter: (T) -> Unit = { localValue = it }

    operator fun getValue(thisRef: Any?, property: KProperty<*>): T = getter()

    operator fun setValue(thisRef: Any?, property: KProperty<*>, newValue: T) {
        setter(newValue)
    }
}

sealed class ValidatedConfigProperty<T : Any>(default: T) : ConfigProperty<T>(default) {
    var validator: ((Any?) -> Boolean)? = null

    init {
        setter = {
            if (validator?.invoke(it) ?: true) {
                localValue = it
            } else {
                LOGGER.error("Validation for config value {} failed. The value was not set.", it)
            }
        }
    }
}

infix fun <T: Any> ValidatedConfigProperty<T>.validator(v: (Any?) -> Boolean) = apply {
    validator = v
}

class NormalConfigProperty<T : Any> internal constructor(default: T) : ValidatedConfigProperty<T>(default)

class EnumConfigProperty<T : Enum<T>> internal constructor(default: T) : ValidatedConfigProperty<T>(default)

class ListConfigProperty<E : Any> internal constructor(
    default: List<E>,
    val newElement: () -> E,
    val elementValidator: (Any?) -> Boolean,
    val allowEmpty: Boolean
) : ConfigProperty<List<E>>(default)

class RangedConfigProperty<T : Comparable<T>> internal constructor(default: T, val range: ClosedRange<T>) :
    ConfigProperty<T>(default)

class OneOfConfigProperty<T : Any> internal constructor(default: T, val allowedValues: List<T>) :
    ConfigProperty<T>(default)