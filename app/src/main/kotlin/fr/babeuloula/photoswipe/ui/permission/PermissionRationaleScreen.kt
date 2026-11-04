package fr.babeuloula.photoswipe.ui.permission

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.babeuloula.photoswipe.R
import fr.babeuloula.photoswipe.ui.theme.PhotoSwipeTheme

/**
 * Écran d'explication relié à son ViewModel : il exécute l'effet [PermissionRationaleEffect]
 * en appelant [onRequestMediaAccess].
 */
@Composable
fun PermissionRationaleRoute(
    onRequestMediaAccess: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PermissionRationaleViewModel = viewModel(),
) {
    LaunchedEffect(viewModel) {
        viewModel.uiEffects.collect { effect ->
            when (effect) {
                PermissionRationaleEffect.RequestMediaAccess -> onRequestMediaAccess()
            }
        }
    }
    PermissionRationaleScreen(
        onContinue = { viewModel.onEvent(PermissionRationaleEvent.Continue) },
        modifier = modifier,
    )
}

/**
 * Écran court qui explique, avant la demande système, ce que fait l'application, pourquoi elle a
 * besoin des photos et vidéos, et que rien ne quitte le téléphone (SPEC §4.1).
 */
@Composable
fun PermissionRationaleScreen(
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Text(
            text = stringResource(R.string.permission_rationale_title),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.semantics { heading() },
        )
        listOf(
            R.string.permission_rationale_what,
            R.string.permission_rationale_why,
            R.string.permission_rationale_private,
        ).forEach { text -> Text(text = stringResource(text), style = MaterialTheme.typography.bodyLarge) }
        Spacer(Modifier.weight(1f))
        Button(onClick = onContinue, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.permission_rationale_continue))
        }
    }
}

@PreviewLightDark
@Composable
private fun PermissionRationaleScreenPreview() {
    PhotoSwipeTheme(dynamicColor = false) {
        PermissionRationaleScreen(onContinue = {})
    }
}
