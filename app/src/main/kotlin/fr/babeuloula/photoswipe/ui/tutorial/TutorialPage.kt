package fr.babeuloula.photoswipe.ui.tutorial

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import fr.babeuloula.photoswipe.R
import fr.babeuloula.photoswipe.platform.PlatformFeatures

/**
 * Page du tutoriel (SPEC §4), dans l'ordre du carrousel : un titre, un texte court et une
 * illustration.
 *
 * @property title titre de la page.
 * @property body explication de la page.
 * @property icon icône de l'illustration, pour une page sans animation.
 * @property illustration description de l'animation lue par TalkBack, `null` pour une page sans
 * animation (son illustration est décorative).
 */
enum class TutorialPage(
    @param:StringRes val title: Int,
    @param:StringRes val body: Int,
    @param:DrawableRes val icon: Int,
    @param:StringRes val illustration: Int? = null,
) {
    /** Le principe : les souvenirs du même jour, les années précédentes. */
    SAME_DAY(
        R.string.tutorial_same_day_title,
        R.string.tutorial_same_day_body,
        R.drawable.ic_photo_library,
        R.string.tutorial_same_day_illustration,
    ),

    /** Glisser à droite pour garder, à gauche pour marquer. */
    KEEP_OR_DELETE(
        R.string.tutorial_swipes_title,
        R.string.tutorial_swipes_body,
        R.drawable.ic_check,
        R.string.tutorial_swipes_illustration,
    ),

    /** Glisser vers le haut : favori ou partage. */
    SWIPE_UP(
        R.string.tutorial_swipe_up_title,
        R.string.tutorial_swipe_up_body,
        R.drawable.ic_favorite,
        R.string.tutorial_swipe_up_illustration,
    ),

    /** Les derniers jours à rattraper et le rappel quotidien. */
    CATCH_UP(
        R.string.tutorial_catch_up_title,
        R.string.tutorial_catch_up_body,
        R.drawable.ic_calendar_today,
        R.string.tutorial_catch_up_illustration,
    ),

    /** La confirmation groupée et la corbeille restaurable. */
    CONFIRMATION(
        R.string.tutorial_confirmation_title,
        R.string.tutorial_confirmation_body,
        R.drawable.ic_delete,
        R.string.tutorial_confirmation_illustration,
    ),

    /** Mode entraînement : trier des cartes d'exemple, jamais les vraies photos. */
    PRACTICE(R.string.tutorial_practice_title, R.string.tutorial_practice_body, R.drawable.ic_photo_library),
}

/**
 * Textes d'une page tels que le téléphone les montre.
 *
 * @property title titre de la page.
 * @property body explication de la page.
 * @property illustration description de l'animation pour TalkBack, `null` sans animation.
 */
data class TutorialTexts(
    @param:StringRes val title: Int,
    @param:StringRes val body: Int,
    @param:StringRes val illustration: Int?,
)

/**
 * Textes de la page sur un téléphone qui a [features] : sans favoris de la galerie (Android 10),
 * la page du glissement vers le haut ne parle que de « Plus d'actions » (PS-183).
 */
fun TutorialPage.texts(features: PlatformFeatures): TutorialTexts =
    when {
        this == TutorialPage.SWIPE_UP && !features.galleryFavorites -> {
            TutorialTexts(
                R.string.tutorial_more_actions_title,
                R.string.tutorial_more_actions_body,
                R.string.tutorial_more_actions_illustration,
            )
        }

        else -> {
            TutorialTexts(title, body, illustration)
        }
    }
