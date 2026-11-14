package fr.babeuloula.photoswipe.platform

import android.os.Build

/**
 * Fonctions d'Android dont dépend PhotoSwipe et qui n'existent pas sur toutes les versions prises
 * en charge (Android 10 et plus, epic 14). Une fonction absente n'est pas proposée, ou son texte
 * s'adapte ; rien n'est reconstruit autrement (PS-181).
 *
 * Fournie par Hilt (`di/PlatformModule`) d'après la version du téléphone, remplaçable dans les tests.
 *
 * @property systemTrash corbeille de la galerie (`MediaStore.createTrashRequest`, Android 11+).
 * @property galleryFavorites favoris de la galerie (`IS_FAVORITE`, `createFavoriteRequest`, Android 11+).
 * @property mediaManagement accès « Gestion des médias » (`MANAGE_MEDIA`, Android 12+).
 * @property exactAlarmPermission autorisation « Alarmes et rappels » à demander pour les alarmes
 * exactes (Android 12+) ; avant, elles sont permises d'office.
 * @property notificationPermission permission de notification à demander (Android 13+) ; avant,
 * les notifications sont permises tant que l'utilisateur ne les coupe pas dans les réglages.
 * @property appLanguageSetting réglage de langue par application (Android 13+).
 * @property granularMediaPermissions permissions `READ_MEDIA_IMAGES` et `READ_MEDIA_VIDEO`
 * (Android 13+) ; avant, la galerie se lit avec `READ_EXTERNAL_STORAGE`.
 * @property partialMediaAccess accès à une sélection de photos et vidéos (Android 14+).
 */
data class PlatformFeatures(
    val systemTrash: Boolean,
    val galleryFavorites: Boolean,
    val mediaManagement: Boolean,
    val exactAlarmPermission: Boolean,
    val notificationPermission: Boolean,
    val appLanguageSetting: Boolean,
    val granularMediaPermissions: Boolean,
    val partialMediaAccess: Boolean,
) {
    companion object {
        /** Fonctions disponibles sur la version d'Android [sdk] (`Build.VERSION.SDK_INT`). */
        fun forSdk(sdk: Int): PlatformFeatures =
            PlatformFeatures(
                systemTrash = sdk >= Build.VERSION_CODES.R,
                galleryFavorites = sdk >= Build.VERSION_CODES.R,
                mediaManagement = sdk >= Build.VERSION_CODES.S,
                exactAlarmPermission = sdk >= Build.VERSION_CODES.S,
                notificationPermission = sdk >= Build.VERSION_CODES.TIRAMISU,
                appLanguageSetting = sdk >= Build.VERSION_CODES.TIRAMISU,
                granularMediaPermissions = sdk >= Build.VERSION_CODES.TIRAMISU,
                partialMediaAccess = sdk >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE,
            )

        /** Fonctions disponibles sur ce téléphone. */
        fun current(): PlatformFeatures = forSdk(Build.VERSION.SDK_INT)
    }
}
