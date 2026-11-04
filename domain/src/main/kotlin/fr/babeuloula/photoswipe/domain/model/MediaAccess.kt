package fr.babeuloula.photoswipe.domain.model

/**
 * Accès de l'application aux photos et vidéos de l'appareil (SPEC §4).
 *
 * `:app` le déduit des permissions accordées ; le domaine s'en sert pour savoir si l'absence d'un
 * média dans `MediaSource` prouve qu'il a disparu.
 */
enum class MediaAccess {
    /** Toute la galerie est lisible : un média absent a vraiment disparu. */
    FULL,

    /** Accès partiel d'Android 14+ : seuls les médias choisis par l'utilisateur sont lisibles. */
    PARTIAL,

    /** Aucun accès : la galerie paraît vide. */
    NONE,
}
