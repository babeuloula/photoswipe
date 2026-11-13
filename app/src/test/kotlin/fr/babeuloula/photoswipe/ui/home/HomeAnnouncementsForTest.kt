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
import fr.babeuloula.photoswipe.widget.WidgetRefresher
import java.time.Clock

/**
 * Annonces de l'accueil sur les vrais use cases et [settings], pour les tests : la version
 * installée [version], les nouveautés [releases] et le rafraîchissement du widget [widgetRefresher].
 */
internal fun homeAnnouncements(
    settings: SettingsRepository,
    clock: Clock,
    version: String = "1.4.0",
    releases: List<WhatsNewRelease> = WHATS_NEW,
    widgetRefresher: WidgetRefresher = WidgetRefresher {},
): HomeAnnouncements =
    HomeAnnouncements(
        SortedDayActions(
            RecordTodaySorted(settings, clock),
            MarkRatingAsked(settings),
            UpdateStreak(settings, clock),
            widgetRefresher,
        ),
        WhatsNewActions(
            CheckWhatsNew(settings, version, releases.map { it.version }),
            MarkWhatsNewSeen(settings, version),
            releases,
        ),
    )
