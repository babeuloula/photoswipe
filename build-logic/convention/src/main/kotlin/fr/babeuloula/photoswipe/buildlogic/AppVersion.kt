package fr.babeuloula.photoswipe.buildlogic

/**
 * Nom de version de l'application tiré du dernier tag Git `vX.Y.Z` (PS-72), au lieu d'une valeur
 * écrite à la main dans `app/build.gradle.kts`.
 *
 * Il se calcule depuis la sortie de `git describe --tags --long --match "v[0-9]*"` (par exemple
 * `v1.2.0-0-g1a2b3c4`) : sur le commit du tag, `1.2.0` ; après lui, `1.2.0-dev.5` (5 commits plus
 * loin) ; sans tag ou sans Git, [DEVELOPMENT]. Le numéro de version (`versionCode`), lui, reste le
 * nombre de commits : toujours croissant, il laisse un build s'installer par-dessus le précédent.
 */
object AppVersion {
    /** Nom de version sans tag : version de développement. */
    const val DEVELOPMENT = "0.0.0-dev"

    private val DESCRIBE = Regex("""^v(\d+\.\d+\.\d+)-(\d+)-g[0-9a-f]+$""")

    /** Nom de version pour la sortie [describe] de `git describe`, `null` si Git a échoué. */
    fun nameFrom(describe: String?): String {
        val match = describe?.trim()?.let(DESCRIBE::matchEntire) ?: return DEVELOPMENT
        val (version, commitsSinceTag) = match.destructured
        return if (commitsSinceTag == "0") version else "$version-dev.$commitsSinceTag"
    }
}
