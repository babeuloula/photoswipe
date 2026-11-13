package fr.babeuloula.photoswipe.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import fr.babeuloula.photoswipe.domain.model.ThemeMode
import fr.babeuloula.photoswipe.domain.port.SettingsRepository
import fr.babeuloula.photoswipe.ui.theme.PhotoSwipeTheme
import fr.babeuloula.photoswipe.ui.theme.isDark
import fr.babeuloula.photoswipe.ui.widget.WidgetSettingsScreen
import fr.babeuloula.photoswipe.widget.ThisDayWidget.Companion.showPhoto
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

/**
 * Activité de configuration du widget « Ce jour-là » (PS-170), ouverte par Android à la pose du
 * widget et par « Réglages du widget » ensuite : le choix de montrer une photo, enregistré dans
 * l'état Glance du widget. Fermée sans « Terminé » à la pose, le widget n'est pas ajouté.
 */
@AndroidEntryPoint
class ThisDayWidgetConfigureActivity : ComponentActivity() {
    /** Réglages, lus pour le thème. */
    @Inject
    lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val appWidgetId =
            intent?.extras?.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
                ?: AppWidgetManager.INVALID_APPWIDGET_ID
        // Annulée tant que l'utilisateur n'a pas validé : Android retire alors le widget posé.
        setResult(RESULT_CANCELED, resultIntent(appWidgetId))
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }
        val glanceId = GlanceAppWidgetManager(this).getGlanceIdBy(appWidgetId)
        // Lectures courtes de petits fichiers locaux : l'écran s'ouvre dans le bon thème, sur le choix actuel.
        val themeMode = runBlocking { settingsRepository.observeThemeMode().catch { emit(ThemeMode.DEFAULT) }.first() }
        val savedShowPhoto =
            runBlocking {
                getAppWidgetState(
                    this@ThisDayWidgetConfigureActivity,
                    PreferencesGlanceStateDefinition,
                    glanceId,
                )
            }.showPhoto
        setContent {
            var showPhoto by rememberSaveable { mutableStateOf(savedShowPhoto) }
            PhotoSwipeTheme(darkTheme = themeMode.isDark(isSystemInDarkTheme())) {
                WidgetSettingsScreen(
                    showPhoto = showPhoto,
                    onShowPhotoChange = { showPhoto = it },
                    onDone = {
                        lifecycleScope.launch {
                            updateAppWidgetState(this@ThisDayWidgetConfigureActivity, glanceId) {
                                it[ThisDayWidget.SHOW_PHOTO] = showPhoto
                            }
                            ThisDayWidget().update(this@ThisDayWidgetConfigureActivity, glanceId)
                            setResult(RESULT_OK, resultIntent(appWidgetId))
                            finish()
                        }
                    },
                )
            }
        }
    }

    private fun resultIntent(appWidgetId: Int): Intent =
        Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
}
