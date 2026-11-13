package fr.babeuloula.photoswipe.data.settings

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import fr.babeuloula.photoswipe.domain.model.DayStatus
import fr.babeuloula.photoswipe.domain.model.Streak
import fr.babeuloula.photoswipe.domain.model.StreakState
import java.time.LocalDate

/**
 * Série de jours (PS-155) dans les préférences : les jours en jours depuis l'epoch, l'état des
 * jours de la fenêtre en une chaîne « jour:ÉTAT » séparée par des virgules. Une entrée illisible
 * (état renommé, chaîne abîmée) est ignorée : le jour sera recompté à la prochaine ouverture.
 */
internal fun Preferences.toStreakState(): StreakState =
    StreakState(
        runStart = this[STREAK_RUN_START]?.let(LocalDate::ofEpochDay),
        settledThrough = this[STREAK_SETTLED_THROUGH]?.let(LocalDate::ofEpochDay),
        recent =
            this[STREAK_RECENT]
                .orEmpty()
                .split(ENTRY_SEPARATOR)
                .mapNotNull { entry ->
                    val day = entry.substringBefore(VALUE_SEPARATOR).toLongOrNull()
                    val status = DayStatus.entries.firstOrNull { it.name == entry.substringAfter(VALUE_SEPARATOR) }
                    if (day != null && status != null) LocalDate.ofEpochDay(day) to status else null
                }.toMap(),
        streak = Streak(current = this[STREAK_CURRENT] ?: 0, best = this[STREAK_BEST] ?: 0),
    )

/** Écrit [state] à la place de la série enregistrée. */
internal fun MutablePreferences.putStreakState(state: StreakState) {
    state.runStart?.let { this[STREAK_RUN_START] = it.toEpochDay() } ?: remove(STREAK_RUN_START)
    state.settledThrough?.let { this[STREAK_SETTLED_THROUGH] = it.toEpochDay() } ?: remove(STREAK_SETTLED_THROUGH)
    this[STREAK_RECENT] =
        state.recent.entries.joinToString(ENTRY_SEPARATOR) { (day, status) ->
            "${day.toEpochDay()}$VALUE_SEPARATOR${status.name}"
        }
    this[STREAK_CURRENT] = state.streak.current
    this[STREAK_BEST] = state.streak.best
}

private val STREAK_RUN_START = longPreferencesKey("streak_run_start")
private val STREAK_SETTLED_THROUGH = longPreferencesKey("streak_settled_through")
private val STREAK_RECENT = stringPreferencesKey("streak_recent")
private val STREAK_CURRENT = intPreferencesKey("streak_current")
private val STREAK_BEST = intPreferencesKey("streak_best")
private const val ENTRY_SEPARATOR = ","
private const val VALUE_SEPARATOR = ":"
