package fr.babeuloula.photoswipe.domain.model

import java.time.LocalTime

/**
 * Réglages du rappel quotidien (SPEC §3.6).
 *
 * @property enabled rappel activé.
 * @property time heure souhaitée, dans le fuseau de l'appareil. Elle reste approximative : Android
 * peut décaler la tâche (Doze).
 */
data class ReminderSettings(
    val enabled: Boolean,
    val time: LocalTime,
) {
    companion object {
        /** Heure par défaut du rappel (SPEC §16, point 5). */
        val DEFAULT_TIME: LocalTime = LocalTime.of(18, 0)

        /**
         * Réglages d'une installation neuve : rappel désactivé à 18:00. La permission
         * `POST_NOTIFICATIONS` n'est demandée qu'au moment où l'utilisateur l'active (SPEC §3.6).
         */
        val DEFAULT: ReminderSettings = ReminderSettings(enabled = false, time = DEFAULT_TIME)
    }
}
