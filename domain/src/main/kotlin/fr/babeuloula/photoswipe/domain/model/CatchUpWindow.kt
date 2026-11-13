package fr.babeuloula.photoswipe.domain.model

/**
 * Durée de la fenêtre de rattrapage choisie dans les réglages (PS-169) : aujourd'hui et les jours
 * précédents proposés sur l'accueil, dans le rappel et dans le résumé.
 *
 * Les durées sont la définition même de chaque valeur : des constantes n'y ajouteraient rien.
 *
 * @property days nombre de jours de la fenêtre, aujourd'hui compris.
 */
@Suppress("MagicNumber")
enum class CatchUpWindow(
    val days: Int,
) {
    /** Une semaine : aujourd'hui et les 6 jours précédents. */
    WEEK(7),

    /** Deux semaines. */
    TWO_WEEKS(14),

    /** Un mois : après une longue absence. */
    MONTH(30),

    ;

    companion object {
        /** Fenêtre d'une installation neuve : une semaine (SPEC §3.1). */
        val DEFAULT: CatchUpWindow = WEEK
    }
}
