package fr.babeuloula.photoswipe.permission

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.NotificationManagerCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import fr.babeuloula.photoswipe.platform.PlatformFeatures
import javax.inject.Inject

/**
 * Permission d'afficher des notifications, demandée seulement à l'activation du rappel (SPEC §3.6),
 * et seulement à partir d'Android 13 ([PlatformFeatures.notificationPermission]).
 */
@SuppressLint("InlinedApi")
const val NOTIFICATION_PERMISSION: String = Manifest.permission.POST_NOTIFICATIONS

/** Dit si l'application peut afficher des notifications. */
fun interface NotificationPermissionReader {
    /**
     * Vrai si l'application peut notifier à cet instant : `POST_NOTIFICATIONS` accordée à partir
     * d'Android 13, notifications non coupées dans les réglages avant ; cela peut changer à tout moment.
     */
    fun isGranted(): Boolean
}

/**
 * [NotificationPermissionReader] sur la permission Android à partir d'Android 13, où elle se demande ;
 * avant, sur le réglage des notifications de l'application (PS-181).
 */
class AndroidNotificationPermissionReader
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
        private val features: PlatformFeatures,
    ) : NotificationPermissionReader {
        override fun isGranted(): Boolean =
            if (features.notificationPermission) {
                context.checkSelfPermission(NOTIFICATION_PERMISSION) == PackageManager.PERMISSION_GRANTED
            } else {
                NotificationManagerCompat.from(context).areNotificationsEnabled()
            }
    }
