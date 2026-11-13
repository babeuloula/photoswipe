package fr.babeuloula.photoswipe.ui.deck

import android.content.IntentSender
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.babeuloula.photoswipe.domain.model.MediaId
import fr.babeuloula.photoswipe.domain.model.MediaItem
import fr.babeuloula.photoswipe.domain.model.MediaType
import fr.babeuloula.photoswipe.domain.model.QualityHint
import fr.babeuloula.photoswipe.domain.model.ReviewDecision
import fr.babeuloula.photoswipe.domain.model.ReviewSession
import fr.babeuloula.photoswipe.domain.model.SessionDecision
import fr.babeuloula.photoswipe.domain.model.SimilarGroup
import fr.babeuloula.photoswipe.domain.port.ShareTargetRepository
import fr.babeuloula.photoswipe.domain.usecase.MarkedMedia
import fr.babeuloula.photoswipe.domain.usecase.UndoLastDecision
import fr.babeuloula.photoswipe.ui.navigation.DeckRoute
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

/** État de la pile de swipe (SPEC §4.3). */
sealed interface DeckUiState {
    /** Paquet en cours de chargement. */
    data object Loading : DeckUiState

    /** La galerie ou la base n'a pas pu être lue. */
    data object Error : DeckUiState

    /**
     * Plus rien à trier pour ce jour.
     *
     * @property canUndo vrai si la dernière décision de la session peut être annulée.
     */
    data class Empty(
        val canUndo: Boolean = false,
    ) : DeckUiState

    /**
     * Paquet chargé.
     *
     * @property cards cartes visibles, dans l'ordre de la pile : celle du dessus et, s'il y en a une,
     * la suivante, déjà composée dessous pour que son image soit chargée avant d'arriver (SPEC §14).
     * Jamais vide, jamais plus de [VISIBLE_CARDS] : une pile de plusieurs milliers de médias ne
     * prépare que deux cartes.
     * @property remainingCount médias encore à trier, carte du dessus comprise.
     * @property sortedCount médias de ce jour déjà triés (décisions de la session, reprises du journal).
     * @property canUndo vrai si la dernière décision de la session peut être annulée.
     * @property returningFrom côté d'où revient la carte du dessus après un undo, celui par lequel
     * elle était sortie ; `null` si elle ne revient pas.
     * @property menuOpen vrai quand le menu favori / partage de la carte du dessus est ouvert.
     * @property shareTarget dernière application choisie pour partager (PS-163), `null` avant un
     * premier partage : le menu propose alors « Renvoyer avec … ».
     * @property yearPosition rang de la carte du dessus parmi les souvenirs de son année (PS-138) :
     * la pile va d'une année à l'autre, de la plus récente à la plus ancienne.
     * @property yearTotal souvenirs de l'année de la carte du dessus dans la pile, triés ou non.
     * @property yearSegments une entrée par année de la pile, de la plus récente à la plus
     * ancienne, avec ses souvenirs triés et son total (PS-144).
     */
    data class Loaded(
        val cards: List<MediaCardUiState>,
        val remainingCount: Int = cards.size,
        val sortedCount: Int = 0,
        val canUndo: Boolean = false,
        val returningFrom: CardSide? = null,
        val menuOpen: Boolean = false,
        val shareTarget: String? = null,
        val yearPosition: Int = 1,
        val yearTotal: Int = 1,
        val yearSegments: List<YearSegment> = emptyList(),
    ) : DeckUiState {
        init {
            require(cards.size in 1..VISIBLE_CARDS) { "a loaded deck shows 1 to $VISIBLE_CARDS cards: ${cards.size}" }
        }

        /** Carte du dessus, celle que l'utilisateur trie. */
        val current: MediaCardUiState get() = cards.first()

        /** Carte suivante, préparée sous celle du dessus ; `null` pour la dernière carte. */
        val next: MediaCardUiState? get() = cards.getOrNull(1)

        /** Année de prise de vue de la carte du dessus. */
        val year: Int get() = current.takenOn.year

        /** Rang de la carte du dessus dans la pile du jour, à partir de 1 (« 4 sur 23 »). */
        val position: Int get() = sortedCount + 1

        /** Médias de la pile du jour : triés et à trier. */
        val total: Int get() = sortedCount + remainingCount

        companion object {
            /** Cartes préparées à l'écran : celle du dessus et trois dessous, en pile (PS-147), pas plus. */
            const val VISIBLE_CARDS = 4
        }
    }
}

/** Action de l'utilisateur sur la pile. */
sealed interface DeckEvent {
    /** « Réessayer » après une erreur. */
    data object Retry : DeckEvent

    /** « Terminer » : passer au résumé. */
    data object Finish : DeckEvent

    /** Flèche de retour : revenir à l'accueil. */
    data object Back : DeckEvent

    /** Carte du dessus glissée à droite : la garder (SPEC §2). */
    data object Keep : DeckEvent

    /** Carte du dessus glissée à gauche : la marquer pour suppression, sans rien supprimer (SPEC §2). */
    data object MarkForDeletion : DeckEvent

    /** « Annuler » : remettre sur la pile le dernier média trié (SPEC §3.4). */
    data object Undo : DeckEvent

    /** Carte glissée vers le haut ou bouton « Menu » : ouvrir le menu favori / partage (SPEC §2). */
    data object OpenMenu : DeckEvent

    /** Menu fermé sans choix : la carte reste en place. */
    data object CloseMenu : DeckEvent

    /** Action sur le favori de la carte du dessus : ajout ou retrait, et réponse de la galerie. */
    sealed interface FavoriteEvent : DeckEvent

    /** « Favori » du menu : garder le média et demander aussitôt sa mise en favori à la galerie (SPEC §2). */
    data object Favorite : FavoriteEvent

    /**
     * Réponse à la demande de favori de la galerie (dialogue d'Android, ou accord immédiat avec
     * l'accès « Gestion des médias »).
     *
     * @property confirmed vrai si le favori a été accepté.
     */
    data class FavoriteRequestAnswered(
        val confirmed: Boolean,
    ) : FavoriteEvent

    /**
     * « Retirer des favoris » du menu d'une carte en favori (PS-124) : sans décision de tri, la
     * carte reste en place.
     */
    data object Unfavorite : FavoriteEvent

    /**
     * Réponse à la demande de retrait des favoris de la galerie.
     *
     * @property confirmed vrai si le retrait a été accepté.
     */
    data class UnfavoriteRequestAnswered(
        val confirmed: Boolean,
    ) : FavoriteEvent

    /** Le média du dessus confié à une autre application, sans changer son état. */
    sealed interface ElsewhereEvent : DeckEvent

    /** « Partager » du menu : envoyer le média du dessus ailleurs, sans changer son état (SPEC §2). */
    data object Share : ElsewhereEvent

    /** « Ouvrir avec » du menu : voir ou retoucher le média du dessus dans une autre application (PS-162). */
    data object OpenWith : ElsewhereEvent

    /** « Renvoyer avec … » du menu : partager directement avec la dernière application choisie (PS-163). */
    data object ShareAgain : ElsewhereEvent

    /**
     * « Partager en carte postale » du menu d'une photo (PS-177) : la photo du dessus dans un cadre,
     * avec son repère « Il y a 5 ans », sans changer son état.
     */
    data object SharePostcard : ElsewhereEvent

    /** Retour sur la pile : après « Ouvrir avec », le média a pu être retouché, copié ou supprimé ailleurs. */
    data object Resumed : ElsewhereEvent
}

/** Navigation demandée par la pile. */
sealed interface DeckEffect {
    /** Confier le média du dessus à une autre application : partage, « Ouvrir avec », « Renvoyer avec ». */
    sealed interface ElsewhereEffect : DeckEffect

    /** Ouvrir le résumé (SPEC §3.5), qui lit lui-même le tri du jour (ADR 0024). */
    data object OpenSummary : DeckEffect

    /** Quitter la pile, vers l'écran précédent. */
    data object Close : DeckEffect

    /**
     * Ouvrir la feuille de partage d'Android pour un média.
     *
     * @property uri URI de contenu du média.
     * @property type photo ou vidéo, pour le type MIME annoncé aux autres applications.
     */
    data class Share(
        val uri: String,
        val type: MediaType,
    ) : ElsewhereEffect

    /**
     * Ouvrir le sélecteur d'Android pour voir ou retoucher un média ailleurs (PS-162).
     *
     * @property uri URI de contenu du média.
     * @property type photo ou vidéo, pour le type MIME annoncé aux autres applications.
     */
    data class OpenWith(
        val uri: String,
        val type: MediaType,
    ) : ElsewhereEffect

