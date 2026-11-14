package fr.babeuloula.photoswipe.ui.summary

import android.os.Build
import fr.babeuloula.photoswipe.MainDispatcherRule
import fr.babeuloula.photoswipe.domain.fixtures.FakeActivityHistory
import fr.babeuloula.photoswipe.domain.fixtures.FakeDecisionJournal
import fr.babeuloula.photoswipe.domain.fixtures.FakeDeletionHistory
import fr.babeuloula.photoswipe.domain.fixtures.FakeMediaActionRequestFactory
import fr.babeuloula.photoswipe.domain.fixtures.FakeMediaSource
import fr.babeuloula.photoswipe.domain.fixtures.FakeReviewRepository
import fr.babeuloula.photoswipe.domain.fixtures.FakeSettingsRepository
import fr.babeuloula.photoswipe.domain.fixtures.FakeUsageStatisticsRepository
import fr.babeuloula.photoswipe.domain.fixtures.FixedClock
import fr.babeuloula.photoswipe.domain.fixtures.aMediaItem
import fr.babeuloula.photoswipe.domain.fixtures.aReviewEntry
import fr.babeuloula.photoswipe.domain.model.ManageMediaTip
import fr.babeuloula.photoswipe.domain.model.MediaAccess
import fr.babeuloula.photoswipe.domain.model.MediaId
import fr.babeuloula.photoswipe.domain.model.ReviewDecision
import fr.babeuloula.photoswipe.domain.service.ReviewWindowProvider
import fr.babeuloula.photoswipe.domain.usecase.CleanOrphanEntries
import fr.babeuloula.photoswipe.domain.usecase.GetMarkedPreviews
import fr.babeuloula.photoswipe.domain.usecase.GetSessionSummary
import fr.babeuloula.photoswipe.domain.usecase.OfferManageMediaTip
import fr.babeuloula.photoswipe.domain.usecase.PrepareTrash
import fr.babeuloula.photoswipe.domain.usecase.RecordTrashResult
import fr.babeuloula.photoswipe.permission.OrphanCleanup
import fr.babeuloula.photoswipe.platform.PlatformFeatures
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.LocalDateTime

/**
 * Confirmation du résumé sur Android 10, sans corbeille : PhotoSwipe demande sa propre confirmation,
 * puis supprime lui-même et pour de bon, sans dialogue d'Android (PS-184).
 */
class SummaryPermanentDeletionTest {
    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val repository = FakeReviewRepository()
    private val gallery = FakeMediaSource((1L..5L).map { aMediaItem(id = it) })
    private val clock = FixedClock.at(LocalDateTime.of(2026, 7, 15, 21, 0))
    private val history = FakeDeletionHistory()
    private val statistics = FakeUsageStatisticsRepository()
    private val settings = FakeSettingsRepository()

    private fun summaryViewModel(directDeletion: FakeDirectDeletion): SummaryViewModel =
        SummaryViewModel(
            getSessionSummary =
                GetSessionSummary(
                    repository,
                    FakeDecisionJournal(repository),
                    history,
                    ReviewWindowProvider(clock),
                    clock,
                ),
            getMarkedPreviews = GetMarkedPreviews(repository, gallery),
            confirmation =
                ConfirmationActions(
                    prepareTrash = PrepareTrash(repository, gallery, FakeMediaActionRequestFactory(batchSize = 2)),
                    recordTrashResult =
                        RecordTrashResult(
                            repository,
                            FakeDecisionJournal(repository),
                            statistics,
                            history,
                            FakeActivityHistory(),
                            ReviewWindowProvider(clock),
                            clock,
                        ),
                    orphanCleanup =
                        OrphanCleanup(
                            CleanOrphanEntries(repository, gallery, FakeDecisionJournal(repository)),
                            accessReader = { MediaAccess.FULL },
                            guard = { true },
                        ),
                    offerManageMediaTip = OfferManageMediaTip(settings),
                ),
            gallery =
                SummaryGallery(
                    mediaActionIntents = { error("no Android dialog on android 10") },
                    mediaManagement = { false },
                    directDeletion = directDeletion,
                    platform = PlatformFeatures.forSdk(Build.VERSION_CODES.Q),
                ),
        )

