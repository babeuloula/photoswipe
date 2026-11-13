package fr.babeuloula.photoswipe.domain.model

import java.time.Duration
import java.time.Instant
import java.time.LocalDate

/**
 * Suppression confirmée : un média envoyé à la corbeille système depuis PhotoSwipe (ADR 0034).
 *
 * Une fois à la corbeille, le média n'est plus lisible dans la galerie et ses décisions sont purgées
 * avec les orphelins : cette trace est tout ce qui reste pour dire ce qui a été supprimé et combien
 * de place cela libérera. Elle est gardée le temps que le média reste dans la corbeille
 * ([TRASH_RETENTION]), pour la page « Corbeille » (PS-168).
 *
 * @property mediaId média supprimé.
 * @property day jour de revue de la fenêtre dont la pile a marqué le média, `null` si le marquage
 * venait d'un jour déjà sorti de la fenêtre.
 * @property sizeBytes taille du média enregistrée au moment du marquage.
 * @property confirmedAt instant de la confirmation.
 */
data class ConfirmedDeletion(
    val mediaId: MediaId,
    val day: LocalDate?,
    val sizeBytes: Long,
    val confirmedAt: Instant,
) {
    init {
        require(sizeBytes >= 0) { "sizeBytes must not be negative: $sizeBytes" }
    }

    /** Instant où Android videra le média de sa corbeille, faute de date lue dans MediaStore. */
    val expectedPurgeAt: Instant get() = confirmedAt.plus(TRASH_RETENTION)

    companion object {
        /** Durée de séjour d'un média dans la corbeille d'Android avant sa suppression définitive. */
        val TRASH_RETENTION: Duration = Duration.ofDays(30)
    }
}