    /**
     * Partager directement avec l'application [component], sans feuille de partage (PS-163).
     *
     * @property uri URI de contenu du média.
     * @property type photo ou vidéo.
     * @property component application choisie au dernier partage (`paquet/classe`).
     */
    data class ShareWith(
        val uri: String,
        val type: MediaType,
        val component: String,
    ) : ElsewhereEffect

    /**
     * Composer la carte postale d'une photo, la montrer en aperçu puis la partager (PS-177).
     *
     * @property card carte du dessus, dont la photo et le repère « Il y a N ans » font la carte postale.
     */
    data class SharePostcard(
        val card: MediaCardUiState,
    ) : DeckEffect

    /**
     * Lancer la demande de favori de la galerie ; la réponse revient par [DeckEvent.FavoriteRequestAnswered].
     *
     * @property intentSender demande système d'ajout aux favoris.
     */
    data class LaunchFavoriteRequest(
        val intentSender: IntentSender,
    ) : DeckEffect

    /**
     * Lancer la demande de retrait des favoris de la galerie (PS-124) ; la réponse revient par
     * [DeckEvent.UnfavoriteRequestAnswered].
     *
     * @property intentSender demande système de retrait des favoris.
     */
    data class LaunchUnfavoriteRequest(
        val intentSender: IntentSender,
    ) : DeckEffect
}

/**
 * ViewModel de la pile d'un jour : charge une fois le paquet du jour de la route (ADR 0009), puis
 * retire lui-même chaque carte triée et enregistre la décision. Les décisions du jour sont
 * gardées dans une [ReviewSession], reprise du journal au chargement (`GetDaySession`, ADR 0019) :
 * « Annuler » remet la dernière carte triée sur la pile, même après être sorti de la pile ou après
 * la mort du process.
 */