    private suspend fun mark(vararg ids: Long) {
        ids.forEach { repository.save(aReviewEntry(mediaId = it, decision = ReviewDecision.MARKED_FOR_DELETION)) }
    }

    private fun TestScope.collectEffects(viewModel: SummaryViewModel): List<SummaryEffect> {
        val effects = mutableListOf<SummaryEffect>()
        backgroundScope.launch(
            UnconfinedTestDispatcher(testScheduler),
        ) { viewModel.uiEffects.collect { effects += it } }
        return effects
    }

    private fun decisions() = repository.entries.sortedBy { it.mediaId.value }.map { it.decision }

    private fun loaded(viewModel: SummaryViewModel) = viewModel.uiState.value as SummaryUiState.Loaded

    @Test
    fun `asks its own confirmation first, as the deletion cannot be undone`() =
        runTest {
            mark(1, 2)
            val deletion = FakeDirectDeletion()
            val viewModel = summaryViewModel(deletion)
            val effects = collectEffects(viewModel)

            viewModel.onEvent(SummaryEvent.Confirm)

            assertTrue(loaded(viewModel).askingTrashConfirmation)
            assertTrue(effects.isEmpty())
            assertTrue(deletion.deleted.isEmpty())
        }

    @Test
    fun `deletes nothing when its confirmation is dismissed`() =
        runTest {
            mark(1)
            val deletion = FakeDirectDeletion()
            val viewModel = summaryViewModel(deletion)
            collectEffects(viewModel)

            viewModel.onEvent(SummaryEvent.Confirm)
            viewModel.onEvent(SummaryEvent.TrashConfirmationDismissed)

            assertFalse(loaded(viewModel).askingTrashConfirmation)
            assertTrue(deletion.deleted.isEmpty())
            assertEquals(listOf(ReviewDecision.MARKED_FOR_DELETION), decisions())
        }

    @Test
    fun `deletes every batch for good without any Android dialog, then goes home to announce it`() =
        runTest {
            mark(1, 2, 3)
            val deletion = FakeDirectDeletion()
            val viewModel = summaryViewModel(deletion)
            val effects = collectEffects(viewModel)

            viewModel.onEvent(SummaryEvent.Confirm)
            viewModel.onEvent(SummaryEvent.TrashConfirmationAccepted)

            assertEquals(listOf(MediaId(1), MediaId(2), MediaId(3)), deletion.deleted)
            assertEquals(List(3) { ReviewDecision.TRASHED }, decisions())
            assertEquals(listOf<SummaryEffect>(SummaryEffect.GoHome(trashedCount = 3)), effects)
            assertEquals(3, statistics.current.deletedCount)
        }

    @Test
    fun `keeps marked what Android refused to delete`() =
        runTest {
            mark(1, 2)
            val viewModel = summaryViewModel(FakeDirectDeletion(refused = setOf(MediaId(2))))
            val effects = collectEffects(viewModel)

            viewModel.onEvent(SummaryEvent.Confirm)
            viewModel.onEvent(SummaryEvent.TrashConfirmationAccepted)

            assertEquals(listOf(ReviewDecision.TRASHED, ReviewDecision.MARKED_FOR_DELETION), decisions())
            assertEquals(SummaryEffect.GoHome(trashedCount = 1), effects.last())
        }

    @Test
    fun `says nothing was deleted when Android refused every media`() =
        runTest {
            mark(1)
            val viewModel = summaryViewModel(FakeDirectDeletion(refused = setOf(MediaId(1))))
            val effects = collectEffects(viewModel)

            viewModel.onEvent(SummaryEvent.Confirm)
            viewModel.onEvent(SummaryEvent.TrashConfirmationAccepted)

            assertEquals(listOf<SummaryEffect>(SummaryEffect.NothingApplied(stillMarkedCount = 1)), effects)
            assertEquals(listOf(ReviewDecision.MARKED_FOR_DELETION), decisions())
        }

    @Test
    fun `never offers media management, which android 10 does not have`() =
        runTest {
            mark(1)
            val viewModel = summaryViewModel(FakeDirectDeletion())
            collectEffects(viewModel)

            viewModel.onEvent(SummaryEvent.Confirm)
            viewModel.onEvent(SummaryEvent.TrashConfirmationAccepted)

            assertEquals(ManageMediaTip.NOT_OFFERED, settings.manageMediaTip)
        }
}
