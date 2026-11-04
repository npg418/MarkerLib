package com.npg418.examplemod

import java.util.*

interface PlatformHelper {
    val isDevEnvironment: Boolean
    fun isModLoaded(id: String): Boolean
}

object Platform : PlatformHelper by loadService()

inline fun <reified T : Any> loadService(): T =
    ServiceLoader.load(T::class.java, T::class.java.classLoader).firstOrNull()
        ?: error("No implementation of ${T::class.qualifiedName}")