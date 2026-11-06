package fr.babeuloula.photoswipe.ui.deck

/** Bouton de la rangée sous la carte (`DecisionButtons`). */
enum class DeckButton {
    /** « Supprimer » : marque la carte pour suppression. */
    MARK_FOR_DELETION,

    /** « Annuler » : annule la dernière décision. */
    UNDO,

    /** « Menu » : favori ou partage. */
    MENU,

    /** « Garder ». */
    KEEP,
}
