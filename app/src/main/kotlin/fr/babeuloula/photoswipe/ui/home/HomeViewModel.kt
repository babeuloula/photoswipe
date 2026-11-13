package fr.babeuloula.photoswipe.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.babeuloula.photoswipe.domain.model.ManageMediaTip
import fr.babeuloula.photoswipe.domain.model.ReviewDay
import fr.babeuloula.photoswipe.domain.model.ReviewSession
import fr.babeuloula.photoswipe.domain.port.SettingsRepository
import fr.babeuloula.photoswipe.domain.usecase.GetReviewDays
import fr.babeuloula.photoswipe.domain.usecase.GetSessionSummary
import fr.babeuloula.photoswipe.domain.usecase.ReopenDay
import fr.babeuloula.photoswipe.permission.MediaManagementReader
import fr.babeuloula.photoswipe.ui.whatsnew.WhatsNewRelease
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/** État de l'accueil (SPEC §4.2). */
sealed interface HomeUiState {
    /** Premier calcul en cours. */
    data object Loading : HomeUiState

    /** La galerie ou la base n'a pas pu être lue. */
    data object Error : HomeUiState

    /**
     * Jours de la fenêtre calculés.
     *
     * @property today aujourd'hui et son nombre de médias à trier.
     * @property previousDays les jours précédents de la fenêtre de rattrapage (6, 13 ou 29, PS-169), du
     * plus récent au plus ancien, tous, y compris ceux sans souvenir (la série de jours les compte).
     * @property pendingDeletionCount médias marqués pour suppression et pas encore confirmés, toutes
     * sessions confondues (SPEC §3.4) ; 0 masque la bannière.
     * @property showManageMediaTip vrai pour proposer l'accès « Gestion des médias » : un dialogue
     * d'Android a déjà été validé, le conseil n'a pas été fermé et l'accès n'est pas donné (ADR 0023).
     * @property showRatingInvitation vrai pour inviter, une seule fois, à noter l'application et à
     * soutenir son développeur, au troisième jour où la pile du jour est entièrement triée (PS-121).
     * @property streak série de jours en cours (PS-155), rappelée sur le bouton « Progression ».
     * @property whatsNew nouveautés des versions manquées depuis la dernière mise à jour (PS-173),
     * de la plus récente à la plus ancienne ; vide pour ne pas afficher la feuille. Jamais en même
     * temps que l'invitation à noter l'application, qui passe avant.
     */
    data class Loaded(
        val today: ReviewDay,
        val previousDays: List<ReviewDay>,
        val pendingDeletionCount: Int = 0,
        val showManageMediaTip: Boolean = false,
        val showRatingInvitation: Boolean = false,
        val streak: Int = 0,
        val whatsNew: List<WhatsNewRelease> = emptyList(),
    ) : HomeUiState {
        /**
         * Vrai si au moins un souvenir existe sur la fenêtre, trié ou non, ou si un jour a été vidé
         * par des suppressions confirmées (SPEC §4, états vides ; ADR 0034).
         */
        val hasMemories: Boolean
            get() = (listOf(today) + previousDays).any { it.memoryCount > 0 || it.deletedCount > 0 }

        /**
         * Jours listés sous « À rattraper » (PS-169) : les 6 premiers toujours, pour montrer qu'on n'a
         * rien manqué ; au-delà, seulement ceux qui ont eu des souvenirs ou des suppressions.
         */
        val listedPreviousDays: List<ReviewDay>
            get() =
                previousDays.filterIndexed { index, day ->
                    index < ALWAYS_LISTED_PREVIOUS_DAYS || day.memoryCount > 0 || day.deletedCount > 0
                }

        private companion object {
            /** Jours précédents toujours listés, même sans souvenir : la semaine d'aujourd'hui. */
            const val ALWAYS_LISTED_PREVIOUS_DAYS = 6
        }
    }
}

/** Action de l'utilisateur ou du système sur l'accueil. */
sealed interface HomeEvent {
    /**
     * Tap sur la carte « Aujourd'hui » ou sur un jour à rattraper.
     *
     * @property date jour dont la pile s'ouvre.
     */
    data class OpenDay(
        val date: LocalDate,
    ) : HomeEvent

    /**
     * « Voir tout le jour » : la vue d'ensemble de ses souvenirs (PS-139).
     *
     * @property date jour à montrer.
     */
    data class OpenOverview(
        val date: LocalDate,
    ) : HomeEvent

