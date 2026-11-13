package fr.babeuloula.photoswipe.ui.deck

import android.app.PendingIntent
import android.content.Intent
import android.content.IntentSender
import androidx.lifecycle.SavedStateHandle
import fr.babeuloula.photoswipe.MainDispatcherRule
import fr.babeuloula.photoswipe.domain.fixtures.FakeDecisionJournal
import fr.babeuloula.photoswipe.domain.fixtures.FakeMediaActionRequestFactory
import fr.babeuloula.photoswipe.domain.fixtures.FakeMediaSource
import fr.babeuloula.photoswipe.domain.fixtures.FakeMotionPhotoReader
import fr.babeuloula.photoswipe.domain.fixtures.FakePhotoAnalyzer
import fr.babeuloula.photoswipe.domain.fixtures.FakePhotoMeasureCache
import fr.babeuloula.photoswipe.domain.fixtures.FakeReviewRepository
import fr.babeuloula.photoswipe.domain.fixtures.FakeSettingsRepository
import fr.babeuloula.photoswipe.domain.fixtures.FakeShareTargetRepository
import fr.babeuloula.photoswipe.domain.fixtures.FixedClock
import fr.babeuloula.photoswipe.domain.fixtures.aMediaItem
import fr.babeuloula.photoswipe.domain.fixtures.aReviewEntry
import fr.babeuloula.photoswipe.domain.model.DecisionRecord
import fr.babeuloula.photoswipe.domain.model.ManageMediaTip
import fr.babeuloula.photoswipe.domain.model.MediaId
import fr.babeuloula.photoswipe.domain.model.MediaItem
import fr.babeuloula.photoswipe.domain.model.ReviewDecision
import fr.babeuloula.photoswipe.domain.model.ReviewEntry
import fr.babeuloula.photoswipe.domain.port.DecisionJournal
import fr.babeuloula.photoswipe.domain.port.MediaSource
import fr.babeuloula.photoswipe.domain.service.RecentYearsFirstOrdering
import fr.babeuloula.photoswipe.domain.service.ReviewWindowProvider
import fr.babeuloula.photoswipe.domain.service.SameDayMatcher
import fr.babeuloula.photoswipe.domain.usecase.DropPendingFavorite
import fr.babeuloula.photoswipe.domain.usecase.FavoriteMedia
import fr.babeuloula.photoswipe.domain.usecase.GetDaySession
import fr.babeuloula.photoswipe.domain.usecase.GetDeckForDay
import fr.babeuloula.photoswipe.domain.usecase.GetMarkedDeck
import fr.babeuloula.photoswipe.domain.usecase.GetMotionPhotos
import fr.babeuloula.photoswipe.domain.usecase.GetPendingFavorites
import fr.babeuloula.photoswipe.domain.usecase.GetPreviousDecisions
import fr.babeuloula.photoswipe.domain.usecase.GetQualityHints
import fr.babeuloula.photoswipe.domain.usecase.GetSimilarMemories
import fr.babeuloula.photoswipe.domain.usecase.KeepMedia
import fr.babeuloula.photoswipe.domain.usecase.MarkForDeletion
import fr.babeuloula.photoswipe.domain.usecase.OfferManageMediaTip
import fr.babeuloula.photoswipe.domain.usecase.RecordFavoriteResult
import fr.babeuloula.photoswipe.domain.usecase.RequestFavorite
import fr.babeuloula.photoswipe.domain.usecase.RequestUnfavorite
import fr.babeuloula.photoswipe.domain.usecase.ReviewMarkedMedia
import fr.babeuloula.photoswipe.domain.usecase.UndoLastDecision
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
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
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime.NOON

/** Pile construite sur les fakes du domaine, partagée par les tests du `DeckViewModel`. */
abstract class DeckViewModelTestBase {
    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    /** Pile rattrapée : le 14 mai, consulté le 16. */
    protected val day = LocalDate.of(2026, 5, 14)
    protected val clock = FixedClock.at(LocalDate.of(2026, 5, 16).atTime(10, 0))
    protected val repository = FakeReviewRepository()

    /** Journal des décisions, partagé par les ViewModels d'un test comme la base de l'appareil. */
    protected val journal = FakeDecisionJournal(repository)

    protected fun takenOn(
        id: Long,
        year: Int,
    ): MediaItem =
        aMediaItem(
            id = id,
            takenAt =
                day
                    .withYear(year)
                    .atTime(NOON)
                    .atZone(clock.zone)
                    .toInstant(),
        )

    protected fun deckViewModel(
        source: MediaSource,
        journal: DecisionJournal = this.journal,
        route: Map<String, Any?> = mapOf("epochDay" to day.toEpochDay()),
    ): DeckViewModel =
        DeckViewModel(
            savedStateHandle = SavedStateHandle(route),
            loading =
                DeckLoading(
                    getDeckForDay =
                        GetDeckForDay(
                            SameDayMatcher(),
                            source,
                            repository,
                            RecentYearsFirstOrdering(),
                            clock,
                        ),
                    getDaySession = GetDaySession(journal, repository, source, ReviewWindowProvider(clock)),
                    getPreviousDecisions = GetPreviousDecisions(repository),
                    getMarkedDeck = GetMarkedDeck(repository, source, SameDayMatcher(), clock),
                    getSimilarMemories = GetSimilarMemories(SameDayMatcher(), source, clock),
                    hints =
                        CardHints(
                            GetQualityHints(photoAnalyzer, FakePhotoMeasureCache(), settings),
                            GetMotionPhotos(motionPhotoReader),
                        ),
                ),
            decisions =
                DeckDecisions(
                    keepMedia = KeepMedia(repository, journal, clock),
                    markForDeletion = MarkForDeletion(repository, journal, clock),
                    favoriteMedia = FavoriteMedia(repository, journal, clock),
                    undoLastDecision = UndoLastDecision(repository, journal),
                    reviewMarkedMedia =
                        ReviewMarkedMedia(
                            KeepMedia(repository, journal, clock),
                            MarkForDeletion(repository, journal, clock),
                            FavoriteMedia(repository, journal, clock),
                        ),
                ),
            clock = clock,
            galleryFavorite =
                GalleryFavorite(
                    RequestFavorite(FakeMediaActionRequestFactory()),
                    RecordFavoriteResult(repository),
                    OfferManageMediaTip(settings),
                    mediaActionIntents = { intentSender },
                    mediaManagement = { mediaManagementGranted },
                    FavoriteRemoval(
                        GetPendingFavorites(repository),
                        RequestUnfavorite(FakeMediaActionRequestFactory()),
                        DropPendingFavorite(repository),
                    ),
                ),
            shareTargets = shareTargets,
        )

    protected val settings = FakeSettingsRepository()

    /** Vignettes des photos à mesurer (PS-164) : aucune par défaut, rien n'est signalé. */
    protected var photoAnalyzer = FakePhotoAnalyzer()

    /** Photos animées (PS-167) : aucune par défaut. */
    protected var motionPhotoReader = FakeMotionPhotoReader()

    /** Dernière application choisie pour partager (PS-163), aucune par défaut. */
    protected val shareTargets = FakeShareTargetRepository()

    /** Accès « Gestion des médias » simulé, refusé par défaut. */
    protected var mediaManagementGranted = false

    /** Demande système factice : un vrai `IntentSender`, jamais lancé ici. */
    protected val intentSender: IntentSender =
        PendingIntent
            .getActivity(RuntimeEnvironment.getApplication(), 0, Intent(), PendingIntent.FLAG_IMMUTABLE)
            .intentSender
}
