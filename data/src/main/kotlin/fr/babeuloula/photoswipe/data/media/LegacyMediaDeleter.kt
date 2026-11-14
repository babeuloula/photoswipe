package fr.babeuloula.photoswipe.data.media

import android.content.ContentResolver
import android.net.Uri
import fr.babeuloula.photoswipe.data.di.IoDispatcher
import fr.babeuloula.photoswipe.domain.model.MediaId
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Supprime pour de bon les médias d'une [DirectDeletionRequest], sans dialogue d'Android : sur
 * Android 10, avec `requestLegacyExternalStorage` et `WRITE_EXTERNAL_STORAGE`, MediaStore laisse
 * l'application supprimer les photos et vidéos de la galerie (PS-184, ADR 0042). Il n'y a pas de
 * corbeille : rien ne se restaure.
 *
 * Un média qu'Android refuse de supprimer (permission retirée, média d'une autre application
 * protégé) ou déjà disparu est laissé de côté : il reste marqué dans PhotoSwipe.
 *
 * @param contentResolver accès aux fournisseurs de contenu d'Android.
 * @param ioDispatcher dispatcher des entrées-sorties, injecté pour les tests.
 */
class LegacyMediaDeleter
    @Inject
    constructor(
        private val contentResolver: ContentResolver,
        @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) {
        /** Supprime les médias de [request] ; renvoie la demande réduite à ceux supprimés, `null` si aucun. */
        suspend fun delete(request: DirectDeletionRequest): DirectDeletionRequest? =
            withContext(ioDispatcher) {
                val deleted = request.mediaIds.zip(request.uris).filter { (_, uri) -> delete(uri) }
                if (deleted.isEmpty()) {
                    null
                } else {
                    DirectDeletionRequest(
                        deleted.mapTo(LinkedHashSet<MediaId>()) { it.first },
                        deleted.map { it.second },
                    )
                }
            }

        private fun delete(uri: Uri): Boolean =
            try {
                contentResolver.delete(uri, null, null) > 0
            } catch (_: SecurityException) {
                false
            }
    }
