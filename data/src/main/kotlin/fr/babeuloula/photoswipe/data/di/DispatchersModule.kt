package fr.babeuloula.photoswipe.data.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * Fournit les dispatchers de coroutines (SPEC §5.2) : le code les reçoit par injection, jamais par
 * `Dispatchers.IO` en dur, pour que les tests puissent passer un dispatcher de test.
 */
@Module
@InstallIn(SingletonComponent::class)
object DispatchersModule {
    @Provides
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO
}
