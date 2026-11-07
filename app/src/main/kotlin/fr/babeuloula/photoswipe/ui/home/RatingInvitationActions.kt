package fr.babeuloula.photoswipe.ui.home

import fr.babeuloula.photoswipe.domain.usecase.MarkRatingAsked
import fr.babeuloula.photoswipe.domain.usecase.RecordTodaySorted
import javax.inject.Inject

/**
 * Use cases de l'invitation à noter l'application (PS-121), regroupés pour l'accueil.
 *
 * @property recordTodaySorted compte la pile du jour entièrement triée et dit si l'invitation est due.
 * @property markRatingAsked retient que l'invitation a été montrée.
 */
class RatingInvitationActions
    @Inject
    constructor(
        val recordTodaySorted: RecordTodaySorted,
        val markRatingAsked: MarkRatingAsked,
    )
