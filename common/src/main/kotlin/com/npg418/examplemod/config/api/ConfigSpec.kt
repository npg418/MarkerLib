@file:Suppress("Unused", "SameParameterValue")

package com.npg418.examplemod.config.api

abstract class ConfigSpec(val configType: ConfigType) {
    protected fun <T : Any> defining(default: T) = NormalConfigProperty(default)

    protected fun <T : Enum<T>> definingEnum(default: T) = EnumConfigProperty(default)

    protected fun <E : Any> definingList(
        default: List<E>,
        newElement: () -> E,
        elementValidator: (Any?) -> Boolean,
        allowEmpty: Boolean = default.isEmpty()
    ) = ListConfigProperty(default, newElement, elementValidator, allowEmpty)

    protected fun <T : Comparable<T>> definingInRange(
        default: T,
        range: ClosedRange<T>,
    ) = RangedConfigProperty(default, range)

    protected fun <T : Any> definingInList(default: T, allowedValues: List<T>) =
        OneOfConfigProperty(default, allowedValues)
}