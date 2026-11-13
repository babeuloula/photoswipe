package fr.babeuloula.photoswipe.ui.postcard

import androidx.compose.ui.graphics.ImageBitmap

/** Aperçu de la carte postale : en préparation, prête, ou impossible à faire. */
sealed interface PostcardPreview {
    /** Photo en cours de lecture ou carte en cours de dessin. */
    data object Loading : PostcardPreview

    /**
     * Carte prête à partager.
     *
     * @property image carte telle qu'elle sera envoyée.
     * @property label repère de la photo, « Il y a 5 ans · 6 nov. 2021 », pour TalkBack.
     */
    data class Ready(
        val image: ImageBitmap,
        val label: String,
    ) : PostcardPreview

    /** La photo n'a pas pu être lue. */
    data object Failed : PostcardPreview
}
