package fr.babeuloula.photoswipe.ui.home

import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import coil3.ImageLoader
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import fr.babeuloula.photoswipe.MainActivity
import fr.babeuloula.photoswipe.domain.fixtures.FakeDecisionJournal
import fr.babeuloula.photoswipe.domain.fixtures.FakeDeletionHistory
import fr.babeuloula.photoswipe.domain.fixtures.FakeMediaSource
import fr.babeuloula.photoswipe.domain.fixtures.FakeReviewRepository
import fr.babeuloula.photoswipe.domain.fixtures.FakeSettingsRepository
import fr.babeuloula.photoswipe.domain.fixtures.FixedClock
import fr.babeuloula.photoswipe.domain.fixtures.aMediaItem
import fr.babeuloula.photoswipe.domain.fixtures.aReviewEntry
import fr.babeuloula.photoswipe.domain.model.RatingInvitation
import fr.babeuloula.photoswipe.domain.service.ReviewWindowProvider
import fr.babeuloula.photoswipe.domain.service.SameDayMatcher
import fr.babeuloula.photoswipe.domain.usecase.GetReviewDays
import fr.babeuloula.photoswipe.domain.usecase.GetSessionSummary
import fr.babeuloula.photoswipe.domain.usecase.MarkRatingAsked
import fr.babeuloula.photoswipe.domain.usecase.RecordTodaySorted
import fr.babeuloula.photoswipe.domain.usecase.ReopenDay
import fr.babeuloula.photoswipe.ui.common.LocalImageLoader
import fr.babeuloula.photoswipe.ui.theme.PhotoSwipeTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.LocalDateTime

/** Accueil relié à son ViewModel : annonce des médias envoyés à la corbeille depuis le résumé. */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class)
class HomeRouteTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val clock = FixedClock.at(LocalDateTime.of(2026, 7, 15, 10, 0))
    private val repository = FakeReviewRepository()
    private val history = FakeDeletionHistory()
    private var settings = FakeSettingsRepository()
    private var source = FakeMediaSource()

    /** Affiche l'accueil ; l'annonce disparaît une fois faite, comme dans le graphe de navigation. */
    private fun show(
        trashedCount: Int,
        onAnnounced: () -> Unit,
    ) {
        val pending = mutableStateOf(trashedCount)
        val viewModel =
            HomeViewModel(
                GetReviewDays(ReviewWindowProvider(clock), SameDayMatcher(), source, repository, clock),
                GetSessionSummary(
                    repository,
                    FakeDecisionJournal(repository),
                    history,
                    ReviewWindowProvider(clock),
                    clock,
                ),
                settings,
                mediaManagement = { false },
                reopenDay =
                    ReopenDay(SameDayMatcher(), FakeMediaSource(), repository, FakeDecisionJournal(repository), clock),
                rating = RatingInvitationActions(RecordTodaySorted(settings, clock), MarkRatingAsked(settings)),
            )
        composeRule.activity.setContent {
            CompositionLocalProvider(LocalImageLoader provides ImageLoader(composeRule.activity)) {
                PhotoSwipeTheme {
                    HomeRoute(
                        navigation = HomeNavigation(openDeck = {}, openSettings = {}, openSummary = {}),
                        confirmed =
                            if (pending.value != 0) {
                                ConfirmationAnnouncement(pending.value) {
                                    onAnnounced()
                                    pending.value = 0
                                }
                            } else {
                                null
                            },
                        viewModel = viewModel,
                    )
                }
            }
        }
    }

    @Test
    fun `announces the media moved to the trash once`() {
        var announced = 0
        show(trashedCount = 3) { announced++ }

        composeRule.waitUntil(TIMEOUT_MILLIS) {
            composeRule
                .onAllNodesWithText(
                    "3 items moved to the trash",
                    substring = true,
                ).fetchSemanticsNodes()
                .isNotEmpty()
        }
        assertEquals(1, announced)
    }

    @Test
    fun `announces nothing without a confirmation`() {
        var announced = 0
        show(trashedCount = 0) { announced++ }
        composeRule.waitForIdle()

        assertEquals(
            0,
            composeRule.onAllNodesWithText("moved to the trash", substring = true).fetchSemanticsNodes().size,
        )
        assertEquals(0, announced)
    }

    @Test
    fun `waits for the trash announcement before inviting to rate the app`() {
        // Aujourd'hui entièrement trié, troisième jour différent : l'invitation est due.
        settings =
            FakeSettingsRepository(
                ratingInvitation = RatingInvitation(sortedDays = 2, lastSortedDay = LocalDate.of(2026, 7, 14)),
            )
        source =
            FakeMediaSource(
                listOf(
                    aMediaItem(
                        id = 1,
                        takenAt = LocalDateTime.of(2025, 7, 15, 12, 0).atZone(FixedClock.PARIS).toInstant(),
                    ),
                ),
            )
        runBlocking { repository.save(aReviewEntry(mediaId = 1, reviewYear = 2026)) }
        composeRule.mainClock.autoAdvance = false
        show(trashedCount = 3) {}
        composeRule.mainClock.advanceTimeBy(1_000)

        composeRule.onAllNodesWithText("3 items moved to the trash", substring = true).assertCountEquals(1)
        composeRule.onAllNodesWithText("Enjoying PhotoSwipe?").assertCountEquals(0)

        // Fin de l'annonce (message long, 10 s) : l'invitation s'affiche.
        composeRule.mainClock.advanceTimeBy(15_000)
        composeRule.onAllNodesWithText("Enjoying PhotoSwipe?").assertCountEquals(1)
    }

    private companion object {
        const val TIMEOUT_MILLIS = 5_000L
    }
}
