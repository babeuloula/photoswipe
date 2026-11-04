package fr.babeuloula.photoswipe.ui.permission

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.babeuloula.photoswipe.permission.MEDIA_PERMISSIONS
import fr.babeuloula.photoswipe.permission.MEDIA_PERMISSION_REQUEST

/**
 * Affiche [content] seulement quand l'application peut lire les photos et vidéos ; sinon, le
 * parcours d'accès (explication, demande système, refus). En accès partiel, une bannière surmonte
 * [content]. [modifier] s'applique aux écrans du parcours d'accès ; [content] gère lui-même les
 * marges des barres système. Il exécute les effets de
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
                    permissionRequest.launch(MEDIA_PERMISSION_REQUEST)
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

        is MediaAccessUiState.Granted -> {
            GrantedContent(
                partial = current.partial,
                onManageAccess = { viewModel.onEvent(MediaAccessEvent.ExtendSelection) },
                content = content,
            )
        }
    }
}

/** Contenu de l'application, surmonté de la bannière d'accès limité si l'accès est [partial]. */
@Composable
private fun GrantedContent(
    partial: Boolean,
    onManageAccess: () -> Unit,
    content: @Composable () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        if (partial) {
            LimitedAccessBanner(
                onManageAccess = onManageAccess,
                modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars),
            )
        }
        // La bannière occupe déjà la barre d'état : le contenu ne la réserve pas une seconde fois.
        val insets = if (partial) Modifier.consumeWindowInsets(WindowInsets.statusBars) else Modifier
        Box(Modifier.weight(1f).then(insets)) { content() }
    }
}
