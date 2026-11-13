package fr.babeuloula.photoswipe.domain.usecase

import fr.babeuloula.photoswipe.domain.model.MediaAccess
import fr.babeuloula.photoswipe.domain.model.TodayWidget
import kotlinx.coroutines.flow.first

/**
 * État du widget « Ce jour-là » (PS-170), calculé sur le jour d'aujourd'hui de [GetReviewDays] :
 * les souvenirs qui restent à trier et une vignette, sinon « tout est trié » ou « aucun souvenir ».
 * Un jour vidé par ses suppressions compte comme trié (ADR 0034).
 *
 * Sans accès aux médias, la galerie paraît vide : le widget invite à ouvrir l'application plutôt
 * que d'annoncer qu'il n'y a aucun souvenir.
 */
class GetTodayWidget(
    private val getReviewDays: GetReviewDays,
) {
    /** État du widget pour l'accès aux médias [access] accordé à l'application. */
    suspend operator fun invoke(access: MediaAccess): TodayWidget {
        if (access == MediaAccess.NONE) return TodayWidget.NoAccess
        // La fenêtre de revue commence par aujourd'hui.
        val today = getReviewDays().first().first()
        return when {
            today.remainingCount > 0 -> {
                TodayWidget.ToSort(
                    date = today.date,
                    remainingCount = today.remainingCount,
                    years = today.years,
                    preview = today.previews.firstOrNull(),
                )
            }

            today.memoryCount > 0 || today.isEmptied -> {
                TodayWidget.AllSorted(today.date)
            }

            else -> {
                TodayWidget.NoMemories(today.date)
            }
        }
    }
}
