package fr.babeuloula.photoswipe.permission

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import dagger.hilt.android.qualifiers.ApplicationContext
import fr.babeuloula.photoswipe.domain.model.MediaAccess
import fr.babeuloula.photoswipe.platform.PlatformFeatures
import javax.inject.Inject

/**
 * Permissions qui donnent l'accès complet à la galerie (SPEC §4.1) : `READ_MEDIA_IMAGES` et
 * `READ_MEDIA_VIDEO` à partir d'Android 13, `READ_EXTERNAL_STORAGE` avant (PS-181). Sur Android 10,
 * sans corbeille, aussi `WRITE_EXTERNAL_STORAGE`, du même groupe, accordée avec elle : PhotoSwipe
 * supprime lui-même les médias marqués (PS-184, ADR 0042).
 */
@SuppressLint("InlinedApi") // Les permissions d'Android 13 ne sont demandées qu'à partir d'Android 13.
fun mediaPermissions(features: PlatformFeatures = PlatformFeatures.current()): Array<String> =
    when {
        features.granularMediaPermissions -> {
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO)
        }

        features.systemTrash -> {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        else -> {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }

/**
 * Permissions à demander : celles de l'accès complet et, sur Android 14+, celle de l'accès
 * partiel. La déclarer laisse l'utilisateur choisir « Autoriser l'accès limité », puis modifier
 * sa sélection en relançant la même demande (SPEC §4.1).
 */
@SuppressLint("InlinedApi") // L'accès partiel n'est demandé qu'à partir d'Android 14.
fun mediaPermissionRequest(features: PlatformFeatures = PlatformFeatures.current()): Array<String> =
    if (features.partialMediaAccess) {
        mediaPermissions(features) + Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
    } else {
        mediaPermissions(features)
    }

/** Permissions de l'accès complet sur ce téléphone, voir [mediaPermissions]. */
val MEDIA_PERMISSIONS: Array<String> get() = mediaPermissions()

/** Permissions à demander sur ce téléphone, voir [mediaPermissionRequest]. */
val MEDIA_PERMISSION_REQUEST: Array<String> get() = mediaPermissionRequest()

/** Lit l'accès actuel de l'application aux photos et vidéos. */
fun interface MediaAccessReader {
    /** Accès accordé à cet instant ; il peut changer à tout moment depuis les réglages système. */
    fun currentAccess(): MediaAccess
}

/**
 * [MediaAccessReader] sur les permissions Android :
 * - accès complet si les permissions de [mediaPermissions] sont accordées ;
 * - accès partiel si seule la sélection de l'utilisateur est lisible
 *   (`READ_MEDIA_VISUAL_USER_SELECTED`, Android 14+) ;
 * - aucun accès sinon.
 */
class AndroidMediaAccessReader
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
        private val features: PlatformFeatures,
    ) : MediaAccessReader {
        @SuppressLint("InlinedApi") // Lue seulement à partir d'Android 14.
        override fun currentAccess(): MediaAccess =
            when {
                mediaPermissions(features).all { isGranted(it) } -> MediaAccess.FULL

                features.partialMediaAccess &&
                    isGranted(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED) -> MediaAccess.PARTIAL

                else -> MediaAccess.NONE
            }

        private fun isGranted(permission: String): Boolean =
            context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
    }
