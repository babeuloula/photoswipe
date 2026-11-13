package fr.babeuloula.photoswipe.domain.model

/**
 * Version publiée de l'application, `X.Y.Z` (PS-173), comparée pour savoir quelles nouveautés
 * l'utilisateur n'a pas encore vues.
 *
 * Elle se lit sur le nom de version (`versionName`), tiré du dernier tag `vX.Y.Z` : un build de
 * développement `1.3.0-dev.5` (5 commits après le tag `v1.3.0`) vaut `1.3.0`, la dernière version
 * publiée qu'il contient ; un build sans tag (`0.0.0-dev`) vaut `0.0.0`. Aucun ne montre donc les
 * nouveautés d'une version pas encore publiée.
 *
 * @property major premier nombre.
 * @property minor deuxième nombre.
 * @property patch troisième nombre.
 */
data class ReleaseVersion(
    val major: Int,
    val minor: Int,
    val patch: Int,
) : Comparable<ReleaseVersion> {
    override fun compareTo(other: ReleaseVersion): Int =
        compareValuesBy(this, other, ReleaseVersion::major, ReleaseVersion::minor, ReleaseVersion::patch)

    /** La version écrite `X.Y.Z`, telle qu'elle est enregistrée. */
    override fun toString(): String = "$major.$minor.$patch"

    /** Lecture d'un nom de version. */
    companion object {
        private val NAME = Regex("""^(\d+)\.(\d+)\.(\d+)(-.*)?$""")

        /**
         * Version du nom [name] (`1.4.0`, `1.3.0-dev.5`, `0.0.0-dev`), sans ce qui suit le tiret ;
         * `null` si le nom n'a pas cette forme.
         */
        fun parse(name: String): ReleaseVersion? =
            NAME
                .matchEntire(name.trim())
                ?.groupValues
                ?.subList(1, VERSION_PARTS + 1)
                ?.map { it.toIntOrNull() }
                ?.takeIf { null !in it }
                ?.filterNotNull()
                ?.let { (major, minor, patch) -> ReleaseVersion(major, minor, patch) }

        /** Nombres d'une version : majeur, mineur, correctif. */
        private const val VERSION_PARTS = 3
    }
}
