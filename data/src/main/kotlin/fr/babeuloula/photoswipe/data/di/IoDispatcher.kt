package fr.babeuloula.photoswipe.data.di

import javax.inject.Qualifier

/** Désigne le `CoroutineDispatcher` des entrées-sorties (disque, MediaStore), injecté au lieu d'un appel statique. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher
