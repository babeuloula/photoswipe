package fr.babeuloula.photoswipe.ui.permission

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.babeuloula.photoswipe.permission.MEDIA_PERMISSIONS

/**
 * Affiche [content] seulement quand l'application peut lire les photos et vidéos ; sinon, le
 * parcours d'accès (explication, demande système, refus). Il exécute les effets de
 * [MediaAccessViewModel] : demande système et ouverture des réglages.
 */
@Composable
fun MediaAccessGate(
    modifier: Modifier = Modifier,
    viewModel: MediaAccessViewModel = hiltViewModel(),
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val permissionRequest =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            val activity = context as? Activity
            val canAskAgain =
                activity != null && MEDIA_PERMISSIONS.any { activity.shouldShowRequestPermissionRationale(it) }
            viewModel.onEvent(MediaAccessEvent.RequestAnswered(canAskAgain))
        }

    LifecycleResumeEffect(viewModel) {
        viewModel.onEvent(MediaAccessEvent.Resumed)
        onPauseOrDispose {}
    }
    LaunchedEffect(viewModel) {
        viewModel.uiEffects.collect { effect ->
            when (effect) {
                MediaAccessEffect.LaunchPermissionRequest -> {
                    permissionRequest.launch(MEDIA_PERMISSIONS)
                }

                MediaAccessEffect.OpenAppSettings -> {
                    context.startActivity(
                        Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", context.packageName, null),
                        ),
                    )
                }
            }
        }
    }

    when (val current = state) {
        MediaAccessUiState.Rationale -> {
            PermissionRationaleScreen(
                onContinue = { viewModel.onEvent(MediaAccessEvent.RequestAccess) },
                modifier = modifier,
            )
        }

        is MediaAccessUiState.Denied -> {
            MediaAccessDeniedScreen(
                canAskAgain = current.canAskAgain,
                onOpenSettings = { viewModel.onEvent(MediaAccessEvent.OpenSettings) },
                onTryAgain = { viewModel.onEvent(MediaAccessEvent.RequestAccess) },
                modifier = modifier,
            )
        }

        MediaAccessUiState.Granted -> {
            content()
        }
    }
}
