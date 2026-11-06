package fr.babeuloula.photoswipe.ui.summary

import android.app.PendingIntent
import android.content.Intent
import android.content.IntentSender
import androidx.lifecycle.SavedStateHandle
import fr.babeuloula.photoswipe.MainDispatcherRule
import fr.babeuloula.photoswipe.domain.fixtures.FakeDecisionJournal
import fr.babeuloula.photoswipe.domain.fixtures.FakeMediaActionRequestFactory
import fr.babeuloula.photoswipe.domain.fixtures.FakeMediaSource
import fr.babeuloula.photoswipe.domain.fixtures.FakeReviewRepository
import fr.babeuloula.photoswipe.domain.fixtures.FakeSettingsRepository
import fr.babeuloula.photoswipe.domain.fixtures.aMediaItem
import fr.babeuloula.photoswipe.domain.fixtures.aReviewEntry
import fr.babeuloula.photoswipe.domain.model.ManageMediaTip
import fr.babeuloula.photoswipe.domain.model.MediaAccess
import fr.babeuloula.photoswipe.domain.model.MediaId
import fr.babeuloula.photoswipe.domain.model.ReviewDecision
import fr.babeuloula.photoswipe.domain.model.ReviewEntry
import fr.babeuloula.photoswipe.domain.model.SessionSummary
import fr.babeuloula.photoswipe.domain.port.ReviewRepository
import fr.babeuloula.photoswipe.domain.usecase.CleanOrphanEntries
import fr.babeuloula.photoswipe.domain.usecase.GetSessionSummary
import fr.babeuloula.photoswipe.domain.usecase.OfferManageMediaTip
import fr.babeuloula.photoswipe.domain.usecase.PrepareTrash
import fr.babeuloula.photoswipe.domain.usecase.RecordTrashResult
import fr.babeuloula.photoswipe.permission.OrphanCleanup
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.IOException

/** Robolectric : la route typée se relit depuis le `SavedStateHandle` avec le `Bundle` d'Android. */
@RunWith(RobolectricTestRunner::class)
class SummaryViewModelTest {
    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val repository = FakeReviewRepository()

    /** Galerie : les médias 1 à 5 existent encore. */
    private val gallery = FakeMediaSource((1L..5L).map { aMediaItem(id = it) })

    /** Dialogue système factice : un vrai `IntentSender`, qui ne sera jamais lancé ici. */
    private val intentSender: IntentSender =
        PendingIntent
            .getActivity(
                RuntimeEnvironment.getApplication(),
                0,
                Intent(),
                PendingIntent.FLAG_IMMUTABLE,
            ).intentSender

    private val factory = FakeMediaActionRequestFactory(batchSize = 2)

    private val settings = FakeSettingsRepository()

    /** Accès « Gestion des médias » simulé, refusé par défaut. */
    private var mediaManagementGranted = false

