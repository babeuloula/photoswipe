package fr.babeuloula.photoswipe.ui.whatsnew

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import fr.babeuloula.photoswipe.R
import fr.babeuloula.photoswipe.domain.model.ReleaseVersion

/**
 * Une nouveauté d'une version (PS-173) : ce que l'utilisateur voit ou peut faire de nouveau.
 *
 * @property title titre court, clé `whats_new_<version>_<sujet>_title`.
 * @property text une ou deux phrases qui disent où la trouver et à quoi elle sert, clé
 * `whats_new_<version>_<sujet>_text`.
 * @property icon icône Material Symbols de la nouveauté.
 */
data class WhatsNewItem(
    @param:StringRes val title: Int,
    @param:StringRes val text: Int,
    @param:DrawableRes val icon: Int,
)

/**
 * Les nouveautés d'une version publiée.
 *
 * @property version version `X.Y.Z` qui les apporte.
 * @property items ses nouveautés, dans l'ordre d'affichage ; autant qu'il en faut.
 */
data class WhatsNewRelease(
    val version: ReleaseVersion,
    val items: List<WhatsNewItem>,
)

/**
 * Nouveautés de chaque version, de la plus récente à la plus ancienne (PS-173).
 *
 * Règle : seulement ce que l'utilisateur voit ou peut faire de nouveau, jamais un correctif ni un
 * changement technique. Une version sans nouveauté visible n'a pas d'entrée : sa mise à jour
 * n'affiche rien. Chaque texte a sa clé de traduction par version (`whats_new_1_4_0_…`), en anglais
 * et en français. L'entrée s'ajoute avant de lancer la release (`docs/store/README.md`).
 */
val WHATS_NEW: List<WhatsNewRelease> =
    listOf(
        WhatsNewRelease(
            version = ReleaseVersion(major = 1, minor = 4, patch = 0),
            items =
                listOf(
                    WhatsNewItem(
                        R.string.whats_new_1_4_0_progress_title,
                        R.string.whats_new_1_4_0_progress_text,
                        R.drawable.ic_progress,
                    ),
                    WhatsNewItem(
                        R.string.whats_new_1_4_0_calendar_title,
                        R.string.whats_new_1_4_0_calendar_text,
                        R.drawable.ic_calendar_today,
                    ),
                    WhatsNewItem(
                        R.string.whats_new_1_4_0_goals_title,
                        R.string.whats_new_1_4_0_goals_text,
                        R.drawable.ic_emoji_events,
                    ),
                    WhatsNewItem(
                        R.string.whats_new_1_4_0_batch_title,
                        R.string.whats_new_1_4_0_batch_text,
                        R.drawable.ic_done_all,
                    ),
                    WhatsNewItem(
                        R.string.whats_new_1_4_0_quality_title,
                        R.string.whats_new_1_4_0_quality_text,
                        R.drawable.ic_blur_on,
                    ),
                    WhatsNewItem(
                        R.string.whats_new_1_4_0_widget_title,
                        R.string.whats_new_1_4_0_widget_text,
                        R.drawable.ic_widgets,
                    ),
                ),
        ),
    )
