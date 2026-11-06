package fr.babeuloula.photoswipe.permission

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/** Permission d'afficher des notifications, demandée seulement à l'activation du rappel (SPEC §3.6). */
const val NOTIFICATION_PERMISSION: String = Manifest.permission.POST_NOTIFICATIONS

/** Dit si l'application peut afficher des notifications. */
fun interface NotificationPermissionReader {
    /** Vrai si `POST_NOTIFICATIONS` est accordée à cet instant ; elle peut changer depuis les réglages. */
    fun isGranted(): Boolean
}

/** [NotificationPermissionReader] sur la permission Android (runtime depuis Android 13, notre `minSdk`). */
class AndroidNotificationPermissionReader
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
    ) : NotificationPermissionReader {
        override fun isGranted(): Boolean =
            context.checkSelfPermission(NOTIFICATION_PERMISSION) == PackageManager.PERMISSION_GRANTED
    }
