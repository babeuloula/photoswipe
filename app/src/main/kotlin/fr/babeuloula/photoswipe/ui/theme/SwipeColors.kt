package fr.babeuloula.photoswipe.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Couleurs des indicateurs de la pile (SPEC §4.3) : vert pour garder, rouge pour supprimer, bleu
 * pour le menu favori, chacune avec son conteneur, plus doux, et leurs couleurs de contenu.
 *
 * Material 3 n'a pas de rôle « succès » : le vert est une couleur personnalisée du thème, comme le
 * bleu (le rôle `primary` suit le fond d'écran et ne serait pas toujours bleu). Ce sont les
 * couleurs #1E8A4A et #2F6FD0 harmonisées vers la graine #8A6A1E (`Blend.harmonize` de
 * material-color-utilities), aux tons 40 / 100 / 90 / 10 en clair et 80 / 20 / 30 / 90 en sombre
 * (ADR 0025). Le rouge reprend les rôles `error` du schéma.
 *
 * @property keep voile du geste « Garder », bouton pressé.
 * @property onKeep contenu (icône, texte) posé sur [keep].
 * @property keepContainer bouton « Garder » au repos, pastille du voile.
 * @property onKeepContainer contenu posé sur [keepContainer].
 * @property delete voile du geste « Supprimer », bouton pressé.
 * @property onDelete contenu posé sur [delete].
 * @property deleteContainer bouton « Supprimer » au repos, pastille du voile.
 * @property onDeleteContainer contenu posé sur [deleteContainer].
 * @property favorite voile du menu favori, cœur des favoris.
 * @property onFavorite contenu posé sur [favorite].
 * @property favoriteContainer pastille du voile du menu favori.
 * @property onFavoriteContainer contenu posé sur [favoriteContainer].
 * @property favoriteOnFixed cœur posé sur le brun du bord de film (`onPrimaryFixed`), le même en
 *   clair et en sombre.
 */
@Immutable
data class SwipeColors(
    val keep: Color,
    val onKeep: Color,
    val keepContainer: Color,
    val onKeepContainer: Color,
    val delete: Color,
    val onDelete: Color,
    val deleteContainer: Color,
    val onDeleteContainer: Color,
    val favorite: Color,
    val onFavorite: Color,
    val favoriteContainer: Color,
    val onFavoriteContainer: Color,
    val favoriteOnFixed: Color,
)

/** Tons d'une couleur personnalisée : la couleur, son contenu, son conteneur et le contenu de celui-ci. */
private data class CustomTones(
    val color: Color,
    val onColor: Color,
    val container: Color,
    val onContainer: Color,
)

/** Vert de « garder » du thème clair. */
private val KeepLight = CustomTones(Color(0xFF316B16), Color(0xFFFFFFFF), Color(0xFFB1F48F), Color(0xFF062100))

/** Vert de « garder » du thème sombre. */
private val KeepDark = CustomTones(Color(0xFF96D876), Color(0xFF103900), Color(0xFF1A5200), Color(0xFFB1F48F))

/** Bleu du menu favori du thème clair. */
private val FavoriteLight = CustomTones(Color(0xFF00629D), Color(0xFFFFFFFF), Color(0xFFCFE5FF), Color(0xFF001D33))

/** Bleu du menu favori du thème sombre. */
private val FavoriteDark = CustomTones(Color(0xFF99CBFF), Color(0xFF003354), Color(0xFF004A78), Color(0xFFCFE5FF))

/** Couleurs des indicateurs pour [colorScheme], en clair ou en sombre selon [darkTheme]. */
internal fun swipeColors(
    colorScheme: ColorScheme,
    darkTheme: Boolean,
): SwipeColors {
    val keep = if (darkTheme) KeepDark else KeepLight
    val favorite = if (darkTheme) FavoriteDark else FavoriteLight
    return SwipeColors(
        keep = keep.color,
        onKeep = keep.onColor,
        keepContainer = keep.container,
        onKeepContainer = keep.onContainer,
        delete = colorScheme.error,
        onDelete = colorScheme.onError,
        deleteContainer = colorScheme.errorContainer,
        onDeleteContainer = colorScheme.onErrorContainer,
        favorite = favorite.color,
        onFavorite = favorite.onColor,
        favoriteContainer = favorite.container,
        onFavoriteContainer = favorite.onContainer,
        favoriteOnFixed = FavoriteDark.color,
    )
}

/** Fournies par [PhotoSwipeTheme]. */
internal val LocalSwipeColors = staticCompositionLocalOf { swipeColors(LightColorScheme, darkTheme = false) }

/** Couleurs des indicateurs de geste du thème courant. */
val MaterialTheme.swipeColors: SwipeColors
    @Composable
    @ReadOnlyComposable
    get() = LocalSwipeColors.current
