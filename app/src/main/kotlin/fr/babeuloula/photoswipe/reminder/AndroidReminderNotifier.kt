package fr.babeuloula.photoswipe.reminder

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.text.format.DateFormat
import android.util.Size
import dagger.hilt.android.qualifiers.ApplicationContext
import fr.babeuloula.photoswipe.MainActivity
import fr.babeuloula.photoswipe.R
import fr.babeuloula.photoswipe.domain.model.MediaItem
import fr.babeuloula.photoswipe.domain.port.ReminderNotifier
import fr.babeuloula.photoswipe.permission.NotificationPermissionReader
import java.time.LocalDate
import java.time.format.DateTimeFormatter
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
            years: IntRange?,
            preview: MediaItem?,
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
            val builder =
                Notification
                    .Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setColor(context.getColor(R.color.notification_accent))
                    .setContentTitle(
                        context.resources.getQuantityString(R.plurals.reminder_title, toSortCount, toSortCount),
                    ).setContentText(text(dayToSort, years))
                    .setContentIntent(openHome)
                    .setAutoCancel(true)
            preview?.let { thumbnail(it) }?.let { builder.setLargeIcon(it) }
            notificationManager.notify(NOTIFICATION_ID, builder.build())
        }

        /**
         * Texte du rappel : « Des photos et vidéos prises un 6 novembre, de 2016 à 2025 », ou le
         * texte général si les années ne sont pas connues.
         */
        private fun text(
            day: LocalDate,
            years: IntRange?,
        ): String {
            if (years == null) return context.getString(R.string.reminder_text)
            val locale = context.resources.configuration.locales[0]
            val date =
                day.format(
                    DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, "dMMMM"), locale),
                )
            return if (years.first == years.last) {
                context.getString(R.string.reminder_text_year, date, years.first)
            } else {
                context.getString(R.string.reminder_text_years, date, years.first, years.last)
            }
        }

        /** Vignette d'un souvenir pour la grande icône, ou `null` si Android ne peut pas la produire. */
        private fun thumbnail(media: MediaItem): Bitmap? =
            runCatching {
                context.contentResolver.loadThumbnail(Uri.parse(media.uri), Size(THUMBNAIL_SIZE, THUMBNAIL_SIZE), null)
            }.getOrNull()

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

            /** Taille de la vignette de la grande icône, en pixels. */
            private const val THUMBNAIL_SIZE = 256
        }
    }
