package fr.babeuloula.photoswipe.ui.home

import fr.babeuloula.photoswipe.MainDispatcherRule
import fr.babeuloula.photoswipe.domain.fixtures.FakeDecisionJournal
import fr.babeuloula.photoswipe.domain.fixtures.FakeDeletionHistory
import fr.babeuloula.photoswipe.domain.fixtures.FakeMediaSource
import fr.babeuloula.photoswipe.domain.fixtures.FakeReviewRepository
import fr.babeuloula.photoswipe.domain.fixtures.FakeSettingsRepository
import fr.babeuloula.photoswipe.domain.fixtures.FixedClock
import fr.babeuloula.photoswipe.domain.fixtures.aMediaItem
import fr.babeuloula.photoswipe.domain.fixtures.aReviewEntry
import fr.babeuloula.photoswipe.domain.model.ManageMediaTip
import fr.babeuloula.photoswipe.domain.model.MediaId
import fr.babeuloula.photoswipe.domain.model.MediaItem
import fr.babeuloula.photoswipe.domain.model.RatingInvitation
import fr.babeuloula.photoswipe.domain.model.ReviewDay
import fr.babeuloula.photoswipe.domain.model.ReviewDecision
import fr.babeuloula.photoswipe.domain.model.ReviewDecision.MARKED_FOR_DELETION
import fr.babeuloula.photoswipe.domain.model.Tutorial
import fr.babeuloula.photoswipe.domain.port.MediaSource
import fr.babeuloula.photoswipe.domain.service.ReviewWindowProvider
import fr.babeuloula.photoswipe.domain.service.SameDayMatcher
import fr.babeuloula.photoswipe.domain.usecase.GetReviewDays
import fr.babeuloula.photoswipe.domain.usecase.GetSessionSummary
import fr.babeuloula.photoswipe.domain.usecase.ReopenDay
import fr.babeuloula.photoswipe.ui.whatsnew.WHATS_NEW
import fr.babeuloula.photoswipe.ui.whatsnew.WhatsNewRelease
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime.NOON

class HomeViewModelTest {
    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val today = LocalDate.of(2026, 7, 15)
    private val clock = FixedClock.at(today.atTime(10, 0))
    private val repository = FakeReviewRepository()
    private val history = FakeDeletionHistory()

    @Test
    fun `shows how many media are left to sort today`() =
        runTest {
            val viewModel = homeViewModel(FakeMediaSource(listOf(takenOn(1, 2025), takenOn(2, 2020), takenOn(3, 2019))))
            repository.save(aReviewEntry(mediaId = 2, reviewYear = 2026))

            assertEquals(loadedWithToday(2, memories = 3), (loaded(viewModel)).withoutPreviews())
        }

    @Test
    fun `updates the count after a decision`() =
        runTest {
            val viewModel = homeViewModel(FakeMediaSource(listOf(takenOn(1, 2025), takenOn(2, 2020))))
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

            repository.save(aReviewEntry(mediaId = 1, reviewYear = 2026))

            assertEquals(loadedWithToday(1, memories = 2), (viewModel.uiState.value).withoutPreviews())
        }

    @Test
    fun `refreshes the home screen widget when the days are read again`() =
        runTest {
            val viewModel = homeViewModel(FakeMediaSource(listOf(takenOn(1, 2025), takenOn(2, 2020))))
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
            val afterLoading = widgetRefreshes

            repository.save(aReviewEntry(mediaId = 1, reviewYear = 2026))

            assertTrue(afterLoading >= 1)
            assertTrue(widgetRefreshes > afterLoading)
        }

    @Test
    fun `starts loading`() {
        assertEquals(HomeUiState.Loading, homeViewModel(FakeMediaSource()).uiState.value)
    }

    @Test
    fun `shows an error when the gallery cannot be read, then recovers on retry`() =
        runTest {
            val source = FailingOnceMediaSource(listOf(takenOn(1, 2025)))
            val viewModel = homeViewModel(source)
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

            assertEquals(HomeUiState.Error, viewModel.uiState.value)

            viewModel.onEvent(HomeEvent.Retry)

            assertEquals(loadedWithToday(1), (viewModel.uiState.value).withoutPreviews())
        }

