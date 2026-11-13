package fr.babeuloula.photoswipe.ui.deck

import fr.babeuloula.photoswipe.domain.model.MediaId
import fr.babeuloula.photoswipe.domain.model.MediaItem
import fr.babeuloula.photoswipe.domain.model.MediaType
import fr.babeuloula.photoswipe.domain.model.QualityHint
import fr.babeuloula.photoswipe.domain.model.ReviewDecision
import fr.babeuloula.photoswipe.domain.model.SimilarGroup
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId

/**
 * Ce qu'affiche la carte d'un média dans la pile (SPEC §4.3).
 *
 * @property id identifiant MediaStore du média.
 * @property uri URI `content://` du média, lue par Coil et le lecteur vidéo.
 * @property type photo ou vidéo.
 * @property takenOn jour local de la prise de vue, dans le fuseau de l'appareil (ADR 0006) : une
 * photo du 29 février 2024 reste datée du 29 février, même montrée un 28 février.
 * @property yearsAgo années écoulées depuis la prise de vue, comptées depuis le jour cible de la
 * pile et non depuis aujourd'hui : un jour rattrapé garde son « Il y a N ans ».
 * @property sizeBytes poids du fichier en octets.
 * @property duration durée d'une vidéo, `null` pour une photo ou une vidéo de durée inconnue.
 * @property favorite vrai si le média est en favori dans la galerie, ou en favori en attente dans
 * l'application : la carte affiche un cœur (PS-86).
 * @property previousDecision décision d'avant (gardé, marqué pour suppression), sur un jour rouvert
 * pour être retrié (ADR 0026) ; `null` sinon.
 * @property similar place de la photo parmi des photos du jour prises à la suite (PS-140), `null` sinon.
 * @property qualityHint photo peut-être floue ou très sombre (PS-164), `null` sinon.
 * @property raw vrai pour une paire RAW + JPEG (PS-166) : [sizeBytes] compte alors les deux fichiers.
 */
data class MediaCardUiState(
    val id: MediaId,
    val uri: String,
    val type: MediaType,
    val takenOn: LocalDate,
    val yearsAgo: Int,
    val sizeBytes: Long,
    val duration: Duration? = null,
    val favorite: Boolean = false,
    val previousDecision: ReviewDecision? = null,
    val similar: SimilarGroup? = null,
    val qualityHint: QualityHint? = null,
    val raw: Boolean = false,
) {
    companion object {
        /**
         * Carte du souvenir [media] montré dans la pile du jour [day], dans le fuseau [zone] ;
         * [favorite] vaut vrai par défaut si la galerie le dit en favori ; [previousDecision] est
         * la décision d'avant, sur un jour rouvert pour être retrié ; [similar] sa place parmi des
         * photos prises à la suite.
         */
        fun of(
            media: MediaItem,
            day: LocalDate,
            zone: ZoneId,
            favorite: Boolean = media.isFavorite,
            previousDecision: ReviewDecision? = null,
            similar: SimilarGroup? = null,
            qualityHint: QualityHint? = null,
        ): MediaCardUiState {
            val takenOn = media.takenAt.atZone(zone).toLocalDate()
            return MediaCardUiState(
                id = media.id,
                uri = media.uri,
                type = media.type,
                takenOn = takenOn,
                yearsAgo = day.year - takenOn.year,
                sizeBytes = media.totalSizeBytes,
                duration = media.duration,
                favorite = favorite,
                previousDecision = previousDecision,
                similar = similar,
                qualityHint = qualityHint,
                raw = media.rawCompanion != null,
            )
        }
    }
}