    /**
     * « Retrier » un jour déjà trié (ADR 0026) : ses médias reviennent à trier, puis sa pile s'ouvre.
     *
     * @property date jour à retrier.
     */
    data class ResortDay(
        val date: LocalDate,
    ) : HomeEvent

    /** « Réessayer » après une erreur. */
    data object Retry : HomeEvent

    /** Retour au premier plan : la galerie ou le jour ont pu changer (ADR 0008). */
    data object Resumed : HomeEvent

    /** Bouton des réglages. */
    data object OpenSettings : HomeEvent

    /** Bouton « Progression » de la barre du haut (PS-153). */
    data object OpenProgress : HomeEvent

    /** Bouton « Corbeille » de la barre du haut : les suppressions encore restaurables (PS-168). */
    data object OpenTrash : HomeEvent

    /** Tap sur la bannière des suppressions en attente. */
    data object OpenPendingDeletions : HomeEvent

    /** « paramètres » du conseil : ouvrir le réglage « Gestion des médias » d'Android. */
    data object OpenManageMediaSettings : HomeEvent

    /** Croix du conseil « Gestion des médias » : ne plus le proposer. */
    data object DismissManageMediaTip : HomeEvent

    /** Réponse à ce que l'accueil annonce de lui-même : invitation à noter l'application, nouveautés. */
    sealed interface NoticeEvent : HomeEvent

    /** « Noter l'application » de l'invitation (PS-121). */
    data object RateApp : NoticeEvent

    /** « Soutenir » de l'invitation : la page de don (PS-121). */
    data object SupportDeveloper : NoticeEvent

    /** Invitation fermée sans choisir : elle ne reviendra pas. */
    data object DismissRatingInvitation : NoticeEvent

    /** Feuille des nouveautés fermée : elle ne reviendra pas avant la prochaine mise à jour (PS-173). */
    data object DismissWhatsNew : NoticeEvent
}

/** Navigation demandée par l'accueil. */
sealed interface HomeEffect {
    /** Ouvrir la pile du jour [date]. */
    data class OpenDeck(
        val date: LocalDate,
    ) : HomeEffect

    /** Ouvrir la vue d'ensemble du jour [date] (PS-139). */
    data class OpenOverview(
        val date: LocalDate,
    ) : HomeEffect

    /** Ouvrir les réglages. */
    data object OpenSettings : HomeEffect

    /** Ouvrir la page « Progression » (PS-153). */
    data object OpenProgress : HomeEffect

    /** Ouvrir la page « Corbeille » (PS-168). */
    data object OpenTrash : HomeEffect

    /** Ouvrir le résumé, pour confirmer les suppressions en attente (SPEC §3.5). */
    data object OpenSummary : HomeEffect

    /** Ouvrir le réglage « Gestion des médias » d'Android pour PhotoSwipe. */
    data object OpenManageMediaSettings : HomeEffect

    /** Ouvrir la fiche de l'application sur le Play Store, pour la noter (ADR 0029). */
    data object OpenRating : HomeEffect

    /** Ouvrir la page de don (ADR 0029). */
    data object OpenDonation : HomeEffect
}