@HiltViewModel
class DeckViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val loading: DeckLoading,
        private val decisions: DeckDecisions,
        private val clock: Clock,
        private val galleryFavorite: GalleryFavorite,
        private val shareTargets: ShareTargetRepository,
    ) : ViewModel() {
        private val route: DeckRoute = savedStateHandle.toRoute<DeckRoute>()

        /** Jour cible de la pile ; `null` pour la pile des médias marqués (PS-112). */
        val day: LocalDate? = route.day

        /** Fin de la pile : le résumé, ou le retour au résumé d'où la pile des marqués a été ouverte. */
        private val doneEffect: DeckEffect = if (route.isMarked) DeckEffect.Close else DeckEffect.OpenSummary

        /** Médias de la pile des marqués, avec les jours de revue de leurs marquages ; vide sinon. */
        private var marked: Map<MediaId, MarkedMedia> = emptyMap()

        private val state = MutableStateFlow<DeckUiState>(DeckUiState.Loading)
        private val effects = Channel<DeckEffect>(Channel.BUFFERED)

        /**
         * Médias encore à trier, dans l'ordre de la pile ; vide tant que le paquet n'est pas chargé.
         * File à double entrée : trier retire le dessus, annuler l'y remet, sans recopier la pile.
         */
        private val deck = ArrayDeque<MediaItem>()

        /**
         * Décisions précédentes des médias d'un jour rouvert pour être retrié (ADR 0026), montrées
         * sur leur carte ; vide pour un jour trié pour la première fois.
         */
        private var previousDecisions: Map<MediaId, ReviewDecision> = emptyMap()

        /** Place des photos du jour prises à la suite (PS-140). */
        private var similar: Map<MediaId, SimilarGroup> = emptyMap()

        /** Photos peut-être floues ou très sombres (PS-164). */
        private var quality: Map<MediaId, QualityHint> = emptyMap()

        /** Décisions du jour encore annulables, de la plus ancienne à la plus récente. */
        var session: ReviewSession = ReviewSession()
            private set

        /** État de la pile. */
        val uiState: StateFlow<DeckUiState> = state.asStateFlow()

        /** Navigations à exécuter, chacune livrée une seule fois. */
        val uiEffects: Flow<DeckEffect> = effects.receiveAsFlow()

        init {
            load()
            readShareTarget()
        }

        /** Traite une action de l'utilisateur. */
        fun onEvent(event: DeckEvent) {
            when (event) {
                DeckEvent.Retry -> {
                    load()
                }

                DeckEvent.Finish -> {
                    effects.trySend(doneEffect)
                }

                DeckEvent.Back -> {
                    effects.trySend(DeckEffect.Close)
                }

                DeckEvent.Keep -> {
                    decideTopCard(decisions.keepMedia::invoke, decisions.reviewMarkedMedia::keep)
                }

                DeckEvent.MarkForDeletion -> {
                    decideTopCard(
                        { media, day -> listOf(decisions.markForDeletion(media, day)) },
                        decisions.reviewMarkedMedia::markForDeletion,
                    )
                }

                is DeckEvent.FavoriteEvent -> {
                    onFavoriteEvent(event)
                }

                DeckEvent.Undo -> {
                    undo()
                }

                DeckEvent.OpenMenu -> {
                    state.setMenuOpen(true)
                }

                DeckEvent.CloseMenu -> {
                    state.setMenuOpen(false)
                }

                is DeckEvent.ElsewhereEvent -> {
                    onElsewhereEvent(event)
                }
            }
        }

        /** Partage, « Ouvrir avec » et retour sur la pile après être passé par une autre application. */
        private fun onElsewhereEvent(event: DeckEvent.ElsewhereEvent) {
            when (event) {
                DeckEvent.Share -> {
                    // Partage du média du dessus, sans changer son état (SPEC §2) ; l'application
                    // choisie sera relue au retour, pour « Renvoyer avec … » (PS-163).
                    deck.firstOrNull()?.let { media ->
                        state.setMenuOpen(false)
                        sharedElsewhere = true
                        effects.trySend(DeckEffect.Share(media.uri, media.type))
                    }
                }

                DeckEvent.ShareAgain -> {
                    val target = shareTarget
                    deck.firstOrNull()?.takeIf { target != null }?.let { media ->
                        state.setMenuOpen(false)
                        effects.trySend(DeckEffect.ShareWith(media.uri, media.type, target!!))
                    }
                }

                DeckEvent.OpenWith -> {
                    // La carte reste en place, sans décision ; la pile se relira au retour (PS-162).
                    deck.firstOrNull()?.let { media ->
                        state.setMenuOpen(false)
                        openedElsewhere = true
                        effects.trySend(DeckEffect.OpenWith(media.uri, media.type))
                    }
                }

                DeckEvent.SharePostcard -> {
                    // Carte postale de la photo du dessus (PS-177) : composée et partagée par l'écran,
                    // la carte reste en place, sans décision.
                    (state.value as? DeckUiState.Loaded)?.current?.takeIf { it.type == MediaType.IMAGE }?.let { card ->
                        state.setMenuOpen(false)
                        effects.trySend(DeckEffect.SharePostcard(card))
                    }
                }

                DeckEvent.Resumed -> {
                    if (sharedElsewhere) {
                        sharedElsewhere = false
                        readShareTarget()
                    }
                    if (openedElsewhere) {
                        openedElsewhere = false
                        load(showLoading = false)
                    }
                }
            }
        }

        /** Ajout ou retrait du favori de la carte du dessus, et réponses de la galerie. */
        private fun onFavoriteEvent(event: DeckEvent.FavoriteEvent) {
            when (event) {
                DeckEvent.Favorite -> {
                    decideTopCard(decisions.favoriteMedia::invoke, decisions.reviewMarkedMedia::favorite) { media ->
                        // Le favori est demandé aussitôt à la galerie (SPEC §2) ; sinon, il reste en attente.
                        galleryFavorite.request(media)?.let { effects.trySend(DeckEffect.LaunchFavoriteRequest(it)) }
                    }
                }

                is DeckEvent.FavoriteRequestAnswered -> {
                    viewModelScope.launch { if (galleryFavorite.answer(event.confirmed)) openSummaryWhenDone() }
                }

                DeckEvent.Unfavorite -> {
                    // Retrait du favori de la carte du dessus (PS-124), sans décision de tri : un favori
                    // seulement en attente est oublié aussitôt, un favori de la galerie passe par sa demande.
                    val media = deck.firstOrNull() ?: return
                    state.setMenuOpen(false)
                    viewModelScope.launch {
                        galleryFavorite
                            .unfavorite(
                                media,
                            )?.let { effects.trySend(DeckEffect.LaunchUnfavoriteRequest(it)) }
                        render()
                    }
                }

                is DeckEvent.UnfavoriteRequestAnswered -> {
                    viewModelScope.launch {
                        galleryFavorite.answerRemoval(event.confirmed)
                        render()
                    }
                }
            }
        }

        /** Vrai après « Ouvrir avec » : la pile se relit au retour, le média ayant pu changer ailleurs. */
        private var openedElsewhere = false

        /** Vrai après un partage : l'application choisie se relit au retour (PS-163). */
        private var sharedElsewhere = false

        /** Dernière application choisie pour partager (PS-163), `null` avant un premier partage. */
        private var shareTarget: String? = null

        /** Relit la dernière application choisie pour partager, puis met l'écran à jour. */
        private fun readShareTarget() {
            viewModelScope.launch {
                shareTarget = runCatching { shareTargets.lastTarget() }.getOrNull()
                if (state.value is DeckUiState.Loaded) render()
            }
        }

        /**
         * Lit la pile ; au retour d'une autre application ([showLoading] faux), l'ancienne reste
         * affichée pendant la lecture, et une copie retouchée apparaît comme un nouveau souvenir.
         */
        private fun load(showLoading: Boolean = true) {
            if (showLoading) state.value = DeckUiState.Loading
            viewModelScope.launch {
                runCatching {
                    galleryFavorite.loadPending()
                    loading.load(route)
                }.fold(
                    onSuccess = { loaded ->
                        deck.clear()
                        deck.addAll(loaded.cards)
                        session = loaded.session
                        previousDecisions = loaded.previousDecisions
                        similar = loaded.similar
                        marked = loaded.marked
                        render()
                        // Après l'affichage : une photo jamais mesurée lit sa vignette (PS-164).
                        quality = runCatching { loading.getQualityHints(loaded.cards) }.getOrDefault(emptyMap())
                        if (quality.isNotEmpty() && state.value is DeckUiState.Loaded) render()
                    },
                    onFailure = { if (showLoading) state.value = DeckUiState.Error },
                )
            }
        }

        /**
         * Applique une décision (garder, marquer, favori) à la carte du dessus : [forDay] dans la pile
         * d'un jour, [forMarked] dans celle des marqués, qui décide pour chaque année de marquage. : elle quitte la pile
         * tout de suite, sans attendre l'écriture, pour que la carte suivante apparaisse sous le
         * doigt. Si l'écriture échoue, la carte revient sur le dessus de la pile, rien n'est perdu.
         * Un marquage n'est qu'une décision en base : rien n'est supprimé avant la confirmation
         * du résumé (SPEC §3.5). La dernière carte triée mène au résumé (SPEC §3.4).
         */
        private fun decideTopCard(
            forDay: suspend (MediaItem, LocalDate) -> List<SessionDecision>,
            forMarked: suspend (MarkedMedia) -> List<SessionDecision>,
            afterDecision: (MediaItem) -> Unit = {},
        ) {
            val media = deck.removeFirstOrNull() ?: return
            render()
            viewModelScope.launch {
                runCatching { day?.let { forDay(media, it) } ?: forMarked(marked.getValue(media.id)) }.fold(
                    onSuccess = { made ->
                        session = made.fold(session) { current, decision -> current.record(decision) }
                        render()
                        afterDecision(media)
                        openSummaryWhenDone()
                    },
                    onFailure = {
                        deck.addFirst(media)
                        render()
                    },
                )
            }
        }

        /**
         * La dernière carte triée mène au résumé (SPEC §3.4), une fois la demande de favori traitée :
         * quitter la pile avant perdrait sa réponse.
         */
        private fun openSummaryWhenDone() {
            if (deck.isEmpty() && !galleryFavorite.isPending) {
                effects.trySend(doneEffect)
            }
        }

        /**
         * Annule la dernière décision : l'entrée d'avant est restaurée en base et le média revient sur
         * le dessus de la pile, du côté par lequel il était parti. Une décision déjà confirmée ou une
         * écriture impossible laissent la pile telle quelle.
         */
        private fun undo() {
            if (session.isEmpty) return
            viewModelScope.launch {
                val result = runCatching { undoLastCard() }.getOrNull()
                if (result is UndoLastDecision.Result.Undone) {
                    if (result.decision.exitSide() ==
                        CardSide.TOP
                    ) {
                        galleryFavorite.forgetPending(result.decision.media.id)
                    }
                    deck.addFirst(result.decision.media)
                    render(returningFrom = result.decision.exitSide())
                }
            }
        }

        /**
         * Annule toutes les décisions de la dernière carte : celle de son année, et celles qui ont
         * levé ses marquages des autres années (PS-113), dans la pile d'un jour comme dans celle
         * des marqués. Renvoie celle de la première annulée.
         */
        private suspend fun undoLastCard(): UndoLastDecision.Result {
            val first = decisions.undoLastDecision(session)
            if (first !is UndoLastDecision.Result.Undone) return first
            session = first.session
            val mediaId = first.decision.media.id
            while (session.lastDecision?.media?.id == mediaId) {
                val next = decisions.undoLastDecision(session) as? UndoLastDecision.Result.Undone ?: break
                session = next.session
            }
            return first
        }

        /** Publie l'état de la pile : seules les deux premières cartes sont préparées. */
        private fun render(returningFrom: CardSide? = null) {
            val canUndo = !session.isEmpty
            state.value =
                if (deck.isEmpty()) {
                    DeckUiState.Empty(canUndo)
                } else {
                    DeckUiState
                        .Loaded(
                            cards =
                                deck
                                    .take(
                                        DeckUiState.Loaded.VISIBLE_CARDS,
                                    ).map {
                                        MediaCardUiState.of(
                                            media = it,
                                            day = day ?: marked.getValue(it.id).reviewDays.first(),
                                            zone = clock.zone,
                                            favorite = galleryFavorite.isFavorite(it),
                                            previousDecision = previousDecisions[it.id],
                                            similar = similar[it.id],
                                            qualityHint = quality[it.id],
                                        )
                                    },
                            remainingCount = deck.size,
                            sortedCount = session.decisions.distinctBy { it.media.id }.size,
                            canUndo = canUndo,
                            returningFrom = returningFrom,
                            shareTarget = shareTarget,
                        ).withYearProgress(deck, session.decisions.map { it.media }, clock.zone)
                }
        }
    }

