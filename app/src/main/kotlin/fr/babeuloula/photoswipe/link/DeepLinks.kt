package fr.babeuloula.photoswipe.link

import java.net.URI
import java.net.URISyntaxException
import java.time.LocalDate
import java.time.format.DateTimeParseException

/**
 * Liens profonds de PhotoSwipe (ADR 0038), à ouvrir depuis une routine d'automatisation, un
 * raccourci ou la tuile des réglages rapides :
 * - `photoswipe://today` : la pile du jour ;
 * - `photoswipe://summary` : le résumé ;
 * - `photoswipe://day/2026-07-15` : la vue d'ensemble d'une date (format ISO `AAAA-MM-JJ`).
 *
 * Un lien ne fait qu'ouvrir un écran : il ne change aucune décision et ne renvoie aucune donnée.
 */
object DeepLinks {
    /** Schéma des liens, déclaré par le filtre d'intent de `MainActivity`. */
    const val SCHEME = "photoswipe"

    /** Lien vers la pile du jour. */
    const val TODAY = "$SCHEME://today"

    /** Lien vers le résumé. */
    const val SUMMARY = "$SCHEME://summary"

    private const val TODAY_HOST = "today"
    private const val SUMMARY_HOST = "summary"
    private const val DAY_HOST = "day"

    /** Lien vers la vue d'ensemble du jour [day]. */
    fun day(day: LocalDate): String = "$SCHEME://$DAY_HOST/$day"

    /** Écran demandé par [link] ; l'accueil pour un lien invalide, d'un autre schéma ou incomplet. */
    fun parse(link: String): LaunchRequest =
        uriOf(link)
            ?.takeIf { it.scheme.equals(SCHEME, ignoreCase = true) }
            ?.let(::requestOf)
            ?: LaunchRequest.Home

    private fun requestOf(uri: URI): LaunchRequest? {
        val segments =
            uri.path
                .orEmpty()
                .split('/')
                .filter { it.isNotEmpty() }
        return when (uri.host?.lowercase()) {
            TODAY_HOST -> LaunchRequest.TodayDeck.takeIf { segments.isEmpty() }
            SUMMARY_HOST -> LaunchRequest.Summary.takeIf { segments.isEmpty() }
            DAY_HOST -> segments.singleOrNull()?.let(::dateOf)?.let(LaunchRequest::DayOverview)
            else -> null
        }
    }

    private fun uriOf(link: String): URI? =
        try {
            URI(link.trim())
        } catch (_: URISyntaxException) {
            null
        }

    private fun dateOf(text: String): LocalDate? =
        try {
            LocalDate.parse(text)
        } catch (_: DateTimeParseException) {
            null
        }
}
