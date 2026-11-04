package fr.babeuloula.photoswipe.di

import fr.babeuloula.photoswipe.data.media.MediaStoreMediaSource
import fr.babeuloula.photoswipe.debug.DemoMediaCatalog
import fr.babeuloula.photoswipe.debug.DeveloperSettings
import fr.babeuloula.photoswipe.domain.fixtures.FakeMediaSource
import fr.babeuloula.photoswipe.domain.model.MediaId
import fr.babeuloula.photoswipe.domain.model.MediaItem
import fr.babeuloula.photoswipe.domain.port.MediaSource
import java.time.Clock
import java.time.Instant
import javax.inject.Inject

/**
 * [MediaSource] de la variante debug, choisi par le câblage Hilt : MediaStore par défaut, ou un [FakeMediaSource] rempli par
 * [DemoMediaCatalog] quand les données de démo sont activées. Le choix est relu à chaque appel :
 * basculer dans les outils développeur prend effet au prochain chargement d'un écran.
 */
class SwitchableMediaSource
    @Inject
    constructor(
        private val mediaStore: MediaStoreMediaSource,
        private val settings: DeveloperSettings,
        private val clock: Clock,
    ) : MediaSource {
        /** Données de démo, générées au premier usage pour le jour courant. */
        private val demo: FakeMediaSource by lazy { FakeMediaSource(DemoMediaCatalog.generate(clock)) }

        override suspend fun mediaTakenIn(ranges: List<OpenEndRange<Instant>>): List<MediaItem> =
            current().mediaTakenIn(ranges)

        override suspend fun mediaByIds(ids: Set<MediaId>): List<MediaItem> = current().mediaByIds(ids)

        private fun current(): MediaSource = if (settings.demoDataEnabled) demo else mediaStore
    }
