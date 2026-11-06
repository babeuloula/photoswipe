package fr.babeuloula.photoswipe.ui.deck

import android.content.IntentSender
import fr.babeuloula.photoswipe.domain.model.MediaId
import fr.babeuloula.photoswipe.domain.model.MediaItem
import fr.babeuloula.photoswipe.domain.port.MediaActionRequest
import fr.babeuloula.photoswipe.domain.usecase.GetPendingFavorites
import fr.babeuloula.photoswipe.domain.usecase.OfferManageMediaTip
import fr.babeuloula.photoswipe.domain.usecase.RecordFavoriteResult
import fr.babeuloula.photoswipe.domain.usecase.RequestFavorite
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
 * pendant la session, ou en favori en attente dans l'application.
 */
class GalleryFavorite
    @Inject
    constructor(
        private val requestFavorite: RequestFavorite,
        private val recordFavoriteResult: RecordFavoriteResult,
        private val offerManageMediaTip: OfferManageMediaTip,
        private val mediaActionIntents: MediaActionIntents,
        private val mediaManagement: MediaManagementReader,
        private val getPendingFavorites: GetPendingFavorites,
    ) {
        private var pending: MediaActionRequest? = null

        /** Favoris acceptés par la galerie pendant la session. */
        private val accepted = mutableSetOf<MediaId>()

        /** Favoris en attente dans l'application : lus au chargement, ou refusés pendant la session. */
        private val waiting = mutableSetOf<MediaId>()

        /** Relit les favoris en attente enregistrés ; une lecture impossible n'affiche simplement pas de cœur. */
        suspend fun loadPending() {
            waiting.clear()
            waiting += runCatching { getPendingFavorites() }.getOrDefault(emptySet())
        }

        /** Vrai si [media] doit porter un cœur. */
        fun isFavorite(media: MediaItem): Boolean = media.isFavorite || media.id in accepted || media.id in waiting

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
            if (confirmed && !mediaManagement.isGranted()) runCatching { offerManageMediaTip() }
            return true
        }
    }
