package fr.babeuloula.photoswipe.reminder

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.format.DateFormat
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dagger.hilt.android.AndroidEntryPoint
import fr.babeuloula.photoswipe.R
import fr.babeuloula.photoswipe.domain.model.ThemeMode
import fr.babeuloula.photoswipe.domain.port.SettingsRepository
import fr.babeuloula.photoswipe.domain.usecase.SnoozeReminderToday
import fr.babeuloula.photoswipe.ui.snooze.SnoozeReminderDialog
import fr.babeuloula.photoswipe.ui.theme.PhotoSwipeTheme
import fr.babeuloula.photoswipe.ui.theme.isDark
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.Date
import javax.inject.Inject

/**
 * Reprogrammation du rappel d'aujourd'hui (SPEC §3.6), ouverte par l'action « Reprogrammer » de la
 * notification : une activité translucide qui n'affiche que le sélecteur d'heure, par-dessus ce
 * qui est à l'écran, sans ouvrir l'accueil. Une heure validée retire la notification et planifie
 * un rappel unique ; une annulation ne change rien.
 */
@AndroidEntryPoint
class SnoozeReminderActivity : ComponentActivity() {
    /** Planifie le rappel unique d'aujourd'hui. */
    @Inject
    lateinit var snoozeReminderToday: SnoozeReminderToday

    /** Réglages, lus pour le thème. */
    @Inject
    lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Lecture courte d'un petit fichier local : le dialogue s'affiche d'emblée dans le bon thème.
        val themeMode = runBlocking { settingsRepository.observeThemeMode().catch { emit(ThemeMode.DEFAULT) }.first() }
        val initialTime = snoozeReminderToday.suggestedTime()
        setContent {
            var timePassed by rememberSaveable { mutableStateOf(false) }
            PhotoSwipeTheme(darkTheme = themeMode.isDark(isSystemInDarkTheme())) {
                SnoozeReminderDialog(
                    initialTime = initialTime,
                    is24Hour = DateFormat.is24HourFormat(this),
                    timePassed = timePassed,
                    onConfirm = { time ->
                        when (snoozeReminderToday(time)) {
                            SnoozeReminderToday.Result.SCHEDULED -> confirm(time)
                            SnoozeReminderToday.Result.TIME_PASSED -> timePassed = true
                        }
                    },
                    onDismiss = ::finish,
                )
            }
        }
    }

    /** Retire la notification, confirme l'heure et ferme. */
    private fun confirm(time: LocalTime) {
        getSystemService(NotificationManager::class.java).cancel(AndroidReminderNotifier.NOTIFICATION_ID)
        val instant =
            LocalDate
                .now()
                .atTime(time)
                .atZone(ZoneId.systemDefault())
                .toInstant()
        val label = DateFormat.getTimeFormat(this).format(Date.from(instant))
        Toast.makeText(this, getString(R.string.reminder_snooze_done, label), Toast.LENGTH_SHORT).show()
        finish()
    }

    companion object {
        /** Intent de l'action « Reprogrammer » de la notification. */
        fun intent(context: Context): Intent =
            Intent(context, SnoozeReminderActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
    }
}
