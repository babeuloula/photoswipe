package fr.babeuloula.photoswipe.domain.service

import fr.babeuloula.photoswipe.domain.model.MediaItem
import java.time.ZoneId

/**
 * Stratégie d'ordre du paquet d'un jour (SPEC §3.2).
 *
 * L'ordre est isolé ici pour pouvoir en changer sans toucher au reste : `GetDeckForDay` reçoit la
 * stratégie par son constructeur et renvoie le paquet dans l'ordre qu'elle a choisi.
 */
fun interface DeckOrdering {
    /**
     * Renvoie [media] dans l'ordre où ils seront proposés, sans en ajouter ni en retirer.
     *
     * @param zone fuseau de l'appareil au moment du calcul, pour lire l'année ou l'heure locale
     * d'une prise de vue (ADR 0006).
     */
    fun order(
        media: List<MediaItem>,
        zone: ZoneId,
    ): List<MediaItem>
}

/**
 * Ordre par défaut (SPEC §3.2) : année de prise de vue décroissante (les souvenirs les plus récents
 * d'abord), puis prise de vue croissante au sein d'une même année.
 *
 * L'année est lue dans le fuseau [order] reçu. Au sein d'une année, on compare les instants : le
 * 28 février d'une année bissextile passe donc avant son 29 février, montrés ensemble un 28 février
 * d'année non bissextile (SPEC §3.1). À instant égal, l'identifiant départage, pour que l'ordre ne
 * dépende pas de celui, non garanti, de `MediaSource`.
 */
class RecentYearsFirstOrdering : DeckOrdering {
    override fun order(
        media: List<MediaItem>,
        zone: ZoneId,
    ): List<MediaItem> =
        media.sortedWith(
            compareByDescending<MediaItem> { it.takenAt.atZone(zone).year }
                .thenBy { it.takenAt }
                .thenBy { it.id.value },
        )
}
