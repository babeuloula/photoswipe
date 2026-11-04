package fr.babeuloula.photoswipe.domain.service

import fr.babeuloula.photoswipe.domain.model.MediaItem
import java.time.LocalDate
import java.time.ZoneId

/**
 * Retrouve les médias « du même jour » qu'un jour cible (SPEC §3.1).
 *
 * Un média correspond au jour cible `D` si sa date de prise de vue a le même mois et le même
 * jour que `D`, dans une année strictement antérieure à celle de `D`. Les médias de l'année de
 * `D` (et ceux datés après) sont exclus : ils ne sont pas encore des souvenirs.
 *
 * Service pur : il ne lit ni l'horloge ni le fuseau du système. Le fuseau est passé à chaque
 * appel, car la SPEC demande celui de l'appareil **au moment du calcul** : l'appelant le lit sur
 * sa `Clock` injectée (ADR 0006), ce qui suit un changement de fuseau sans recréer le service.
 *
 * Limite connue : la prise de vue est un instant sans fuseau d'origine. Une photo prise dans un
 * autre fuseau que celui de l'appareil peut donc tomber sur le jour voisin.
 */
class SameDayMatcher {
    /** Indique si [media] a été pris le même jour que [target], une année antérieure. */
    fun matches(
        media: MediaItem,
        target: LocalDate,
        zone: ZoneId,
    ): Boolean {
        val takenOn = media.takenAt.atZone(zone).toLocalDate()
        return takenOn.year < target.year &&
            takenOn.month == target.month &&
            takenOn.dayOfMonth == target.dayOfMonth
    }

    /** Garde, dans leur ordre d'origine, les médias de [media] qui correspondent à [target]. */
    fun select(
        media: Iterable<MediaItem>,
        target: LocalDate,
        zone: ZoneId,
    ): List<MediaItem> = media.filter { matches(it, target, zone) }
}
