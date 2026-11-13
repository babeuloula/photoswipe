package fr.babeuloula.photoswipe.widget

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Size
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import fr.babeuloula.photoswipe.domain.model.MediaItem
import fr.babeuloula.photoswipe.domain.model.TodayWidget
import fr.babeuloula.photoswipe.domain.usecase.GetTodayWidget
import fr.babeuloula.photoswipe.permission.MediaAccessReader
import kotlinx.coroutines.CancellationException
import javax.inject.Inject

/**
 * Ce que dessine le widget « Ce jour-là » (PS-170).
 *
 * @property state souvenirs du jour à trier, ou l'état qui en tient lieu.
 * @property photo vignette du souvenir montré, `null` sans photo (choix du widget, ou vignette
 * impossible à produire).
 */
data class ThisDayWidgetData(
    val state: TodayWidget,
    val photo: Bitmap? = null,
)

/**
 * Lit l'état du widget « Ce jour-là » dans le domaine ([GetTodayWidget]) et produit sa vignette,
 * seulement si le widget montre une photo.
 */
class ThisDayWidgetLoader
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
        private val getTodayWidget: GetTodayWidget,
        private val mediaAccess: MediaAccessReader,
    ) {
        /** État et vignette du widget ; [showPhoto] faux : aucune vignette n'est lue. */
        suspend fun load(showPhoto: Boolean): ThisDayWidgetData {
            val state =
                runCatching { getTodayWidget(mediaAccess.currentAccess()) }.getOrElse { error ->
                    if (error is CancellationException) throw error
                    // Galerie illisible (accès retiré entre-temps) : le widget invite à ouvrir l'application.
                    TodayWidget.NoAccess
                }
            val preview = (state as? TodayWidget.ToSort)?.preview?.takeIf { showPhoto }
            return ThisDayWidgetData(state, preview?.let { thumbnail(it) })
        }

        /** Vignette d'un souvenir, ou `null` si Android ne peut pas la produire. */
        private fun thumbnail(media: MediaItem): Bitmap? =
            runCatching {
                context.contentResolver.loadThumbnail(Uri.parse(media.uri), Size(THUMBNAIL_SIZE, THUMBNAIL_SIZE), null)
            }.getOrNull()

        private companion object {
            /** Taille de la vignette, en pixels : nette sur un widget moyen sans peser sur ses RemoteViews. */
            const val THUMBNAIL_SIZE = 360
        }
    }

/** Accès du widget au graphe Hilt : un widget Glance n'est pas un composant Android injectable. */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface ThisDayWidgetEntryPoint {
    /** Lecture de l'état du widget. */
    fun widgetLoader(): ThisDayWidgetLoader

    /** Planification de ses mises à jour. */
    fun widgetUpdates(): ThisDayWidgetUpdates
}
