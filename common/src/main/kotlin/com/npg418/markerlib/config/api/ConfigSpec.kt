@file:Suppress("Unused", "SameParameterValue")

package com.npg418.markerlib.config.api

abstract class ConfigSpec(val configType: ConfigType) {
    protected inline fun <reified T : Any> entry(default: T) = NormalConfigProperty(default, T::class)

    protected inline fun <reified T : Enum<T>> entry(default: T) = EnumConfigProperty(default, T::class)

    protected inline fun <reified E : Any> entry(
        default: List<E>,
        noinline newElement: () -> E,
        noinline elementValidator: (E) -> Boolean = { true },
        allowEmpty: Boolean = default.isEmpty()
    ) = ListConfigProperty(default, E::class, newElement, elementValidator, allowEmpty)
}