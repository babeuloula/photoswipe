package fr.babeuloula.photoswipe.domain.model

import java.time.LocalDate

/**
 * Jour cible de la fenêtre de revue (aujourd'hui ou l'un des 6 jours précédents, SPEC §3.1).
 *
 * @property date jour cible, dans le fuseau de l'appareil.
 * @property remainingCount nombre de médias qui restent à trier pour ce jour.
 * @property memoryCount nombre de souvenirs de ce jour, triés ou non : 0 signifie qu'aucun média
 * n'a été trouvé, et non que tout est trié. Par défaut, égal à [remainingCount] (rien de trié).
 * @property previews souvenirs du jour à montrer en vignette dans une bande de [PREVIEW_SLOTS] cases :
 * tous s'ils y tiennent, sinon un de moins, la dernière case donnant le nombre des autres (« +20 »).
 * Ceux qui restent à trier d'abord ; dans chaque groupe, une vignette par année de la plus récente à
 * la plus ancienne, puis une deuxième par année, et ainsi de suite.
 * @property years années de prise de vue des souvenirs du jour, de la plus ancienne à la plus
 * récente (« de 2016 à 2025 ») ; `null` s'il n'y en a aucun.
 */
data class ReviewDay(
    val date: LocalDate,
    val remainingCount: Int,
    val memoryCount: Int = remainingCount,
    val previews: List<MediaItem> = emptyList(),
    val years: IntRange? = null,
) {
    init {
        require(remainingCount >= 0) { "remainingCount must not be negative: $remainingCount" }
        require(memoryCount >= remainingCount) {
            "memoryCount ($memoryCount) must be at least remainingCount ($remainingCount)"
        }
    }

    /** Année de revue à laquelle les décisions de ce jour sont rattachées (SPEC §3.3). */
    val reviewYear: Int get() = date.year

    /** Souvenirs du jour qui ne sont pas en vignette (« +20 »). */
    val hiddenCount: Int get() = (memoryCount - previews.size).coerceAtLeast(0)

    companion object {
        /** Cases de la bande de vignettes : quatre photos, ou trois et le nombre des autres. */
        const val PREVIEW_SLOTS = 4
    }
}