    @Test
    fun `counts media added to the gallery when the app comes back to the foreground`() =
        runTest {
            val gallery = FakeMediaSource(listOf(takenOn(1, 2025)))
            val viewModel = homeViewModel(gallery)
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
            gallery.add(takenOn(2, 2024))

            viewModel.onEvent(HomeEvent.Resumed)

            assertEquals(loadedWithToday(2), (viewModel.uiState.value).withoutPreviews())
        }

    @Test
    fun `opens the deck of the chosen day`() =
        runTest {
            val viewModel = homeViewModel(FakeMediaSource())

            viewModel.onEvent(HomeEvent.OpenDay(today.minusDays(3)))

            assertEquals(HomeEffect.OpenDeck(today.minusDays(3)), viewModel.uiEffects.first())
        }

    @Test
    fun `opens the overview of the chosen day`() =
        runTest {
            val viewModel = homeViewModel(FakeMediaSource())

            viewModel.onEvent(HomeEvent.OpenOverview(today))

            assertEquals(HomeEffect.OpenOverview(today), viewModel.uiEffects.first())
        }

    @Test
    fun `reopens a sorted day, brings its count back and opens its deck`() =
        runTest {
            repository.save(aReviewEntry(mediaId = 1, reviewYear = 2026, decision = ReviewDecision.KEPT))
            val viewModel = homeViewModel(FakeMediaSource(listOf(takenOn(1, 2025))))
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
            assertEquals(0, (loaded(viewModel) as HomeUiState.Loaded).today.remainingCount)

            viewModel.onEvent(HomeEvent.ResortDay(today))

            assertEquals(HomeEffect.OpenDeck(today), viewModel.uiEffects.first())
            assertEquals(1, (viewModel.uiState.value as HomeUiState.Loaded).today.remainingCount)
        }

    @Test
    fun `lists the six previous days with their own count`() =
        runTest {
            val yesterday2024 =
                aMediaItem(
                    id = 9,
                    takenAt =
                        LocalDateTime
                            .of(
                                today.minusDays(1).withYear(2024),
                                NOON,
                            ).atZone(FixedClock.PARIS)
                            .toInstant(),
                )
            val viewModel = homeViewModel(FakeMediaSource(listOf(takenOn(1, 2025), yesterday2024)))

            val state = loaded(viewModel) as HomeUiState.Loaded

            assertEquals((1L..6L).map { today.minusDays(it) }, state.previousDays.map { it.date })
            assertEquals(listOf(1, 0, 0, 0, 0, 0), state.previousDays.map { it.remainingCount })
        }

    @Test
    fun `opens the settings`() =
        runTest {
            val viewModel = homeViewModel(FakeMediaSource())

            viewModel.onEvent(HomeEvent.OpenSettings)

            assertEquals(HomeEffect.OpenSettings, viewModel.uiEffects.first())
        }

    @Test
    fun `opens the progress page`() =
        runTest {
            val viewModel = homeViewModel(FakeMediaSource())

            viewModel.onEvent(HomeEvent.OpenProgress)

            assertEquals(HomeEffect.OpenProgress, viewModel.uiEffects.first())
        }

    @Test
    fun `offers media management once a system confirmation went through`() =
        runTest {
            settings.saveManageMediaTip(ManageMediaTip.OFFERED)

            val state = loaded(homeViewModel(FakeMediaSource())) as HomeUiState.Loaded

            assertEquals(true, state.showManageMediaTip)
        }

    @Test
    fun `does not offer media management before any system confirmation`() =
        runTest {
            val state = loaded(homeViewModel(FakeMediaSource())) as HomeUiState.Loaded

            assertEquals(false, state.showManageMediaTip)
        }

    @Test
    fun `hides the media management tip once access is granted, on return from the settings`() =
        runTest {
            settings.saveManageMediaTip(ManageMediaTip.OFFERED)
            val viewModel = homeViewModel(FakeMediaSource())
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

            mediaManagementGranted = true
            viewModel.onEvent(HomeEvent.Resumed)

            assertEquals(false, (viewModel.uiState.value as HomeUiState.Loaded).showManageMediaTip)
        }

    @Test
    fun `opens the media management setting and closes the tip for good`() =
        runTest {
            settings.saveManageMediaTip(ManageMediaTip.OFFERED)
            val viewModel = homeViewModel(FakeMediaSource())
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

            viewModel.onEvent(HomeEvent.OpenManageMediaSettings)
            assertEquals(HomeEffect.OpenManageMediaSettings, viewModel.uiEffects.first())
            viewModel.onEvent(HomeEvent.DismissManageMediaTip)

            assertEquals(ManageMediaTip.DISMISSED, settings.manageMediaTip)
            assertEquals(false, (viewModel.uiState.value as HomeUiState.Loaded).showManageMediaTip)
        }

