package fr.babeuloula.photoswipe.ui.home

import fr.babeuloula.photoswipe.domain.usecase.MarkRatingAsked
import fr.babeuloula.photoswipe.domain.usecase.RecordTodaySorted
import fr.babeuloula.photoswipe.domain.usecase.UpdateStreak
import fr.babeuloula.photoswipe.widget.WidgetRefresher
import javax.inject.Inject

/**
 * Use cases que l'accueil lance d'après les jours triés, regroupés : l'invitation à noter
 * l'application (PS-121), la série de jours (PS-155) et la mise à jour du widget « Ce jour-là »
 * (PS-170).
 *
 * @property recordTodaySorted compte la pile du jour entièrement triée et dit si l'invitation est due.
 * @property markRatingAsked retient que l'invitation a été montrée.
 * @property updateStreak recompte la série de jours d'après la fenêtre.
 * @property widgetRefresher redessine le widget de l'écran d'accueil d'après les jours relus.
 */
class SortedDayActions
    @Inject
    constructor(
        val recordTodaySorted: RecordTodaySorted,
        val markRatingAsked: MarkRatingAsked,
        val updateStreak: UpdateStreak,
        val widgetRefresher: WidgetRefresher,
    )
