package fr.babeuloula.photoswipe.ui.deck

import fr.babeuloula.photoswipe.domain.fixtures.FakeMediaSource
import fr.babeuloula.photoswipe.domain.fixtures.aMediaItem
import fr.babeuloula.photoswipe.domain.model.MediaId
import fr.babeuloula.photoswipe.domain.model.ReviewDecision
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Une paire RAW + JPEG dans la pile : une carte, une décision pour les deux fichiers (PS-166). */
@RunWith(RobolectricTestRunner::class)
class DeckRawPairTest : DeckViewModelTestBase() {
    private val raw = aMediaItem(id = 9, sizeBytes = 25_000_000)
    private val pair = takenOn(1, 2023).copy(sizeBytes = 3_000_000, rawCompanion = raw)

    private fun DeckViewModel.loaded() = uiState.value as DeckUiState.Loaded

    @Test
    fun `shows a pair as one card weighing both files`() =
        runTest {
            val viewModel = deckViewModel(FakeMediaSource(listOf(pair, takenOn(2, 2022))))

            val card = viewModel.loaded().cards.first()
            assertEquals(MediaId(1), card.id)
            assertTrue(card.raw)
            assertEquals(28_000_000L, card.sizeBytes)
        }

    @Test
    fun `marks both files and counts a single sorted card`() =
        runTest {
            val viewModel = deckViewModel(FakeMediaSource(listOf(pair, takenOn(2, 2022))))

            viewModel.onEvent(DeckEvent.MarkForDeletion)

            assertEquals(
                setOf(MediaId(1), MediaId(9)),
                repository.entries
                    .filter { it.decision == ReviewDecision.MARKED_FOR_DELETION }
                    .map { it.mediaId }
                    .toSet(),
            )
            assertEquals(1, viewModel.loaded().sortedCount)
        }

    @Test
    fun `undoes the decision of both files at once`() =
        runTest {
            val viewModel = deckViewModel(FakeMediaSource(listOf(pair, takenOn(2, 2022))))
            viewModel.onEvent(DeckEvent.Keep)

            viewModel.onEvent(DeckEvent.Undo)

            assertTrue(repository.entries.isEmpty())
            assertEquals(
                MediaId(1),
                viewModel
                    .loaded()
                    .cards
                    .first()
                    .id,
            )
            assertEquals(0, viewModel.loaded().sortedCount)
        }
}
