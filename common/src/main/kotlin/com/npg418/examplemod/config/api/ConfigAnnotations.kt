@file:Suppress("unused")

package com.npg418.examplemod.config.api

@Target(AnnotationTarget.CLASS, AnnotationTarget.PROPERTY)
@MustBeDocumented
annotation class Name(val value: String)

@Target(AnnotationTarget.CLASS, AnnotationTarget.PROPERTY)
@MustBeDocumented
annotation class Comment(val value: String)

@Target(AnnotationTarget.CLASS, AnnotationTarget.PROPERTY)
@MustBeDocumented
annotation class Translation(val value: String)

@Target(AnnotationTarget.PROPERTY)
@MustBeDocumented
annotation class WorldRestart

@Target(AnnotationTarget.PROPERTY)
@MustBeDocumented
annotation class GameRestart