package fr.babeuloula.photoswipe.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import fr.babeuloula.photoswipe.data.media.DirectDeletionRequest
import fr.babeuloula.photoswipe.data.media.IntentSenderMediaActionRequest
import fr.babeuloula.photoswipe.data.media.LegacyMediaDeleter
import fr.babeuloula.photoswipe.domain.port.MediaActionRequest
import fr.babeuloula.photoswipe.ui.common.DirectDeletion
import fr.babeuloula.photoswipe.ui.common.MediaActionIntents

/**
 * Relie les demandes système de `:data` à l'écran qui les lance (ADR 0018), et les suppressions
 * directes d'Android 10 à celui qui les applique (PS-184) : seul ce module connaît leur
 * implémentation MediaStore.
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

    /** Suppressions définitives d'Android 10, appliquées sans dialogue par `LegacyMediaDeleter` (PS-184). */
    @Provides
    fun provideDirectDeletion(deleter: LegacyMediaDeleter): DirectDeletion =
        object : DirectDeletion {
            override fun handles(request: MediaActionRequest): Boolean = request is DirectDeletionRequest

            override suspend fun delete(request: MediaActionRequest): MediaActionRequest? =
                deleter.delete(
                    requireNotNull(request as? DirectDeletionRequest) { "not a direct deletion: $request" },
                )
        }
}
