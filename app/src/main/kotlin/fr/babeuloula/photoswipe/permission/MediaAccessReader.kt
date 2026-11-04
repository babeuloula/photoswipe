package fr.babeuloula.photoswipe.permission

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import fr.babeuloula.photoswipe.domain.model.MediaAccess
import javax.inject.Inject

/** Permissions qui donnent l'accès complet à la galerie (SPEC §4.1). */
val MEDIA_PERMISSIONS: Array<String> =
    arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO)

/**
 * Permissions à demander : celles de l'accès complet et, sur Android 14+, celle de l'accès
 * partiel. La déclarer laisse l'utilisateur choisir « Autoriser l'accès limité », puis modifier
 * sa sélection en relançant la même demande (SPEC §4.1).
 */
val MEDIA_PERMISSION_REQUEST: Array<String> =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        MEDIA_PERMISSIONS + Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
    } else {
        MEDIA_PERMISSIONS
    }

/** Lit l'accès actuel de l'application aux photos et vidéos. */
fun interface MediaAccessReader {
    /** Accès accordé à cet instant ; il peut changer à tout moment depuis les réglages système. */
    fun currentAccess(): MediaAccess
}

/**
 * [MediaAccessReader] sur les permissions Android :
 * - accès complet si `READ_MEDIA_IMAGES` et `READ_MEDIA_VIDEO` sont accordées ;
 * - accès partiel si seule la sélection de l'utilisateur est lisible
 *   (`READ_MEDIA_VISUAL_USER_SELECTED`, Android 14+) ;
 * - aucun accès sinon.
 */
class AndroidMediaAccessReader
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
    ) : MediaAccessReader {
        override fun currentAccess(): MediaAccess =
            when {
                MEDIA_PERMISSIONS.all { isGranted(it) } -> MediaAccess.FULL

                Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
                    isGranted(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED) -> MediaAccess.PARTIAL

                else -> MediaAccess.NONE
            }

        private fun isGranted(permission: String): Boolean =
            context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
    }
