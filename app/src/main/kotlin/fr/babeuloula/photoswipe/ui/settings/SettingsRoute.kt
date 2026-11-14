package fr.babeuloula.photoswipe.ui.settings

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.babeuloula.photoswipe.R
import fr.babeuloula.photoswipe.locale.appLanguage
import fr.babeuloula.photoswipe.locale.appLanguageSettingsIntent
import fr.babeuloula.photoswipe.locale.nativeName
import fr.babeuloula.photoswipe.permission.ExactAlarmPermissionReader
import fr.babeuloula.photoswipe.permission.NOTIFICATION_PERMISSION
import fr.babeuloula.photoswipe.ui.common.SupportLinks
import fr.babeuloula.photoswipe.ui.deck.hasAccelerometer
import kotlinx.coroutines.launch

/**
 * Réglages reliés à leur ViewModel : la route exécute les navigations, demande la permission de
 * notification et explique un refus, avec un raccourci vers les réglages système quand Android
 * ne la redemandera plus (SPEC §3.6). Elle lit aussi la langue de l'application, relue après le
 * changement de langue qui recrée l'activité (SPEC §9).
 */
@Composable
fun SettingsRoute(
    navigation: SettingsNavigation,
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
            if (navigation.navigate(effect)) return@collect
            when (effect) {
                SettingsEffect.OpenLanguageSettings,
                SettingsEffect.OpenExactAlarmSettings,
                SettingsEffect.OpenDeveloperTools,
                SettingsEffect.OpenNotificationSettings,
                SettingsEffect.OpenRating,
                SettingsEffect.OpenDonation,
                SettingsEffect.OpenSourceCode,
                -> {
                    if (!openOutside(context, effect, viewModel.developerTools)) {
                        scope.launch { snackbarHostState.showSnackbar(resources.getString(R.string.link_unavailable)) }
                    }
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

                else -> {
                    Unit
                }
            }
        }
    }
    SettingsScreen(
        state = state,
        onEvent = viewModel::onEvent,
        modifier = modifier,
        environment = rememberEnvironment(viewModel),
        snackbarHostState = snackbarHostState,
    )
}

/**
 * Navigations demandées par les réglages, exécutées par le graphe de navigation.
 *
 * @property back revient à l'écran précédent.
 * @property openLicenses ouvre les licences.
 * @property openTutorial rouvre le tutoriel.
 * @property openProgress ouvre la page « Progression » (PS-153).
 * @property openTrash ouvre la page « Corbeille » (PS-168).
 */
class SettingsNavigation(
    val back: () -> Unit,
    val openLicenses: () -> Unit = {},
    val openTutorial: () -> Unit = {},
    val openProgress: () -> Unit = {},
    val openTrash: () -> Unit = {},
)

/** Exécute [effect] s'il mène à un autre écran de l'application ; renvoie `false` sinon. */
private fun SettingsNavigation.navigate(effect: SettingsEffect): Boolean {
    when (effect) {
        SettingsEffect.Close -> back()
        SettingsEffect.OpenLicenses -> openLicenses()
        SettingsEffect.OpenTutorial -> openTutorial()
        SettingsEffect.OpenProgress -> openProgress()
        SettingsEffect.OpenTrash -> openTrash()
        else -> return false
    }
    return true
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
 * développeur, fiche du Play Store, page de don ou code source (ADR 0029). Renvoie `false` si rien ne peut
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

        SettingsEffect.OpenSourceCode -> {
            SupportLinks.openSourceCode(context)
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
            // Le réglage de langue par application n'existe qu'à partir d'Android 13 (PS-181).
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) appLanguageSettingsIntent(context) else null
        }

        SettingsEffect.OpenDeveloperTools -> {
            developerTools.intent(context)
        }

        SettingsEffect.OpenExactAlarmSettings -> {
            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.fromParts("package", context.packageName, null))
        }

        SettingsEffect.OpenNotificationSettings -> {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        }

        else -> {
            null
        }
    }

/**
 * Ce que les réglages lisent hors de leur ViewModel : langue, outils développeur, alarmes exactes,
 * présence d'un accéléromètre.
 */
@Composable
private fun rememberEnvironment(viewModel: SettingsViewModel): SettingsEnvironment {
    val context = LocalContext.current
    return SettingsEnvironment(
        languageLabel = rememberLanguageLabel(),
        showDeveloperTools = remember(viewModel) { viewModel.developerTools.intent(context) != null },
        exactAlarmAllowed = rememberExactAlarmAllowed(viewModel.exactAlarms),
        shakeAvailable = remember(context) { hasAccelerometer(context) },
    )
}

/**
 * Autorisation des alarmes exactes, relue à chaque retour au premier plan : l'utilisateur la donne
 * dans les réglages d'Android (PS-125).
 */
@Composable
private fun rememberExactAlarmAllowed(reader: ExactAlarmPermissionReader): Boolean {
    var allowed by remember(reader) { mutableStateOf(reader.isGranted()) }
    LifecycleResumeEffect(reader) {
        allowed = reader.isGranted()
        onPauseOrDispose {}
    }
    return allowed
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
