package fr.babeuloula.photoswipe.domain.model

/**
 * Session de tri en cours et sa pile d'undo (SPEC §3.4).
 *
 * Valeur immuable : chaque décision ou undo produit une nouvelle session, que la pile de swipe
 * garde dans l'état de son écran. La pile vit donc avec la session et disparaît avec elle ; les
 * décisions, elles, sont déjà en base.
 *
 * @property decisions décisions de la session, de la plus ancienne à la plus récente.
 */
data class ReviewSession(
    val decisions: List<SessionDecision> = emptyList(),
) {
    /** Vrai si aucune décision n'est à annuler. */
    val isEmpty: Boolean get() = decisions.isEmpty()

    /** Décision la plus récente, la prochaine que l'undo annulera ; `null` si la session est vide. */
    val lastDecision: SessionDecision? get() = decisions.lastOrNull()

    /** Session qui ajoute [decision] au sommet de la pile. */
    fun record(decision: SessionDecision): ReviewSession = ReviewSession(decisions + decision)

    /** Session sans sa décision la plus récente ; une session vide reste vide. */
    fun withoutLastDecision(): ReviewSession = ReviewSession(decisions.dropLast(1))
}
