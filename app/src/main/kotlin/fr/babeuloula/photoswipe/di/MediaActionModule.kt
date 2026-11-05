package fr.babeuloula.photoswipe.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import fr.babeuloula.photoswipe.data.media.IntentSenderMediaActionRequest
import fr.babeuloula.photoswipe.ui.common.MediaActionIntents

/**
 * Relie les demandes système de `:data` à l'écran qui les lance (ADR 0018) : seul ce module
 * connaît leur implémentation MediaStore.
 */
@Module
@InstallIn(SingletonComponent::class)
object MediaActionModule {
    @Provides
    fun provideMediaActionIntents(): MediaActionIntents =
        MediaActionIntents { request ->
            requireNotNull(
                request as? IntentSenderMediaActionRequest,
            ) { "not a MediaStore request: $request" }.intentSender
        }
}
