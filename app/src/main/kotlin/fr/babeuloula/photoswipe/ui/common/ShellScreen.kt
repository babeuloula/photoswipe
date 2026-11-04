package fr.babeuloula.photoswipe.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import fr.babeuloula.photoswipe.R

/**
 * Écran provisoire (« coquille ») : barre du haut avec retour, message d'attente et actions de
 * navigation. Chaque coquille est remplacée par son écran réel dans son ticket.
 *
 * @param onBack retour, `null` pour la destination de départ (pas de flèche).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShellScreen(
    title: String,
    onBack: (() -> Unit)?,
    actions: List<ShellAction>,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.navigate_back),
                            )
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(stringResource(R.string.shell_coming_soon), style = MaterialTheme.typography.bodyLarge)
            actions.forEach { action ->
                Button(onClick = action.onClick, modifier = Modifier.fillMaxWidth()) { Text(action.label) }
            }
        }
    }
}
