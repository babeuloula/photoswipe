package fr.babeuloula.photoswipe.domain.usecase

import fr.babeuloula.photoswipe.domain.model.MediaId
import fr.babeuloula.photoswipe.domain.model.MediaItem
import fr.babeuloula.photoswipe.domain.model.MediaType
import fr.babeuloula.photoswipe.domain.model.QualityHint
import fr.babeuloula.photoswipe.domain.port.PhotoAnalyzer
import fr.babeuloula.photoswipe.domain.port.PhotoMeasureCache
import fr.babeuloula.photoswipe.domain.port.SettingsRepository
import fr.babeuloula.photoswipe.domain.service.PhotoQualityRules
import kotlinx.coroutines.flow.first

/**
 * Photos peut-être ratées parmi [media] (PS-164) : floues ou très sombres. Chaque photo n'est
 * mesurée qu'une fois, la mesure étant gardée ([PhotoMeasureCache]) ; les vidéos ne sont pas
 * mesurées. Rien n'est signalé si l'utilisateur a coupé la détection dans les réglages, ni pour une
 * photo illisible. Jamais de décision : seulement un indice à montrer.
 */
class GetQualityHints(
    private val analyzer: PhotoAnalyzer,
    private val cache: PhotoMeasureCache,
    private val settings: SettingsRepository,
    private val rules: PhotoQualityRules = PhotoQualityRules(),
) {
    /** Indice de chaque photo de [media] qui en a un. */
    suspend operator fun invoke(media: List<MediaItem>): Map<MediaId, QualityHint> {
        val enabled = settings.observeQualityHints().first()
        val photos = media.filter { it.type == MediaType.IMAGE }.distinctBy { it.id }
        if (!enabled || photos.isEmpty()) return emptyMap()
        val known = cache.measures(photos.mapTo(HashSet()) { it.id })
        val measured =
            photos
                .filter { it.id !in known }
                .mapNotNull { photo -> analyzer.grayThumbnail(photo)?.let { photo.id to rules.measure(it) } }
                .toMap()
        if (measured.isNotEmpty()) cache.save(measured)
        return (known + measured).mapNotNull { (id, measure) -> rules.hint(measure)?.let { id to it } }.toMap()
    }
}
