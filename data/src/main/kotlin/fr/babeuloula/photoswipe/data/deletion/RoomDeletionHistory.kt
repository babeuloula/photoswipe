package fr.babeuloula.photoswipe.data.deletion

import fr.babeuloula.photoswipe.data.database.ConfirmedDeletionDao
import fr.babeuloula.photoswipe.data.database.ConfirmedDeletionEntity
import fr.babeuloula.photoswipe.domain.model.ConfirmedDeletion
import fr.babeuloula.photoswipe.domain.model.MediaId
import fr.babeuloula.photoswipe.domain.port.DeletionHistory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject

/**
 * [DeletionHistory] sur la base Room, table `confirmed_deletion` (ADR 0034). Les instants sont
 * stockés en millisecondes epoch, les jours en jours depuis l'epoch.
 */
class RoomDeletionHistory
    @Inject
    constructor(
        private val dao: ConfirmedDeletionDao,
    ) : DeletionHistory {
        override suspend fun record(deletions: List<ConfirmedDeletion>) {
            if (deletions.isNotEmpty()) dao.insert(deletions.map { it.toEntity() })
        }

        override fun observe(): Flow<List<ConfirmedDeletion>> =
            dao.observeAll().map { rows -> rows.map { it.toDomain() } }

        override suspend fun forgetConfirmedBefore(instant: Instant) {
            dao.deleteConfirmedBefore(instant.toEpochMilli())
        }

        override suspend fun forget(mediaIds: Set<MediaId>) {
            // Par lots, sous la limite de paramètres d'une requête SQLite.
            mediaIds.map { it.value }.chunked(MAX_IDS_PER_QUERY).forEach { dao.deleteFor(it) }
        }

        private fun ConfirmedDeletion.toEntity(): ConfirmedDeletionEntity =
            ConfirmedDeletionEntity(
                mediaId = mediaId.value,
                day = day?.toEpochDay(),
                sizeBytes = sizeBytes,
                confirmedAt = confirmedAt.toEpochMilli(),
            )

        private fun ConfirmedDeletionEntity.toDomain(): ConfirmedDeletion =
            ConfirmedDeletion(
                mediaId = MediaId(mediaId),
                day = day?.let(LocalDate::ofEpochDay),
                sizeBytes = sizeBytes,
                confirmedAt = Instant.ofEpochMilli(confirmedAt),
            )

        private companion object {
            /** Identifiants par requête, loin sous la limite de paramètres de SQLite. */
            const val MAX_IDS_PER_QUERY = 500
        }
    }