/**
 * Place de la carte du dessus parmi les souvenirs de son année (PS-138) : ceux de [sorted] (triés
 * pendant la session) qui ne sont plus dans [deck], puis ceux qui y restent ; années lues dans [zone].
 * Et, pour la barre de progression, les triés et le total de chaque année de la pile (PS-144).
 */
internal fun DeckUiState.Loaded.withYearProgress(
    deck: Collection<MediaItem>,
    sorted: List<MediaItem>,
    zone: ZoneId,
): DeckUiState.Loaded {
    val inYear = { media: MediaItem -> media.takenAt.atZone(zone).year == year }
    val deckIds = deck.mapTo(HashSet()) { it.id }
    val done = sorted.distinctBy { it.id }.filter { it.id !in deckIds }
    val sortedInYear = done.count(inYear)
    val yearOf = { media: MediaItem -> media.takenAt.atZone(zone).year }
    val segments =
        (done + deck).map(yearOf).distinct().sortedDescending().map { segmentYear ->
            val sortedCount = done.count { yearOf(it) == segmentYear }
            YearSegment(segmentYear, sortedCount, sortedCount + deck.count { yearOf(it) == segmentYear })
        }
    return copy(
        yearPosition = sortedInYear + 1,
        yearTotal = sortedInYear + deck.count(inYear),
        yearSegments = segments,
    )
}

/**
 * Côté par lequel la carte est sortie, lu sur l'entrée écrite au swipe : marquée à gauche, favori
 * en haut, gardée à droite. L'undo la fait revenir de là.
 */
private fun SessionDecision.exitSide(): CardSide =
    when {
        made.decision == ReviewDecision.MARKED_FOR_DELETION -> CardSide.LEFT
        made.favoritePending -> CardSide.TOP
        else -> CardSide.RIGHT
    }

/** Ouvre ou ferme le menu de la carte du dessus ; sans carte, il n'y a pas de menu. */
private fun MutableStateFlow<DeckUiState>.setMenuOpen(open: Boolean) {
    update { current -> if (current is DeckUiState.Loaded) current.copy(menuOpen = open) else current }
}
