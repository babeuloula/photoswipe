package fr.babeuloula.photoswipe.ui.home

import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import fr.babeuloula.photoswipe.MainActivity
import fr.babeuloula.photoswipe.domain.fixtures.FakeMediaSource
import fr.babeuloula.photoswipe.domain.fixtures.FakeReviewRepository
import fr.babeuloula.photoswipe.domain.fixtures.FakeSettingsRepository
import fr.babeuloula.photoswipe.domain.fixtures.FixedClock
import fr.babeuloula.photoswipe.domain.service.ReviewWindowProvider
import fr.babeuloula.photoswipe.domain.service.SameDayMatcher
import fr.babeuloula.photoswipe.domain.usecase.GetReviewDays
import fr.babeuloula.photoswipe.domain.usecase.GetSessionSummary
import fr.babeuloula.photoswipe.ui.theme.PhotoSwipeTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
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

    /** Affiche l'accueil ; l'annonce disparaît une fois faite, comme dans le graphe de navigation. */
    private fun show(
        trashedCount: Int,
        onAnnounced: () -> Unit,
    ) {
        val pending = mutableStateOf(trashedCount)
        val viewModel =
            HomeViewModel(
                GetReviewDays(ReviewWindowProvider(clock), SameDayMatcher(), FakeMediaSource(), repository, clock),
                GetSessionSummary(repository),
                FakeSettingsRepository(),
                mediaManagement = { false },
            )
        composeRule.activity.setContent {
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

    private companion object {
        const val TIMEOUT_MILLIS = 5_000L
    }
}
