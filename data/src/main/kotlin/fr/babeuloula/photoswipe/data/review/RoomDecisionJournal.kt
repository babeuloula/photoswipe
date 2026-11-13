package fr.babeuloula.photoswipe.data.review

import fr.babeuloula.photoswipe.data.database.DecisionJournalDao
import fr.babeuloula.photoswipe.data.database.DecisionRecordEntity
import fr.babeuloula.photoswipe.data.database.ReviewEntryDao
import fr.babeuloula.photoswipe.domain.model.DecisionRecord
import fr.babeuloula.photoswipe.domain.model.MediaId
import fr.babeuloula.photoswipe.domain.model.ReviewEntry
import fr.babeuloula.photoswipe.domain.port.DecisionJournal
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

/**
 * [DecisionJournal] sur la base Room, dans la même base que les décisions (ADR 0019) : écrire ou
 * annuler une décision et tenir le journal se font dans une seule transaction.
 *
 * @param dao accès au journal et aux décisions.
 * @param clock fuseau de l'appareil, pour rattacher une décision au jour où elle est prise (ADR 0037).
 */
class RoomDecisionJournal
    @Inject
    constructor(
        private val dao: DecisionJournalDao,
        private val clock: Clock,
    ) : DecisionJournal {
        override suspend fun record(
            day: LocalDate,
            made: ReviewEntry,
            previous: ReviewEntry?,
        ): DecisionRecord {
            val entity =
                DecisionRecordEntity(day = day.toEpochDay(), made = made.toEntity(), previous = previous?.toEntity())
            return entity.copy(id = dao.record(entity, made.activityDay())).toDomain()
        }

        override suspend fun undo(record: DecisionRecord) {
            dao.undo(record.toEntity(), record.made.activityDay())
        }

        override suspend fun recordsFor(day: LocalDate): List<DecisionRecord> =
            dao.recordsFor(day.toEpochDay()).map { it.toDomain() }

        override suspend fun forget(mediaIds: Set<MediaId>) {
            mediaIds.map { it.value }.chunked(ReviewEntryDao.MAX_IDS_PER_QUERY).forEach { dao.forget(it) }
        }

        override suspend fun forgetDaysBefore(day: LocalDate) {
            dao.forgetDaysBefore(day.toEpochDay())
        }

        /** Jour où la décision a été prise, dans le fuseau de l'appareil : celui de son activité (ADR 0037). */
        private fun ReviewEntry.activityDay(): Long = decidedAt.atZone(clock.zone).toLocalDate().toEpochDay()

        private fun DecisionRecordEntity.toDomain(): DecisionRecord =
            DecisionRecord(id, LocalDate.ofEpochDay(day), made.toDomain(), previous?.toDomain())

        private fun DecisionRecord.toEntity(): DecisionRecordEntity =
            DecisionRecordEntity(id, day.toEpochDay(), made.toEntity(), previous?.toEntity())
    }
