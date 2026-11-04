package fr.babeuloula.photoswipe.data.review

import fr.babeuloula.photoswipe.data.database.ReviewEntryEntity
import fr.babeuloula.photoswipe.domain.model.MediaId
import fr.babeuloula.photoswipe.domain.model.ReviewEntry
import java.time.Instant

/**
 * Conversion entre une ligne `review_entry` et le modèle du domaine.
 *
 * L'instant de décision est stocké en millisecondes epoch (SPEC §6) : la partie plus fine qu'une
 * milliseconde est perdue à l'écriture, ce qui suffit à ordonner les décisions.
 */
internal fun ReviewEntryEntity.toDomain(): ReviewEntry =
    ReviewEntry(
        mediaId = MediaId(mediaId),
        reviewYear = reviewYear,
        decision = decision,
        sizeBytes = sizeBytes,
        decidedAt = Instant.ofEpochMilli(decidedAt),
        favoritePending = favoritePending,
    )

/** Ligne `review_entry` qui enregistre [ReviewEntry]. */
internal fun ReviewEntry.toEntity(): ReviewEntryEntity =
    ReviewEntryEntity(
        mediaId = mediaId.value,
        reviewYear = reviewYear,
        decision = decision,
        favoritePending = favoritePending,
        sizeBytes = sizeBytes,
        decidedAt = decidedAt.toEpochMilli(),
    )