/**
 * ViewModel de l'accueil : la carte « Aujourd'hui » et les jours précédents de la fenêtre de rattrapage (PS-169), chacun avec son
 * nombre de médias à trier (SPEC §4.2).
 *
 * Les nombres suivent les décisions en direct (`GetReviewDays` est un flux). Ils sont recalculés
 * depuis le début à chaque retour au premier plan et après « Réessayer » : la galerie, le jour ou
 * les permissions ont pu changer entre-temps.
 *
 * Après une mise à jour, il annonce les nouveautés des versions manquées (PS-173), une fois
 * l'invitation à noter l'application passée.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel
    @Inject
    constructor(
        private val getReviewDays: GetReviewDays,
        private val getSessionSummary: GetSessionSummary,
        private val settingsRepository: SettingsRepository,
        private val mediaManagement: MediaManagementReader,
        private val reopenDay: ReopenDay,
        private val announcements: HomeAnnouncements,
    ) : ViewModel() {
        private val sortedDay = announcements.sortedDay

        /**
         * Invitation à noter l'application (PS-121) et nouveautés (PS-173), dans un seul état pour
         * que l'une passe toujours avant les autres, sans état intermédiaire.
         */
        private val notices = MutableStateFlow(HomeNotices())

        /** Série de jours en cours (PS-155), recomptée à chaque lecture des jours. */
        private val streak = MutableStateFlow(0)

        /** Derniers jours passés au calcul de la série, pour ne pas le refaire sans changement. */
        private var streakDays: List<ReviewDay>? = null

        /** Dernier jour dont le tri complet a été compté, pour ne le compter qu'une fois par process. */
        private var countedDay: LocalDate? = null

        /** Demandes de calcul ; la valeur dit s'il faut repasser par l'état de chargement. */
        private val reloads = MutableSharedFlow<Boolean>(replay = 1).apply { tryEmit(false) }
        private val effects = Channel<HomeEffect>(Channel.BUFFERED)

        /** État de l'accueil. */
        val uiState: StateFlow<HomeUiState> =
            reloads
                .flatMapLatest { showLoading -> loadDays(showLoading) }
                .onEach { state ->
                    if (state is HomeUiState.Loaded) {
                        // Le widget « Ce jour-là » suit les tris, relus à chaque retour à l'accueil (PS-170).
                        sortedDay.widgetRefresher.refresh()
                        countTodaySorted(state.today)
                        countStreak(listOf(state.today) + state.previousDays)
                    }
                }.combine(notices) { state, shown ->
                    if (state is HomeUiState.Loaded) {
                        state.copy(showRatingInvitation = shown.ratingInvitation, whatsNew = shown.whatsNewShown)
                    } else {
                        state
                    }
                }.combine(streak) { state, days ->
                    if (state is HomeUiState.Loaded) state.copy(streak = days) else state
                }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), HomeUiState.Loading)

        /** Navigations à exécuter, chacune livrée une seule fois. */
        val uiEffects: Flow<HomeEffect> = effects.receiveAsFlow()

        init {
            // Un échec de lecture des réglages n'annonce rien.
            viewModelScope.launch {
                val missed = runCatching { announcements.whatsNew.missed() }.getOrDefault(emptyList())
                notices.update { it.copy(whatsNew = missed) }
            }
        }

        /** Traite une action de l'utilisateur ou du système. */
        fun onEvent(event: HomeEvent) {
            when (event) {
                is HomeEvent.OpenDay -> {
                    effects.trySend(HomeEffect.OpenDeck(event.date))
                }

                is HomeEvent.OpenOverview -> {
                    effects.trySend(HomeEffect.OpenOverview(event.date))
                }

                is HomeEvent.ResortDay -> {
                    viewModelScope.launch {
                        runCatching { reopenDay(event.date) }.onSuccess {
                            effects.trySend(HomeEffect.OpenDeck(event.date))
                        }
                    }
                }

                HomeEvent.Retry -> {
                    reloads.tryEmit(true)
                }

                HomeEvent.Resumed -> {
                    reloads.tryEmit(false)
                }

                HomeEvent.OpenSettings -> {
                    effects.trySend(HomeEffect.OpenSettings)
                }

                HomeEvent.OpenProgress -> {
                    effects.trySend(HomeEffect.OpenProgress)
                }

                HomeEvent.OpenTrash -> {
                    effects.trySend(HomeEffect.OpenTrash)
                }

                HomeEvent.OpenPendingDeletions -> {
                    effects.trySend(HomeEffect.OpenSummary)
                }

                HomeEvent.OpenManageMediaSettings -> {
                    effects.trySend(HomeEffect.OpenManageMediaSettings)
                }

                is HomeEvent.NoticeEvent -> {
                    onNoticeEvent(event)
                }

                HomeEvent.DismissManageMediaTip -> {
                    viewModelScope.launch {
                        runCatching { settingsRepository.saveManageMediaTip(ManageMediaTip.DISMISSED) }
                    }
                }
            }
        }

        /**
         * Calcule les jours. Après « Réessayer » ([showLoading]), l'écran repasse par le chargement ;
         * au retour au premier plan, il garde l'état affiché jusqu'au nouveau résultat, sans clignoter.
         * Le nombre de suppressions en attente est lu à chaque calcul : les marquages se font dans la
         * pile, et l'accueil se recalcule en y revenant.
         */
        private fun loadDays(showLoading: Boolean): Flow<HomeUiState> {
            val loaded: Flow<HomeUiState> =
                combine(
                    getReviewDays(),
                    flow { emit(pendingDeletionCount()) },
                    manageMediaTipShown(),
                ) { days, pending, tip ->
                    HomeUiState.Loaded(
                        today = days.first(),
                        previousDays = days.drop(1),
                        pendingDeletionCount = pending,
                        showManageMediaTip = tip,
                    )
                }
            return loaded
                .onStart { if (showLoading) emit(HomeUiState.Loading) }
                .catch { emit(HomeUiState.Error) }
        }

        /**
         * Conseil « Gestion des médias » à afficher : proposé, pas fermé, accès pas encore donné.
         * L'accès se relit à chaque calcul, donc au retour des réglages d'Android. Un réglage
         * illisible masque le conseil.
         */
        private fun manageMediaTipShown(): Flow<Boolean> =
            settingsRepository
                .observeManageMediaTip()
                .map { it == ManageMediaTip.OFFERED && !mediaManagement.isGranted() }
                .catch { emit(false) }

        /**
         * Pile d'aujourd'hui entièrement triée : un jour de plus vers l'invitation à noter
         * l'application (PS-121), qui s'affiche au troisième jour différent. Un échec de lecture
         * ou d'écriture des réglages ne montre rien. Une fois la réponse connue, les nouveautés
         * peuvent s'afficher.
         */
        private fun countTodaySorted(today: ReviewDay) {
            // Jour déjà compté : la réponse est connue, ou le sera à la fin du compte en cours.
            if (countedDay == today.date) return
            if (today.memoryCount == 0 || today.remainingCount > 0) {
                notices.update { it.copy(ratingSettled = true) }
                return
            }
            countedDay = today.date
            viewModelScope.launch {
                val invite = runCatching { sortedDay.recordTodaySorted() }.getOrDefault(false)
                notices.update { it.copy(ratingInvitation = it.ratingInvitation || invite, ratingSettled = true) }
            }
        }

        /**
         * Recompte la série de jours (PS-155) quand les jours de la fenêtre ont changé. Un échec de
         * lecture ou d'écriture des réglages garde la série affichée.
         */
        private fun countStreak(days: List<ReviewDay>) {
            if (days == streakDays) return
            streakDays = days
            viewModelScope.launch {
                runCatching {
                    sortedDay.updateStreak(
                        days,
                    )
                }.onSuccess { streak.value = it.current }
            }
        }

        /** Réponse à l'invitation à noter l'application ou à la feuille des nouveautés. */
        private fun onNoticeEvent(event: HomeEvent.NoticeEvent) {
            when (event) {
                HomeEvent.RateApp -> {
                    closeRatingInvitation()
                    effects.trySend(HomeEffect.OpenRating)
                }

                HomeEvent.SupportDeveloper -> {
                    closeRatingInvitation()
                    effects.trySend(HomeEffect.OpenDonation)
                }

                HomeEvent.DismissRatingInvitation -> {
                    closeRatingInvitation()
                }

                HomeEvent.DismissWhatsNew -> {
                    notices.update { it.copy(whatsNew = emptyList()) }
                    viewModelScope.launch { runCatching { announcements.whatsNew.markSeen() } }
                }
            }
        }

        /** Ferme l'invitation pour de bon, quel que soit le choix. */
        private fun closeRatingInvitation() {
            notices.update { it.copy(ratingInvitation = false) }
            viewModelScope.launch { runCatching { sortedDay.markRatingAsked() } }
        }

        /** Marquages à confirmer : le même nombre que le résumé annoncera (ADR 0011). */
        private suspend fun pendingDeletionCount(): Int = getSessionSummary().markedForDeletionCount

        private companion object {
            /** Garde le calcul actif pendant une rotation ou un court passage en arrière-plan. */
            const val STOP_TIMEOUT_MILLIS = 5_000L
        }
    }

/**
 * Ce que l'accueil annonce de lui-même, par-dessus les jours.
 *
 * @property ratingInvitation vrai pour montrer l'invitation à noter l'application (PS-121).
 * @property ratingSettled vrai une fois su si l'invitation est due : les nouveautés attendent
 * jusque-là, pour ne pas s'afficher puis s'effacer devant elle.
 * @property whatsNew nouveautés à annoncer (PS-173), lues une fois à l'ouverture de l'accueil.
 */
private data class HomeNotices(
    val ratingInvitation: Boolean = false,
    val ratingSettled: Boolean = false,
    val whatsNew: List<WhatsNewRelease> = emptyList(),
) {
    /** Nouveautés à afficher : seulement l'invitation connue et fermée, jamais avec elle. */
    val whatsNewShown: List<WhatsNewRelease>
        get() = if (ratingSettled && !ratingInvitation) whatsNew else emptyList()
}
