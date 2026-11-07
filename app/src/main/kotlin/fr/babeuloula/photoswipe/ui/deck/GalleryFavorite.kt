package fr.babeuloula.photoswipe.ui.deck

import android.content.IntentSender
import fr.babeuloula.photoswipe.domain.model.MediaId
import fr.babeuloula.photoswipe.domain.model.MediaItem
import fr.babeuloula.photoswipe.domain.port.MediaActionRequest
import fr.babeuloula.photoswipe.domain.usecase.DropPendingFavorite
import fr.babeuloula.photoswipe.domain.usecase.GetPendingFavorites
import fr.babeuloula.photoswipe.domain.usecase.OfferManageMediaTip
import fr.babeuloula.photoswipe.domain.usecase.RecordFavoriteResult
import fr.babeuloula.photoswipe.domain.usecase.RequestFavorite
import fr.babeuloula.photoswipe.domain.usecase.RequestUnfavorite
import fr.babeuloula.photoswipe.permission.MediaManagementReader
import fr.babeuloula.photoswipe.ui.common.MediaActionIntents
import javax.inject.Inject

/**
 * Favori de la galerie demandé dès la pile (SPEC §2) : prépare la demande système du média mis en
 * favori, puis enregistre la réponse. Android affiche son dialogue, ou applique le favori sans
 * rien demander avec l'accès « Gestion des médias » (ADR 0023). Un dialogue d'Android validé fait
 * proposer cet accès sur l'accueil. Une demande à la fois, propre à la pile qui l'utilise.
 *
 * Il sait aussi quels médias montrer avec un cœur (PS-86) : en favori dans la galerie, accepté
 * pendant la session, ou en favori en attente dans l'application ; et retire un favori (PS-124).
 */
class GalleryFavorite
    @Inject
    constructor(
        private val requestFavorite: RequestFavorite,
        private val recordFavoriteResult: RecordFavoriteResult,
        private val offerManageMediaTip: OfferManageMediaTip,
        private val mediaActionIntents: MediaActionIntents,
        private val mediaManagement: MediaManagementReader,
        private val removal: FavoriteRemoval,
    ) {
        /** Demande de retrait des favoris qui attend sa réponse. */
        private var pendingRemoval: MediaActionRequest? = null

        /** Favoris retirés pendant la session : la galerie lue au chargement les montre encore. */
        private val removed = mutableSetOf<MediaId>()

        private var pending: MediaActionRequest? = null

        /** Favoris acceptés par la galerie pendant la session. */
        private val accepted = mutableSetOf<MediaId>()

        /** Favoris en attente dans l'application : lus au chargement, ou refusés pendant la session. */
        private val waiting = mutableSetOf<MediaId>()

        /** Relit les favoris en attente enregistrés ; une lecture impossible n'affiche simplement pas de cœur. */
        suspend fun loadPending() {
            waiting.clear()
            waiting += runCatching { removal.getPendingFavorites() }.getOrDefault(emptySet())
        }

        /** Vrai si [media] doit porter un cœur. */
        fun isFavorite(media: MediaItem): Boolean =
            media.id in accepted || media.id in waiting || (media.isFavorite && media.id !in removed)

        /** Favori annulé par « Annuler » : s'il n'était qu'en attente, le cœur disparaît. */
        fun forgetPending(id: MediaId) {
            waiting -= id
        }

        /** Vrai tant qu'une demande attend sa réponse. */
        val isPending: Boolean get() = pending != null

        /**
         * Prépare la demande de favori de [media] et renvoie le dialogue à lancer ; `null` si elle ne
         * peut pas être préparée : le média reste gardé, favori en attente.
         */
        fun request(media: MediaItem): IntentSender? {
            val request = runCatching { requestFavorite(media) }.getOrNull() ?: return null
            pending = request
            return mediaActionIntents.intentSender(request)
        }

        /**
         * Enregistre la réponse à la demande en cours : acceptée, le favori est appliqué ; refusée, il
         * reste en attente. Renvoie faux s'il n'y avait aucune demande en cours.
         */
        suspend fun answer(confirmed: Boolean): Boolean {
            val request = pending ?: return false
            pending = null
            runCatching { recordFavoriteResult(request, confirmed) }
            if (confirmed) accepted += request.mediaIds else waiting += request.mediaIds
            removed -= request.mediaIds
            if (confirmed && !mediaManagement.isGranted()) runCatching { offerManageMediaTip() }
            return true
        }

        /**
         * Retire [media] des favoris (PS-124). Un favori seulement en attente dans l'application
         * est oublié aussitôt et rien n'est renvoyé ; un favori de la galerie renvoie la demande
         * système à lancer, `null` si elle ne peut pas être préparée.
         */
        suspend fun unfavorite(media: MediaItem): IntentSender? {
            val inGallery = (media.isFavorite && media.id !in removed) || media.id in accepted
            if (!inGallery) {
                runCatching { removal.dropPendingFavorite(media.id) }.onSuccess { waiting -= media.id }
                return null
            }
            pendingRemoval = runCatching { removal.requestUnfavorite(media) }.getOrNull()
            return pendingRemoval?.let(mediaActionIntents::intentSender)
        }

        /** Enregistre la réponse au retrait en cours : accepté, le cœur disparaît ; refusé, rien ne change. */
        suspend fun answerRemoval(confirmed: Boolean) {
            val request = pendingRemoval ?: return
            pendingRemoval = null
            if (!confirmed) return
            removed += request.mediaIds
            accepted -= request.mediaIds
            request.mediaIds.forEach { id ->
                if (id in waiting) runCatching { removal.dropPendingFavorite(id) }
                waiting -= id
            }
        }
    }

/**
 * Use cases des favoris en attente et du retrait d'un favori (PS-86, PS-124), regroupés pour
 * [GalleryFavorite].
 *
 * @property getPendingFavorites favoris en attente dans l'application, pour le cœur des cartes.
 * @property requestUnfavorite prépare la demande système de retrait des favoris.
 * @property dropPendingFavorite oublie un favori resté en attente dans l'application.
 */
class FavoriteRemoval
    @Inject
    constructor(
        val getPendingFavorites: GetPendingFavorites,
        val requestUnfavorite: RequestUnfavorite,
        val dropPendingFavorite: DropPendingFavorite,
    )
