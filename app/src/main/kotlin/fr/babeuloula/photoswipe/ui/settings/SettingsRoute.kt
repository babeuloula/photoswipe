package fr.babeuloula.photoswipe.ui.settings

import android.app.Activity
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.babeuloula.photoswipe.R
import fr.babeuloula.photoswipe.locale.appLanguage
import fr.babeuloula.photoswipe.locale.appLanguageSettingsIntent
import fr.babeuloula.photoswipe.locale.nativeName
import fr.babeuloula.photoswipe.permission.NOTIFICATION_PERMISSION
import kotlinx.coroutines.launch

/**
 * Réglages reliés à leur ViewModel : la route exécute les navigations, demande la permission de
 * notification et explique un refus, avec un raccourci vers les réglages système quand Android
 * ne la redemandera plus (SPEC §3.6). Elle lit aussi la langue de l'application, relue après le
 * changement de langue qui recrée l'activité (SPEC §9).
 */
@Composable
fun SettingsRoute(
    onBack: () -> Unit,
    onOpenLicenses: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val resources = LocalResources.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val permissionRequest =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            val canAskAgain =
                (context as? Activity)?.shouldShowRequestPermissionRationale(NOTIFICATION_PERMISSION) == true
            viewModel.onEvent(SettingsEvent.NotificationPermissionAnswered(granted, canAskAgain))
        }
    LaunchedEffect(viewModel) {
        viewModel.uiEffects.collect { effect ->
            when (effect) {
                SettingsEffect.Close -> {
                    onBack()
                }

                SettingsEffect.OpenLanguageSettings -> {
                    context.startActivity(appLanguageSettingsIntent(context))
                }

                SettingsEffect.OpenLicenses -> {
                    onOpenLicenses()
                }

                SettingsEffect.RequestNotificationPermission -> {
                    permissionRequest.launch(NOTIFICATION_PERMISSION)
                }

                is SettingsEffect.NotificationPermissionDenied -> {
                    val message = resources.getString(R.string.settings_reminder_permission_denied)
                    val action = resources.getString(R.string.settings_reminder_open_settings)
                    scope.launch {
                        if (snackbarHostState.showDenied(message, action.takeUnless { effect.canAskAgain })) {
                            viewModel.onEvent(SettingsEvent.OpenNotificationSettings)
                        }
                    }
                }

                SettingsEffect.OpenNotificationSettings -> {
                    context.startActivity(
                        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
                    )
                }
            }
        }
    }
    // Une langue choisie dans les réglages d'Android recrée l'activité : la configuration change.
    val configuration = LocalConfiguration.current
    val languageLabel =
        remember(configuration) {
            appLanguage(context)?.nativeName()
                ?: resources.getString(R.string.settings_language_system, configuration.locales[0].nativeName())
        }
    SettingsScreen(
        state = state,
        onEvent = viewModel::onEvent,
        modifier = modifier,
        languageLabel = languageLabel,
        snackbarHostState = snackbarHostState,
    )
}

/** Affiche le refus des notifications ; renvoie vrai si l'utilisateur a choisi [action] (les réglages). */
private suspend fun SnackbarHostState.showDenied(
    message: String,
    action: String?,
): Boolean =
    showSnackbar(message, actionLabel = action, duration = SnackbarDuration.Long) == SnackbarResult.ActionPerformed