    /** État chargé avec [count] médias à trier sur [memories] souvenirs aujourd'hui, et aucun les jours précédents. */
    private fun loadedWithToday(
        count: Int,
        memories: Int = count,
    ): HomeUiState =
        HomeUiState.Loaded(
            today = ReviewDay(today, remainingCount = count, memoryCount = memories),
            previousDays = (1L..6L).map { ReviewDay(today.minusDays(it), remainingCount = 0) },
        )

    /** État sans les vignettes ni les années des jours, comparé aux nombres seulement. */
    private fun HomeUiState.withoutPreviews(): HomeUiState =
        if (this is HomeUiState.Loaded) {
            copy(
                today = today.copy(previews = emptyList(), years = null),
                previousDays = previousDays.map { it.copy(previews = emptyList(), years = null) },
            )
        } else {
            this
        }

    @Test
    fun `shows a preview of today's memories and their years`() =
        runTest {
            val viewModel = homeViewModel(FakeMediaSource(listOf(takenOn(1, 2025), takenOn(2, 2019))))

            val state = loaded(viewModel) as HomeUiState.Loaded

            assertEquals(listOf(MediaId(1), MediaId(2)), state.today.previews.map { it.id })
            assertEquals(2019..2025, state.today.years)
        }

    /** Pile d'aujourd'hui entièrement triée, deux jours triés déjà comptés avant aujourd'hui. */
    private suspend fun sortedTodayAfterTwoDays(): HomeViewModel {
        settings =
            FakeSettingsRepository(
                ratingInvitation = RatingInvitation(sortedDays = 2, lastSortedDay = today.minusDays(1)),
            )
        repository.save(aReviewEntry(mediaId = 1, reviewYear = 2026))
        return homeViewModel(FakeMediaSource(listOf(takenOn(1, 2025))))
    }

    @Test
    fun `invites to rate the app once today is sorted on the third different day`() =
        runTest {
            val viewModel = sortedTodayAfterTwoDays()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

            val state = viewModel.uiState.first { it is HomeUiState.Loaded && it.showRatingInvitation }

            assertTrue((state as HomeUiState.Loaded).showRatingInvitation)
            assertEquals(3, settings.ratingInvitation.sortedDays)
        }

    @Test
    fun `shows the news missed since the last update, then never again once closed`() =
        runTest {
            settings =
                FakeSettingsRepository(tutorialVersionSeen = Tutorial.CURRENT_VERSION, whatsNewVersionSeen = "1.3.0")
            val viewModel = homeViewModel(FakeMediaSource(listOf(takenOn(1, 2025))))
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

            val state = viewModel.uiState.first { it is HomeUiState.Loaded && it.whatsNew.isNotEmpty() }
            assertEquals(WHATS_NEW, (state as HomeUiState.Loaded).whatsNew)

            viewModel.onEvent(HomeEvent.DismissWhatsNew)

            assertEquals(emptyList<WhatsNewRelease>(), (viewModel.uiState.value as HomeUiState.Loaded).whatsNew)
            assertEquals("1.4.0", settings.whatsNewVersionSeen)
            val reopened = homeViewModel(FakeMediaSource(listOf(takenOn(1, 2025))))
            assertEquals(emptyList<WhatsNewRelease>(), (loaded(reopened) as HomeUiState.Loaded).whatsNew)
        }

    @Test
    fun `shows no news on the first install`() =
        runTest {
            val viewModel = homeViewModel(FakeMediaSource(listOf(takenOn(1, 2025))))
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

            assertEquals(emptyList<WhatsNewRelease>(), (loaded(viewModel) as HomeUiState.Loaded).whatsNew)
            assertEquals("1.4.0", settings.whatsNewVersionSeen)
        }

