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
 * pour le menu favori.
 *
 * Material 3 n'a pas de rôle « succès » : le vert est une couleur personnalisée du thème, générée
 * avec Material Theme Builder (source #4CAF50) pour le clair et le sombre. Le bleu l'est aussi
 * (source #2196F3) : le rôle `primary` suivrait le fond d'écran et ne serait pas toujours bleu. Le
 * rouge reprend le rôle `error` du schéma, couleurs dynamiques comprises.
 *
 * @property keep voile et bouton « Garder ».
 * @property onKeep contenu (icône, texte) posé sur [keep].
 * @property delete voile et bouton « Supprimer ».
 * @property onDelete contenu posé sur [delete].
 * @property favorite voile du menu favori.
 * @property onFavorite contenu posé sur [favorite].
 */
@Immutable
data class SwipeColors(
    val keep: Color,
    val onKeep: Color,
    val delete: Color,
    val onDelete: Color,
    val favorite: Color,
    val onFavorite: Color,
)

/** Vert de « garder » du thème clair, et sa couleur de contenu. */
private val KeepLight = Color(0xFF2E6B30)
private val OnKeepLight = Color(0xFFFFFFFF)

/** Vert de « garder » du thème sombre, et sa couleur de contenu. */
private val KeepDark = Color(0xFF94D78F)
private val OnKeepDark = Color(0xFF00390A)

/** Bleu du menu favori du thème clair, et sa couleur de contenu. */
private val FavoriteLight = Color(0xFF1F5FA6)
private val OnFavoriteLight = Color(0xFFFFFFFF)

/** Bleu du menu favori du thème sombre, et sa couleur de contenu. */
private val FavoriteDark = Color(0xFFA4C9FF)
private val OnFavoriteDark = Color(0xFF00315D)

/** Couleurs des indicateurs pour [colorScheme], en clair ou en sombre selon [darkTheme]. */
internal fun swipeColors(
    colorScheme: ColorScheme,
    darkTheme: Boolean,
): SwipeColors =
    SwipeColors(
        keep = if (darkTheme) KeepDark else KeepLight,
        onKeep = if (darkTheme) OnKeepDark else OnKeepLight,
        delete = colorScheme.error,
        onDelete = colorScheme.onError,
        favorite = if (darkTheme) FavoriteDark else FavoriteLight,
        onFavorite = if (darkTheme) OnFavoriteDark else OnFavoriteLight,
    )

/** Fournies par [PhotoSwipeTheme]. */
internal val LocalSwipeColors = staticCompositionLocalOf { swipeColors(LightColorScheme, darkTheme = false) }

/** Couleurs des indicateurs de geste du thème courant. */
val MaterialTheme.swipeColors: SwipeColors
    @Composable
    @ReadOnlyComposable
    get() = LocalSwipeColors.current