    private fun summaryViewModel(
        keptCount: Int = 0,
        reviews: ReviewRepository = repository,
    ): SummaryViewModel =
        SummaryViewModel(
            savedStateHandle = SavedStateHandle(mapOf("keptCount" to keptCount)),
            getSessionSummary = GetSessionSummary(reviews),
            confirmation =
                ConfirmationActions(
                    prepareTrash = PrepareTrash(reviews, gallery, factory),
                    recordTrashResult = RecordTrashResult(reviews, FakeDecisionJournal(repository)),
                    orphanCleanup =
                        OrphanCleanup(
                            CleanOrphanEntries(reviews, gallery, FakeDecisionJournal(repository)),
                            accessReader = { MediaAccess.FULL },
                            guard = { true },
                        ),
                    offerManageMediaTip = OfferManageMediaTip(settings),
                ),
            mediaActionIntents = { intentSender },
            mediaManagement = { mediaManagementGranted },
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

    @Test
    fun `shows the media kept during the session and all pending requests`() =
        runTest {
            repository.save(
                aReviewEntry(mediaId = 1, decision = ReviewDecision.MARKED_FOR_DELETION, sizeBytes = 3_000_000),
            )
            repository.save(
                aReviewEntry(mediaId = 2, decision = ReviewDecision.MARKED_FOR_DELETION, sizeBytes = 45_000_000),
            )
            repository.save(aReviewEntry(mediaId = 3, decision = ReviewDecision.KEPT, favoritePending = true))

            val viewModel = summaryViewModel(keptCount = 5)

            assertEquals(
                SummaryUiState.Loaded(
                    SessionSummary(
                        keptCount = 5,
                        markedForDeletionCount = 2,
                        favoriteCount = 1,
                        estimatedFreedBytes = 48_000_000,
                    ),
                ),
                viewModel.uiState.value,
            )
        }

    @Test
    fun `shows an empty summary when nothing is waiting`() =
        runTest {
            assertEquals(
                SummaryUiState.Loaded(SessionSummary(0, 0, 0, 0)),
                summaryViewModel().uiState.value,
            )
        }

    @Test
    fun `shows an error when the database cannot be read, then recovers on retry`() =
        runTest {
            val reviews = FailingOnceRepository()
            val viewModel = summaryViewModel(reviews = reviews)
            assertEquals(SummaryUiState.Error, viewModel.uiState.value)

            viewModel.onEvent(SummaryEvent.Retry)

            assertEquals(SummaryUiState.Loaded(SessionSummary(0, 0, 0, 0)), viewModel.uiState.value)
        }

    @Test
    fun `goes home later without changing anything`() =
        runTest {
            repository.save(aReviewEntry(mediaId = 1, decision = ReviewDecision.MARKED_FOR_DELETION))
            val viewModel = summaryViewModel()

            viewModel.onEvent(SummaryEvent.Later)

            assertEquals(SummaryEffect.GoHome(), viewModel.uiEffects.first())
            assertEquals(ReviewDecision.MARKED_FOR_DELETION, repository.entries.single().decision)
        }

    @Test
    fun `presents one system dialog per batch of marked media`() =
        runTest {
            mark(1, 2, 3)
            val viewModel = summaryViewModel()
            val effects = collectEffects(viewModel)

            viewModel.onEvent(SummaryEvent.Confirm)

            assertEquals(listOf<SummaryEffect>(SummaryEffect.LaunchSystemRequest(intentSender)), effects)
            assertTrue((viewModel.uiState.value as SummaryUiState.Loaded).confirming)

            viewModel.onEvent(SummaryEvent.SystemRequestAnswered(confirmed = true))

            assertEquals(2, effects.count { it is SummaryEffect.LaunchSystemRequest })
        }

    @Test
    fun `moves the confirmed media to the trash and goes home to announce it`() =
        runTest {
            mark(1, 2, 3)
            val viewModel = summaryViewModel()
            val effects = collectEffects(viewModel)

            viewModel.onEvent(SummaryEvent.Confirm)
            viewModel.onEvent(SummaryEvent.SystemRequestAnswered(confirmed = true))
            viewModel.onEvent(SummaryEvent.SystemRequestAnswered(confirmed = true))

            assertEquals(List(3) { ReviewDecision.TRASHED }, decisions())
            assertEquals(SummaryEffect.GoHome(trashedCount = 3), effects.last())
        }

    @Test
    fun `forgets the trashed media once they left the gallery`() =
        runTest {
            mark(1, 2)
            repository.save(aReviewEntry(mediaId = 3))
            val viewModel = summaryViewModel()
            val effects = collectEffects(viewModel)

            viewModel.onEvent(SummaryEvent.Confirm)
            gallery.remove(MediaId(1))
            gallery.remove(MediaId(2))
            viewModel.onEvent(SummaryEvent.SystemRequestAnswered(confirmed = true))

            assertEquals(listOf(3L), repository.entries.map { it.mediaId.value })
            assertEquals(SummaryEffect.GoHome(trashedCount = 2), effects.last())
        }

    @Test
    fun `keeps the marks of a refused batch`() =
        runTest {
            mark(1, 2, 3)
            val viewModel = summaryViewModel()
            val effects = collectEffects(viewModel)

            viewModel.onEvent(SummaryEvent.Confirm)
            viewModel.onEvent(SummaryEvent.SystemRequestAnswered(confirmed = false))
            viewModel.onEvent(SummaryEvent.SystemRequestAnswered(confirmed = true))

            assertEquals(
                listOf(ReviewDecision.MARKED_FOR_DELETION, ReviewDecision.MARKED_FOR_DELETION, ReviewDecision.TRASHED),
                decisions(),
            )
            assertEquals(SummaryEffect.GoHome(trashedCount = 1), effects.last())
        }

    @Test
    fun `stays on the updated summary when every batch is refused`() =
        runTest {
            mark(1)
            val viewModel = summaryViewModel()
            val effects = collectEffects(viewModel)

            viewModel.onEvent(SummaryEvent.Confirm)
            viewModel.onEvent(SummaryEvent.SystemRequestAnswered(confirmed = false))

            assertTrue(effects.none { it is SummaryEffect.GoHome })
            assertEquals(listOf(ReviewDecision.MARKED_FOR_DELETION), decisions())
            val state = viewModel.uiState.value as SummaryUiState.Loaded
            assertEquals(1, state.summary.markedForDeletionCount)
            assertFalse(state.confirming)
        }

    @Test
    fun `says the marks are kept when every batch is refused or cancelled`() =
        runTest {
            mark(1, 2, 3)
            val viewModel = summaryViewModel()
            val effects = collectEffects(viewModel)

            viewModel.onEvent(SummaryEvent.Confirm)
            viewModel.onEvent(SummaryEvent.SystemRequestAnswered(confirmed = false))
            viewModel.onEvent(SummaryEvent.SystemRequestAnswered(confirmed = false))

            assertEquals(List(3) { ReviewDecision.MARKED_FOR_DELETION }, decisions())
            assertEquals(SummaryEffect.NothingApplied(stillMarkedCount = 3), effects.last())
        }

    @Test
    fun `lets the user try again after refusing`() =
        runTest {
            mark(1)
            val viewModel = summaryViewModel()
            val effects = collectEffects(viewModel)
            viewModel.onEvent(SummaryEvent.Confirm)
            viewModel.onEvent(SummaryEvent.SystemRequestAnswered(confirmed = false))
            assertTrue((viewModel.uiState.value as SummaryUiState.Loaded).canConfirm)

            viewModel.onEvent(SummaryEvent.Confirm)
            viewModel.onEvent(SummaryEvent.SystemRequestAnswered(confirmed = true))

            assertEquals(2, effects.count { it is SummaryEffect.LaunchSystemRequest })
            assertEquals(listOf(ReviewDecision.TRASHED), decisions())
            assertEquals(SummaryEffect.GoHome(trashedCount = 1), effects.last())
        }

    @Test
    fun `keeps the marks and says so when the system dialogs cannot be prepared`() =
        runTest {
            mark(1)
            val viewModel = summaryViewModel(reviews = FailingOnceRepository(repository, failOnRead = 2))
            val effects = collectEffects(viewModel)

            viewModel.onEvent(SummaryEvent.Confirm)

            assertTrue(effects.none { it is SummaryEffect.LaunchSystemRequest })
            assertEquals(listOf(ReviewDecision.MARKED_FOR_DELETION), decisions())
            assertEquals(SummaryEffect.NothingApplied(stillMarkedCount = 1), effects.last())
            assertTrue((viewModel.uiState.value as SummaryUiState.Loaded).canConfirm)
        }

    @Test
    fun `says when the marked media are no longer in the gallery`() =
        runTest {
            mark(99)
            val viewModel = summaryViewModel()
            val effects = collectEffects(viewModel)

            viewModel.onEvent(SummaryEvent.Confirm)

            assertEquals(listOf<SummaryEffect>(SummaryEffect.MarkedMediaMissing), effects)
            assertFalse((viewModel.uiState.value as SummaryUiState.Loaded).confirming)
        }

    @Test
    fun `cannot confirm when nothing is marked`() =
        runTest {
            val viewModel = summaryViewModel()
            val effects = collectEffects(viewModel)

            assertFalse((viewModel.uiState.value as SummaryUiState.Loaded).canConfirm)
            viewModel.onEvent(SummaryEvent.Confirm)

            assertTrue(effects.isEmpty())
        }

    @Test
    fun `ignores an answer when no dialog is shown`() =
        runTest {
            mark(1)
            val viewModel = summaryViewModel()

            viewModel.onEvent(SummaryEvent.SystemRequestAnswered(confirmed = true))

            assertEquals(listOf(ReviewDecision.MARKED_FOR_DELETION), decisions())
        }

    @Test
    fun `goes back with the back arrow`() =
        runTest {
            val viewModel = summaryViewModel()

            viewModel.onEvent(SummaryEvent.Back)

            assertEquals(SummaryEffect.Close, viewModel.uiEffects.first())
        }

    /** Dépôt dont une seule lecture des marquages échoue : la [failOnRead]-ième (la première par défaut). */
    private class FailingOnceRepository(
        private val delegate: FakeReviewRepository = FakeReviewRepository(),
        private val failOnRead: Int = 1,
    ) : ReviewRepository by delegate {
        private var reads = 0

        override suspend fun entriesMarkedForDeletion(): List<ReviewEntry> {
            reads++
            if (reads == failOnRead) throw IOException("database unavailable")
            return delegate.entriesMarkedForDeletion()
        }
    }

    @Test
    fun `asks its own confirmation before trashing when Android no longer asks`() =
        runTest {
            mark(1, 2)
            mediaManagementGranted = true
            val viewModel = summaryViewModel()
            val effects = collectEffects(viewModel)

            viewModel.onEvent(SummaryEvent.Confirm)
            assertTrue((viewModel.uiState.value as SummaryUiState.Loaded).askingTrashConfirmation)
            assertEquals(emptyList<SummaryEffect>(), effects)

            viewModel.onEvent(SummaryEvent.TrashConfirmationAccepted)

            assertFalse((viewModel.uiState.value as SummaryUiState.Loaded).askingTrashConfirmation)
            assertTrue(effects.single() is SummaryEffect.LaunchSystemRequest)
        }

    @Test
    fun `sends nothing to the trash when its own confirmation is dismissed`() =
        runTest {
            mark(1)
            mediaManagementGranted = true
            val viewModel = summaryViewModel()
            val effects = collectEffects(viewModel)

            viewModel.onEvent(SummaryEvent.Confirm)
            viewModel.onEvent(SummaryEvent.TrashConfirmationDismissed)

            assertFalse((viewModel.uiState.value as SummaryUiState.Loaded).askingTrashConfirmation)
            assertEquals(emptyList<SummaryEffect>(), effects)
            assertEquals(listOf(ReviewDecision.MARKED_FOR_DELETION), decisions())
        }

    @Test
    fun `goes straight to the Android dialog without media management`() =
        runTest {
            mark(1)
            val viewModel = summaryViewModel()
            val effects = collectEffects(viewModel)

            viewModel.onEvent(SummaryEvent.Confirm)

            assertFalse((viewModel.uiState.value as SummaryUiState.Loaded).askingTrashConfirmation)
            assertTrue(effects.single() is SummaryEffect.LaunchSystemRequest)
        }

    @Test
    fun `offers media management on the home screen after an Android dialog was accepted`() =
        runTest {
            mark(1)
            val viewModel = summaryViewModel()
            collectEffects(viewModel)

            viewModel.onEvent(SummaryEvent.Confirm)
            viewModel.onEvent(SummaryEvent.SystemRequestAnswered(confirmed = true))

            assertEquals(ManageMediaTip.OFFERED, settings.manageMediaTip)
        }

    @Test
    fun `does not offer media management when Android dialogs were refused`() =
        runTest {
            mark(1)
            val viewModel = summaryViewModel()
            collectEffects(viewModel)

            viewModel.onEvent(SummaryEvent.Confirm)
            viewModel.onEvent(SummaryEvent.SystemRequestAnswered(confirmed = false))

            assertEquals(ManageMediaTip.NOT_OFFERED, settings.manageMediaTip)
        }
}