    @Test
    fun `shows the news only after the rating invitation, never with it`() =
        runTest {
            settings =
                FakeSettingsRepository(
                    tutorialVersionSeen = Tutorial.CURRENT_VERSION,
                    whatsNewVersionSeen = "1.3.0",
                    ratingInvitation = RatingInvitation(sortedDays = 2, lastSortedDay = today.minusDays(1)),
                )
            repository.save(aReviewEntry(mediaId = 1, reviewYear = 2026))
            val viewModel = homeViewModel(FakeMediaSource(listOf(takenOn(1, 2025))))
            var dismissed = false
            var newsTooEarly = false
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect {
                    if (!dismissed && it is HomeUiState.Loaded &&
                        it.whatsNew.isNotEmpty()
                    ) {
                        newsTooEarly = true
                    }
                }
            }

            val invited = viewModel.uiState.first { it is HomeUiState.Loaded && it.showRatingInvitation }
            assertEquals(emptyList<WhatsNewRelease>(), (invited as HomeUiState.Loaded).whatsNew)

            dismissed = true
            viewModel.onEvent(HomeEvent.DismissRatingInvitation)

            assertEquals(WHATS_NEW, (viewModel.uiState.value as HomeUiState.Loaded).whatsNew)
            // Rien des nouveautés avant que l'invitation soit fermée : ni avant elle, ni avec elle.
            assertFalse(newsTooEarly)
        }

    @Test
    fun `counts the streak of days, today sorted included, and saves it`() =
        runTest {
            repository.save(aReviewEntry(mediaId = 1, reviewYear = 2026))
            val viewModel = homeViewModel(FakeMediaSource(listOf(takenOn(1, 2025))))
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

            val state = viewModel.uiState.first { it is HomeUiState.Loaded && it.streak > 0 } as HomeUiState.Loaded

            // Aujourd'hui trié, les six jours précédents sans souvenir : un seul jour de série.
            assertEquals(1, state.streak)
            assertEquals(1, settings.streakState.streak.best)
        }

    @Test
    fun `has no streak while today is left to sort and nothing else was sorted`() =
        runTest {
            val viewModel = homeViewModel(FakeMediaSource(listOf(takenOn(1, 2025))))

            val state = loaded(viewModel) as HomeUiState.Loaded

            assertEquals(0, state.streak)
        }

    @Test
    fun `does not count today while memories are left to sort`() =
        runTest {
            settings =
                FakeSettingsRepository(
                    ratingInvitation = RatingInvitation(sortedDays = 2, lastSortedDay = today.minusDays(1)),
                )
            val viewModel = homeViewModel(FakeMediaSource(listOf(takenOn(1, 2025))))

            val state = loaded(viewModel) as HomeUiState.Loaded

            assertFalse(state.showRatingInvitation)
            assertEquals(2, settings.ratingInvitation.sortedDays)
        }

    @Test
    fun `opens the Play Store from the invitation, which never comes back`() =
        runTest {
            val viewModel = sortedTodayAfterTwoDays()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
            viewModel.uiState.first { it is HomeUiState.Loaded && it.showRatingInvitation }

            viewModel.onEvent(HomeEvent.RateApp)

            assertEquals(HomeEffect.OpenRating, viewModel.uiEffects.first())
            assertFalse((viewModel.uiState.value as HomeUiState.Loaded).showRatingInvitation)
            assertTrue(settings.ratingInvitation.asked)
        }

    @Test
    fun `opens the donation page from the invitation`() =
        runTest {
            val viewModel = sortedTodayAfterTwoDays()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
            viewModel.uiState.first { it is HomeUiState.Loaded && it.showRatingInvitation }

            viewModel.onEvent(HomeEvent.SupportDeveloper)

            assertEquals(HomeEffect.OpenDonation, viewModel.uiEffects.first())
            assertTrue(settings.ratingInvitation.asked)
        }

    @Test
    fun `closes the invitation for good when dismissed`() =
        runTest {
            val viewModel = sortedTodayAfterTwoDays()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
            viewModel.uiState.first { it is HomeUiState.Loaded && it.showRatingInvitation }

            viewModel.onEvent(HomeEvent.DismissRatingInvitation)

            assertFalse((viewModel.uiState.value as HomeUiState.Loaded).showRatingInvitation)
            assertTrue(settings.ratingInvitation.asked)
        }

    private var settings = FakeSettingsRepository()

    /** Demandes de mise à jour du widget « Ce jour-là » (PS-170). */
    private var widgetRefreshes = 0

    /** Accès « Gestion des médias » simulé, refusé par défaut. */
    private var mediaManagementGranted = false

    private fun homeViewModel(source: MediaSource): HomeViewModel =
        HomeViewModel(
            GetReviewDays(ReviewWindowProvider(clock), SameDayMatcher(), source, repository, history, clock),
            GetSessionSummary(repository, FakeDecisionJournal(repository), history, ReviewWindowProvider(clock), clock),
            settings,
            mediaManagement = { mediaManagementGranted },
            reopenDay = ReopenDay(SameDayMatcher(), source, repository, FakeDecisionJournal(repository), clock),
            announcements = homeAnnouncements(settings, clock, widgetRefresher = { widgetRefreshes++ }),
        )

    private suspend fun loaded(viewModel: HomeViewModel): HomeUiState =
        viewModel.uiState.first { it is HomeUiState.Loaded }

    private fun takenOn(
        id: Long,
        year: Int,
    ): MediaItem =
        aMediaItem(id = id, takenAt = LocalDateTime.of(today.withYear(year), NOON).atZone(FixedClock.PARIS).toInstant())

    /** Galerie qui échoue à la première lecture, comme un MediaStore indisponible un instant. */
    private class FailingOnceMediaSource(
        media: List<MediaItem>,
    ) : MediaSource {
        private val delegate = FakeMediaSource(media)
        private var failed = false

        override suspend fun mediaTakenIn(ranges: List<OpenEndRange<Instant>>): List<MediaItem> {
            if (!failed) {
                failed = true
                throw IOException("MediaStore unavailable")
            }
            return delegate.mediaTakenIn(ranges)
        }

        override suspend fun mediaByIds(ids: Set<MediaId>): List<MediaItem> = delegate.mediaByIds(ids)
    }

    @Test
    fun `keeps showing the counts while they are recomputed on return`() =
        runTest {
            val viewModel = homeViewModel(FakeMediaSource(listOf(takenOn(1, 2025))))
            val states = mutableListOf<HomeUiState>()
            backgroundScope.launch(
                UnconfinedTestDispatcher(testScheduler),
            ) { viewModel.uiState.collect { states += it } }

            viewModel.onEvent(HomeEvent.Resumed)

            assertEquals(listOf(HomeUiState.Loading, loadedWithToday(1)), states.map { it.withoutPreviews() })
        }

    @Test
    fun `counts the deletions waiting for confirmation, whatever their review year`() =
        runTest {
            repository.save(aReviewEntry(mediaId = 1, reviewYear = 2026, decision = MARKED_FOR_DELETION))
            repository.save(aReviewEntry(mediaId = 2, reviewYear = 2025, decision = MARKED_FOR_DELETION))
            repository.save(aReviewEntry(mediaId = 3, reviewYear = 2026))
            val viewModel = homeViewModel(FakeMediaSource())

            assertEquals(2, (loaded(viewModel) as HomeUiState.Loaded).pendingDeletionCount)
        }

    @Test
    fun `refreshes the pending deletions when coming back to home`() =
        runTest {
            val viewModel = homeViewModel(FakeMediaSource())
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
            repository.save(aReviewEntry(mediaId = 1, decision = MARKED_FOR_DELETION))

            viewModel.onEvent(HomeEvent.Resumed)

            assertEquals(1, (viewModel.uiState.value as HomeUiState.Loaded).pendingDeletionCount)
        }

    @Test
    fun `opens the summary from the pending deletions banner`() =
        runTest {
            val viewModel = homeViewModel(FakeMediaSource())

            viewModel.onEvent(HomeEvent.OpenPendingDeletions)

            assertEquals(HomeEffect.OpenSummary, viewModel.uiEffects.first())
        }

    @Test
    fun `tells memories already sorted apart from days without memories`() =
        runTest {
            repository.save(aReviewEntry(mediaId = 1, reviewYear = 2026))
            val viewModel = homeViewModel(FakeMediaSource(listOf(takenOn(1, 2025))))

            val state = loaded(viewModel) as HomeUiState.Loaded

            assertEquals(
                ReviewDay(today, remainingCount = 0, memoryCount = 1),
                state.today.copy(previews = emptyList(), years = null),
            )
            assertEquals(true, state.hasMemories)
        }

    @Test
    fun `reports no memories for an empty week`() =
        runTest {
            val state = loaded(homeViewModel(FakeMediaSource())) as HomeUiState.Loaded

            assertEquals(false, state.hasMemories)
        }
}
