package fr.babeuloula.photoswipe.ui.home

import fr.babeuloula.photoswipe.domain.usecase.MarkRatingAsked
import fr.babeuloula.photoswipe.domain.usecase.RecordTodaySorted
import fr.babeuloula.photoswipe.domain.usecase.UpdateStreak
import javax.inject.Inject

/**
 * Use cases que l'accueil lance d'après les jours triés, regroupés : l'invitation à noter
 * l'application (PS-121) et la série de jours (PS-155).
 *
 * @property recordTodaySorted compte la pile du jour entièrement triée et dit si l'invitation est due.
 * @property markRatingAsked retient que l'invitation a été montrée.
 * @property updateStreak recompte la série de jours d'après la fenêtre.
 */
class SortedDayActions
    @Inject
    constructor(
        val recordTodaySorted: RecordTodaySorted,
        val markRatingAsked: MarkRatingAsked,
        val updateStreak: UpdateStreak,
    )
