package fr.babeuloula.photoswipe.domain.port

import fr.babeuloula.photoswipe.domain.model.MediaId
import fr.babeuloula.photoswipe.domain.model.TrashedMedia

/**
 * Lecture de la corbeille d'Android (PS-168), que [MediaSource] ne voit jamais (SPEC §3.1).
 * Implémenté dans `:data` sur MediaStore (`QUERY_ARG_MATCH_TRASHED`).
 */
interface SystemTrash {
    /**
     * Médias encore dans la corbeille parmi [ids]. Un identifiant absent du résultat désigne un média
     * déjà vidé de la corbeille, restauré ailleurs ou illisible : il n'y a plus rien à restaurer.
     */
    suspend fun trashed(ids: Set<MediaId>): List<TrashedMedia>
}
