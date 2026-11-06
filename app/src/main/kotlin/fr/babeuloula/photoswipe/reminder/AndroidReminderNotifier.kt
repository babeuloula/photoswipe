package fr.babeuloula.photoswipe.reminder

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import fr.babeuloula.photoswipe.MainActivity
import fr.babeuloula.photoswipe.R
import fr.babeuloula.photoswipe.domain.port.ReminderNotifier
import fr.babeuloula.photoswipe.permission.NotificationPermissionReader
import java.time.LocalDate
import javax.inject.Inject

/**
 * [ReminderNotifier] sur les notifications Android (SPEC §3.6) : un canal dédié « Rappel
 * quotidien », un titre au pluriel (« 3 souvenirs à trier »), et un appui qui ouvre directement
 * la pile du jour à trier, au-dessus de l'accueil.
 * Sans permission de notification, rien n'est affiché.
 */
class AndroidReminderNotifier
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
        private val permission: NotificationPermissionReader,
    ) : ReminderNotifier {
        private val notificationManager: NotificationManager = context.getSystemService(NotificationManager::class.java)

        override fun notify(
            toSortCount: Int,
            dayToSort: LocalDate,
        ) {
            if (!permission.isGranted()) return
            createChannel()
            val openHome =
                PendingIntent.getActivity(
                    context,
                    0,
                    MainActivity.openDeckIntent(context, dayToSort),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                )
            val notification =
                Notification
                    .Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle(
                        context.resources.getQuantityString(R.plurals.reminder_title, toSortCount, toSortCount),
                    ).setContentText(context.getString(R.string.reminder_text))
                    .setContentIntent(openHome)
                    .setAutoCancel(true)
                    .build()
            notificationManager.notify(NOTIFICATION_ID, notification)
        }

        /** Crée le canal du rappel, ou met à jour son nom traduit ; sans effet sur les choix de l'utilisateur. */
        private fun createChannel() {
            val channel =
                NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.reminder_channel_name),
                    NotificationManager.IMPORTANCE_DEFAULT,
                ).apply { description = context.getString(R.string.reminder_channel_description) }
            notificationManager.createNotificationChannel(channel)
        }

        companion object {
            /** Canal des notifications du rappel quotidien. */
            const val CHANNEL_ID: String = "daily_reminder"

            /** Une seule notification de rappel à la fois : la suivante remplace la précédente. */
            const val NOTIFICATION_ID: Int = 1
        }
    }
