package fr.babeuloula.photoswipe.domain.model

/** Ce qu'un badge mesure (PS-158). */
enum class BadgeKind {
    /** Médias triés : gardés ou envoyés à la corbeille. */
    SORTED,

    /** Meilleure série de jours. */
    STREAK,

    /** Octets libérés. */
    FREED,

    /** Les jours de la fenêtre de rattrapage tous triés en même temps (1 une fois atteint). */
    WEEK,
}

/**
 * Badges de la page « Progression » (PS-158), chacun atteint quand sa mesure ([kind]) arrive à
 * [threshold]. Leur nom est enregistré : renommer une constante fait perdre ce badge à qui l'a.
 * Les seuils sont les badges eux-mêmes : les nommer n'ajouterait rien.
 */
@Suppress("MagicNumber")
enum class Badge(
    val kind: BadgeKind,
    val threshold: Long,
) {
    /** Premier média trié. */
    FIRST_SORT(BadgeKind.SORTED, 1),

    /** 100 médias triés. */
    SORTED_100(BadgeKind.SORTED, 100),

    /** 1 000 médias triés. */
    SORTED_1000(BadgeKind.SORTED, 1_000),

    /** 7 jours de suite. */
    STREAK_7(BadgeKind.STREAK, 7),

    /** 30 jours de suite. */
    STREAK_30(BadgeKind.STREAK, 30),

    /** 100 jours de suite. */
    STREAK_100(BadgeKind.STREAK, 100),

    /** 1 Go libéré. */
    FREED_1_GB(BadgeKind.FREED, GIGABYTE),

    /** 5 Go libérés. */
    FREED_5_GB(BadgeKind.FREED, 5 * GIGABYTE),

    /** 10 Go libérés. */
    FREED_10_GB(BadgeKind.FREED, 10 * GIGABYTE),

    /** 50 Go libérés. */
    FREED_50_GB(BadgeKind.FREED, 50 * GIGABYTE),

    /** Toute la liste à rattraper triée. */
    WEEK_CAUGHT_UP(BadgeKind.WEEK, 1),
}

/** Un gigaoctet, en octets décimaux comme les tailles affichées par Android. */
const val GIGABYTE: Long = 1_000_000_000

/**
 * Mesures dont dépendent les badges.
 *
 * @property sorted médias triés (gardés depuis l'historique d'activité, supprimés depuis toujours).
 * @property bestStreak meilleure série de jours.
 * @property freedBytes octets libérés depuis toujours.
 * @property weekCaughtUp vrai si toute la fenêtre de rattrapage est triée.
 */
data class BadgeMeasures(
    val sorted: Long = 0,
    val bestStreak: Long = 0,
    val freedBytes: Long = 0,
    val weekCaughtUp: Boolean = false,
) {
    /** Valeur de la mesure [kind]. */
    fun valueOf(kind: BadgeKind): Long =
        when (kind) {
            BadgeKind.SORTED -> sorted
            BadgeKind.STREAK -> bestStreak
            BadgeKind.FREED -> freedBytes
            BadgeKind.WEEK -> if (weekCaughtUp) 1 else 0
        }
}

/**
 * Où en est un badge.
 *
 * @property badge badge concerné.
 * @property earned vrai une fois obtenu ; il le reste même si sa mesure redescend (annulation).
 * @property remaining ce qu'il manque pour l'obtenir, dans l'unité de sa mesure ; 0 s'il est obtenu.
 */
data class BadgeStatus(
    val badge: Badge,
    val earned: Boolean,
    val remaining: Long,
)

/**
 * Objectif de place à libérer (PS-158), compté à partir du moment où il est fixé.
 *
 * @property targetBytes octets à libérer.
 * @property startFreedBytes octets déjà libérés quand l'objectif a été fixé.
 */
data class FreeSpaceGoal(
    val targetBytes: Long,
    val startFreedBytes: Long,
)

/**
 * Objectif et avancement.
 *
 * @property goal objectif fixé.
 * @property freedBytes octets libérés depuis qu'il est fixé.
 */
data class GoalProgress(
    val goal: FreeSpaceGoal,
    val freedBytes: Long,
) {
    /** Vrai une fois l'objectif atteint. */
    val reached: Boolean get() = freedBytes >= goal.targetBytes

    /** Avancement entre 0 et 1. */
    val fraction: Float get() = (freedBytes.toFloat() / goal.targetBytes).coerceIn(0f, 1f)
}

/**
 * Badges et objectif de la page « Progression ».
 *
 * @property badges tous les badges, dans l'ordre de [Badge].
 * @property newlyEarned badges obtenus depuis la dernière lecture, à annoncer une fois.
 * @property goal objectif en cours, `null` s'il n'y en a pas.
 */
data class Achievements(
    val badges: List<BadgeStatus>,
    val newlyEarned: List<Badge> = emptyList(),
    val goal: GoalProgress? = null,
)
