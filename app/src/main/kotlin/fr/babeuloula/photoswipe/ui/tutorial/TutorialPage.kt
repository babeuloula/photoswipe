package fr.babeuloula.photoswipe.ui.tutorial

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import fr.babeuloula.photoswipe.R

/**
 * Page du tutoriel (SPEC §4), dans l'ordre du carrousel : un titre, un texte court et une
 * illustration.
 *
 * @property title titre de la page.
 * @property body explication de la page.
 * @property icon icône de l'illustration, en attendant son animation.
 */
enum class TutorialPage(
    @param:StringRes val title: Int,
    @param:StringRes val body: Int,
    @param:DrawableRes val icon: Int,
) {
    /** Le principe : les souvenirs du même jour, les années précédentes. */
    SAME_DAY(R.string.tutorial_same_day_title, R.string.tutorial_same_day_body, R.drawable.ic_photo_library),

    /** Glisser à droite pour garder, à gauche pour marquer. */
    KEEP_OR_DELETE(R.string.tutorial_swipes_title, R.string.tutorial_swipes_body, R.drawable.ic_check),

    /** Glisser vers le haut : favori ou partage. */
    SWIPE_UP(R.string.tutorial_swipe_up_title, R.string.tutorial_swipe_up_body, R.drawable.ic_favorite),

    /** Les 7 derniers jours à rattraper et le rappel quotidien. */
    CATCH_UP(R.string.tutorial_catch_up_title, R.string.tutorial_catch_up_body, R.drawable.ic_calendar_today),

    /** La confirmation groupée et la corbeille restaurable. */
    CONFIRMATION(R.string.tutorial_confirmation_title, R.string.tutorial_confirmation_body, R.drawable.ic_delete),
}
