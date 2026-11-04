package fr.babeuloula.photoswipe.ui.deck

import fr.babeuloula.photoswipe.domain.model.MediaId
import fr.babeuloula.photoswipe.domain.model.MediaItem
import fr.babeuloula.photoswipe.domain.model.MediaType
import java.time.LocalDate
import java.time.ZoneId

/**
 * Ce qu'affiche la carte d'un média dans la pile (SPEC §4.3).
 *
 * @property takenOn jour local de la prise de vue, dans le fuseau de l'appareil (ADR 0006) : une
 * photo du 29 février 2024 reste datée du 29 février, même montrée un 28 février.
 * @property yearsAgo années écoulées depuis la prise de vue, comptées depuis le jour cible de la
 * pile et non depuis aujourd'hui : un jour rattrapé garde son « Il y a N ans ».
 * @property sizeBytes poids du fichier en octets.
 */
data class MediaCardUiState(
    val id: MediaId,
    val uri: String,
    val type: MediaType,
    val takenOn: LocalDate,
    val yearsAgo: Int,
    val sizeBytes: Long,
) {
    companion object {
        /** Carte du souvenir [media] montré dans la pile du jour [day], dans le fuseau [zone]. */
        fun of(
            media: MediaItem,
            day: LocalDate,
            zone: ZoneId,
        ): MediaCardUiState {
            val takenOn = media.takenAt.atZone(zone).toLocalDate()
            return MediaCardUiState(
                id = media.id,
                uri = media.uri,
                type = media.type,
                takenOn = takenOn,
                yearsAgo = day.year - takenOn.year,
                sizeBytes = media.sizeBytes,
            )
        }
    }
}
