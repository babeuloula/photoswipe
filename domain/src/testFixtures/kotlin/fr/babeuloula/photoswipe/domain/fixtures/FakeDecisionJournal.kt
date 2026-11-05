package fr.babeuloula.photoswipe.domain.fixtures

import fr.babeuloula.photoswipe.domain.model.DecisionRecord
import fr.babeuloula.photoswipe.domain.model.MediaId
import fr.babeuloula.photoswipe.domain.model.ReviewEntry
import fr.babeuloula.photoswipe.domain.port.DecisionJournal
import java.time.LocalDate

/**
 * [DecisionJournal] en mémoire, qui écrit les décisions dans [reviews] comme l'implémentation Room
 * écrit dans la même base : une décision et sa ligne de journal changent ensemble.
 *
 * Les identifiants des lignes croissent à partir de 1, dans l'ordre d'écriture. [records] permet
 * au test de vérifier le journal.
 *
 * @param reviews dépôt des décisions, partagé avec les use cases testés.
 */
class FakeDecisionJournal(
    private val reviews: FakeReviewRepository = FakeReviewRepository(),
) : DecisionJournal {
    private val stored = mutableListOf<DecisionRecord>()
    private var lastId = 0L

    /** Lignes du journal, tous jours confondus, dans l'ordre d'écriture. */
    val records: List<DecisionRecord> get() = stored.toList()

    override suspend fun record(
        day: LocalDate,
        made: ReviewEntry,
        previous: ReviewEntry?,
    ): DecisionRecord {
        reviews.save(made)
        return DecisionRecord(++lastId, day, made, previous).also { stored += it }
    }

    override suspend fun undo(record: DecisionRecord) {
        val previous = record.previous
        if (previous == null) reviews.delete(record.mediaId, record.made.reviewYear) else reviews.save(previous)
        stored.removeAll { it.id == record.id }
    }

    override suspend fun recordsFor(day: LocalDate): List<DecisionRecord> = stored.filter { it.day == day }

    override suspend fun forget(mediaIds: Set<MediaId>) {
        stored.removeAll { it.mediaId in mediaIds }
    }

    override suspend fun forgetDaysBefore(day: LocalDate) {
        stored.removeAll { it.day < day }
    }
}
