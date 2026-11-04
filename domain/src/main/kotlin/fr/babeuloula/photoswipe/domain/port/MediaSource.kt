package fr.babeuloula.photoswipe.domain.port

import fr.babeuloula.photoswipe.domain.model.MediaId
import fr.babeuloula.photoswipe.domain.model.MediaItem
import java.time.Instant

/**
 * Accès en lecture aux photos et vidéos de la galerie (SPEC §5.2). Implémenté dans `:data` sur
 * MediaStore.
 *
 * Les médias déjà dans la corbeille système ne sont jamais renvoyés (SPEC §3.1). La date de prise
 * de vue de chaque [MediaItem] suit la règle `DATE_TAKEN`, à défaut `DATE_ADDED`.
 */
interface MediaSource {
    /**
     * Médias pris dans l'une des plages [ranges], chacune ouverte à droite (`début..<fin`).
     *
     * Le domaine calcule ces plages (un jour d'une année donnée, dans le fuseau de l'appareil) :
     * l'implémentation les traduit en bornes epoch, sans fonction SQL de date (SPEC §5.3).
     * L'ordre du résultat n'est pas garanti ; un média n'y figure qu'une fois.
     */
    suspend fun mediaTakenIn(ranges: List<OpenEndRange<Instant>>): List<MediaItem>

    /**
     * Médias encore présents parmi [ids]. Un identifiant absent du résultat désigne un média
     * supprimé ou mis à la corbeille : c'est ainsi qu'on repère les entrées orphelines (SPEC §6).
     */
    suspend fun mediaByIds(ids: Set<MediaId>): List<MediaItem>
}
