package fr.babeuloula.photoswipe.widget

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.currentState
import androidx.glance.material3.ColorProviders
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import dagger.hilt.android.EntryPointAccessors
import fr.babeuloula.photoswipe.ui.theme.DarkColorScheme
import fr.babeuloula.photoswipe.ui.theme.LightColorScheme

/**
 * Widget « Ce jour-là » de l'écran d'accueil (PS-170) : une vignette d'un souvenir du jour, la
 * date et le nombre de souvenirs qui restent à trier ; un appui ouvre la pile du jour.
 *
 * Chaque widget posé garde son état Glance ([PreferencesGlanceStateDefinition]) : le choix « sans
 * photo » de son écran de réglage ([SHOW_PHOTO]) et un compteur de rafraîchissements
 * ([REFRESH]). Une session Glance déjà ouverte ne relance pas [provideGlance] : c'est le compteur,
 * lu dans la composition, qui fait relire les souvenirs ([refreshAll]).
 *
 * Aux couleurs de l'application (palette « planche-contact », ADR 0025), claires ou sombres selon
 * le téléphone. Deux tailles : petite (2 × 2) et moyenne (4 × 2).
 */
class ThisDayWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Responsive(setOf(SMALL, MEDIUM))

    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(
        context: Context,
        id: GlanceId,
    ) {
        val loader = EntryPointAccessors.fromApplication<ThisDayWidgetEntryPoint>(context).widgetLoader()
        val savedState = getAppWidgetState(context, PreferencesGlanceStateDefinition, id)
        val firstShowPhoto = savedState.showPhoto
        val firstRefresh = savedState[REFRESH] ?: 0
        val first = loader.load(firstShowPhoto)
        provideContent {
            val state = currentState<Preferences>()
            val showPhoto = state.showPhoto
            val refresh = state[REFRESH] ?: 0
            val data by produceState(first, showPhoto, refresh) {
                // Les souvenirs lus avant la composition sont encore à jour au premier passage.
                if (showPhoto != firstShowPhoto || refresh != firstRefresh) value = loader.load(showPhoto)
            }
            GlanceTheme(colors = WidgetColors) {
                ThisDayWidgetContent(data)
            }
        }
    }

    companion object {
        /** Petite taille, 2 × 2 cases : la photo seule, la date et le nombre posés dessus. */
        val SMALL: DpSize = DpSize(110.dp, 110.dp)

        /** Taille moyenne, 4 × 2 cases : la photo à gauche, le texte à droite. */
        val MEDIUM: DpSize = DpSize(250.dp, 110.dp)

        /** Choix de montrer une photo dans le widget ; vrai tant qu'il n'a pas été réglé. */
        val SHOW_PHOTO: Preferences.Key<Boolean> = booleanPreferencesKey("show_photo")

        /** Compteur des rafraîchissements demandés, lu par la composition pour relire les souvenirs. */
        val REFRESH: Preferences.Key<Int> = intPreferencesKey("refresh")

        /** Couleurs du thème de l'application, claires et sombres. */
        private val WidgetColors = ColorProviders(light = LightColorScheme, dark = DarkColorScheme)

        /** Relit les souvenirs de chaque widget « Ce jour-là » posé et le redessine. */
        suspend fun refreshAll(context: Context) {
            GlanceAppWidgetManager(context).getGlanceIds(ThisDayWidget::class.java).forEach { id ->
                updateAppWidgetState(context, id) { it[REFRESH] = (it[REFRESH] ?: 0) + 1 }
                ThisDayWidget().update(context, id)
            }
        }

        /** Choix de montrer une photo du widget dont l'état est [this]. */
        val Preferences.showPhoto: Boolean get() = this[SHOW_PHOTO] ?: true
    }
}
