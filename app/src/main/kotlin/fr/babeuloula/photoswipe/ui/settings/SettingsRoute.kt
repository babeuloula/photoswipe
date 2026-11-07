package fr.babeuloula.photoswipe.ui.settings

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Resources
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
import fr.babeuloula.photoswipe.ui.common.SupportLinks
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
    onOpenTutorial: () -> Unit = {},
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

                SettingsEffect.OpenLanguageSettings,
                SettingsEffect.OpenDeveloperTools,
                SettingsEffect.OpenNotificationSettings,
                SettingsEffect.OpenRating,
                SettingsEffect.OpenDonation,
                -> {
                    if (!openOutside(context, effect, viewModel.developerTools)) {
                        scope.launch { snackbarHostState.showSnackbar(resources.getString(R.string.link_unavailable)) }
                    }
                }

                SettingsEffect.OpenLicenses -> {
                    onOpenLicenses()
                }

                SettingsEffect.OpenTutorial -> {
                    onOpenTutorial()
                }

                SettingsEffect.RequestNotificationPermission -> {
                    permissionRequest.launch(NOTIFICATION_PERMISSION)
                }

                is SettingsEffect.NotificationPermissionDenied -> {
                    scope.launch {
                        if (snackbarHostState.showDenied(resources, effect.canAskAgain)) {
                            viewModel.onEvent(SettingsEvent.OpenNotificationSettings)
                        }
                    }
                }
            }
        }
    }
    SettingsScreen(
        state = state,
        onEvent = viewModel::onEvent,
        modifier = modifier,
        environment =
            SettingsEnvironment(
                languageLabel = rememberLanguageLabel(),
                showDeveloperTools = remember(viewModel) { viewModel.developerTools.intent(context) != null },
            ),
        snackbarHostState = snackbarHostState,
    )
}

/**
 * Affiche le refus des notifications, avec « Réglages » quand Android ne redemandera plus
 * ([canAskAgain] faux) ; renvoie vrai si l'utilisateur a choisi les réglages.
 */
private suspend fun SnackbarHostState.showDenied(
    resources: Resources,
    canAskAgain: Boolean,
): Boolean =
    showSnackbar(
        resources.getString(R.string.settings_reminder_permission_denied),
        actionLabel = resources.getString(R.string.settings_reminder_open_settings).takeUnless { canAskAgain },
        duration = SnackbarDuration.Long,
    ) == SnackbarResult.ActionPerformed

/**
 * Ouvre l'écran hors de l'application demandé par [effect] : réglage d'Android, outils
 * développeur, fiche du Play Store ou page de don (ADR 0029). Renvoie `false` si rien ne peut
 * l'ouvrir.
 */
private fun openOutside(
    context: Context,
    effect: SettingsEffect,
    developerTools: DeveloperToolsEntry,
): Boolean =
    when (effect) {
        SettingsEffect.OpenRating -> {
            SupportLinks.openRating(context)
        }

        SettingsEffect.OpenDonation -> {
            SupportLinks.openDonation(context)
        }

        else -> {
            otherScreenIntent(context, effect, developerTools)?.let(context::startActivity) != null
        }
    }

/** Écran hors de l'application à ouvrir pour [effect] : réglage d'Android ou outils développeur. */
private fun otherScreenIntent(
    context: Context,
    effect: SettingsEffect,
    developerTools: DeveloperToolsEntry,
): Intent? =
    when (effect) {
        SettingsEffect.OpenLanguageSettings -> {
            appLanguageSettingsIntent(context)
        }

        SettingsEffect.OpenDeveloperTools -> {
            developerTools.intent(context)
        }

        SettingsEffect.OpenNotificationSettings -> {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        }

        else -> {
            null
        }
    }

/**
 * Langue de l'application, ou celle du téléphone (SPEC §9). Une langue choisie dans les réglages
 * d'Android recrée l'activité : la configuration change et le libellé est relu.
 */
@Composable
private fun rememberLanguageLabel(): String {
    val context = LocalContext.current
    val resources = LocalResources.current
    val configuration = LocalConfiguration.current
    return remember(configuration) {
        appLanguage(context)?.nativeName()
            ?: resources.getString(R.string.settings_language_system, configuration.locales[0].nativeName())
    }
}
