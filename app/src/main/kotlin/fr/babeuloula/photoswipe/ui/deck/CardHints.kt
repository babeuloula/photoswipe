package fr.babeuloula.photoswipe.ui.deck

import fr.babeuloula.photoswipe.domain.model.MediaId
import fr.babeuloula.photoswipe.domain.model.MediaItem
import fr.babeuloula.photoswipe.domain.model.QualityHint
import fr.babeuloula.photoswipe.domain.usecase.GetMotionPhotos
import fr.babeuloula.photoswipe.domain.usecase.GetQualityHints
import javax.inject.Inject

/**
 * Ce qui se lit dans le fichier des photos une fois l'écran affiché, pour la pile comme pour la
 * vue d'ensemble : les photos peut-être ratées (PS-164) et les photos animées (PS-167). Ce ne sont
 * que des aides : une lecture qui échoue n'apporte rien, sans gêner l'écran.
 */
class CardHints
    @Inject
    constructor(
        private val getQualityHints: GetQualityHints,
        private val getMotionPhotos: GetMotionPhotos,
    ) {
        /** Photos de [media] peut-être floues ou très sombres. */
        suspend fun quality(media: List<MediaItem>): Map<MediaId, QualityHint> =
            runCatching { getQualityHints(media) }.getOrDefault(emptyMap())

        /** URI de la vidéo de chaque photo animée de [media], à donner au lecteur de la pile. */
        suspend fun motionVideos(media: List<MediaItem>): Map<MediaId, String> {
            val uris = media.associate { it.id to it.uri }
            return runCatching { getMotionPhotos(media) }
                .getOrDefault(emptyMap())
                .mapValues { (id, motion) -> motionVideoUri(uris.getValue(id), motion) }
        }
    }
