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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import fr.babeuloula.photoswipe.R
import fr.babeuloula.photoswipe.ui.theme.PhotoSwipeTheme

/**
 * Écran affiché après un refus d'accès (SPEC §4.1) : il explique que l'application ne peut rien
 * montrer sans les photos et vidéos et mène aux réglages système. Si Android accepte encore
 * d'afficher sa demande ([canAskAgain]), un second bouton la relance.
 */
@Composable
fun MediaAccessDeniedScreen(
    canAskAgain: Boolean,
    onOpenSettings: () -> Unit,
    onTryAgain: () -> Unit,
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
            text = stringResource(R.string.media_access_denied_title),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.semantics { heading() },
        )
        Text(text = stringResource(R.string.media_access_denied_body), style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.weight(1f))
        Button(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.media_access_open_settings))
        }
        if (canAskAgain) {
            OutlinedButton(onClick = onTryAgain, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.media_access_try_again))
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun MediaAccessDeniedScreenPreview() {
    PhotoSwipeTheme(dynamicColor = false) {
        MediaAccessDeniedScreen(canAskAgain = true, onOpenSettings = {}, onTryAgain = {})
    }
}
