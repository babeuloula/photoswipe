package fr.babeuloula.photoswipe.ui.home

import fr.babeuloula.photoswipe.ui.whatsnew.WhatsNewActions
import javax.inject.Inject

/**
 * Ce que l'accueil annonce de lui-même, regroupé : ce qui suit les jours triés (invitation à noter
 * l'application, série de jours) et les nouveautés après une mise à jour (PS-173).
 *
 * @property sortedDay use cases lancés d'après les jours triés.
 * @property whatsNew nouveautés des versions manquées.
 */
class HomeAnnouncements
    @Inject
    constructor(
        val sortedDay: SortedDayActions,
        val whatsNew: WhatsNewActions,
    )
