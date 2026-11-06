package fr.babeuloula.photoswipe.domain.model

/** Thème de l'application choisi dans les réglages (SPEC §4.5). */
enum class ThemeMode {
    /** Suit le thème clair ou sombre du téléphone. */
    SYSTEM,

    /** Toujours clair. */
    LIGHT,

    /** Toujours sombre. */
    DARK,

    ;

    companion object {
        /** Thème d'une installation neuve : celui du téléphone. */
        val DEFAULT: ThemeMode = SYSTEM
    }
}
