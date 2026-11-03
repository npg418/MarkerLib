package com.npg418.examplemod.config.api

import org.slf4j.Logger
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.jvm.isAccessible

abstract class ConfigTreeWalker(private val logger: Logger) {
    abstract fun onSection(name: String, sectionClass: KClass<*>, visitChildren: () -> Unit)
    abstract fun onProperty(name: String, property: KProperty1<out Any, *>, delegate: ConfigProperty<*>)

    protected fun walkRoot(kClass: KClass<out ConfigSpec>): Result<Unit> {
        val instance = kClass.objectInstance
            ?: return Result.failure(IllegalStateException("Could not register config class ${kClass.qualifiedName} because it is not object class."))
        walk(kClass, instance)
        return Result.success(Unit)
    }

    private fun walk(kClass: KClass<*>, instance: Any) {
        kClass.declaredMemberProperties.forEach { property ->
            val name = property.findAnnotation<Name>()?.value ?: property.name
            property.isAccessible = true
            @Suppress("UNCHECKED_CAST")
            val delegate = (property as KProperty1<Any, *>).getDelegate(instance)
            if (delegate !is ConfigProperty<*>) {
                logger.warn(
                    "Property {} in class {} is not a {}. Skipping.",
                    name,
                    instance::class.qualifiedName,
                    ConfigProperty::class.simpleName
                )
                return@forEach
            }
            onProperty(name, property, delegate)
        }
        kClass.nestedClasses.forEach { nestedClass ->
            val nestedInstance = nestedClass.objectInstance
            if (nestedInstance == null) {
                logger.warn(
                    "Nested class {} inside config class {} is not an object. Skipping.",
                    nestedClass.qualifiedName,
                    kClass.qualifiedName
                )
                return@forEach
            }

            val name = nestedClass.findAnnotation<Name>()?.value ?: nestedClass.simpleName!!
            onSection(name, nestedClass) {
                walk(nestedClass, nestedInstance)
            }
        }
    }
}