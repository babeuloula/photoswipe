package fr.babeuloula.photoswipe.ui.home

import fr.babeuloula.photoswipe.domain.port.SettingsRepository
import fr.babeuloula.photoswipe.domain.usecase.CheckWhatsNew
import fr.babeuloula.photoswipe.domain.usecase.MarkRatingAsked
import fr.babeuloula.photoswipe.domain.usecase.MarkWhatsNewSeen
import fr.babeuloula.photoswipe.domain.usecase.RecordTodaySorted
import fr.babeuloula.photoswipe.domain.usecase.UpdateStreak
import fr.babeuloula.photoswipe.ui.whatsnew.WHATS_NEW
import fr.babeuloula.photoswipe.ui.whatsnew.WhatsNewActions
import fr.babeuloula.photoswipe.ui.whatsnew.WhatsNewRelease
import java.time.Clock

/**
 * Annonces de l'accueil sur les vrais use cases et [settings], pour les tests : la version
 * installée [version] et les nouveautés [releases].
 */
internal fun homeAnnouncements(
    settings: SettingsRepository,
    clock: Clock,
    version: String = "1.4.0",
    releases: List<WhatsNewRelease> = WHATS_NEW,
): HomeAnnouncements =
    HomeAnnouncements(
        SortedDayActions(RecordTodaySorted(settings, clock), MarkRatingAsked(settings), UpdateStreak(settings, clock)),
        WhatsNewActions(
            CheckWhatsNew(settings, version, releases.map { it.version }),
            MarkWhatsNewSeen(settings, version),
            releases,
        ),
    )
