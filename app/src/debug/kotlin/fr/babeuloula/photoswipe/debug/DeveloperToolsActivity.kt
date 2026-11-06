package fr.babeuloula.photoswipe.debug

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dagger.hilt.android.AndroidEntryPoint
import fr.babeuloula.photoswipe.R
import fr.babeuloula.photoswipe.ui.theme.PhotoSwipeTheme
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Outils développeur (SPEC §10), présents seulement dans la variante debug : une entrée à part
 * dans le lanceur, déclarée par le manifeste debug, et une entrée dans les réglages. Ils activent
 * les données de démo et remettent la base des décisions à zéro.
 */
@AndroidEntryPoint
class DeveloperToolsActivity : ComponentActivity() {
    @Inject
    lateinit var settings: DeveloperSettings

    @Inject
    lateinit var decisionsReset: DecisionsReset

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            PhotoSwipeTheme {
                DeveloperToolsScreen(
                    initialDemoData = settings.demoDataEnabled,
                    onDemoDataChange = { settings.demoDataEnabled = it },
                    onResetDecisions = decisionsReset::resetAll,
                )
            }
        }
    }
}

/** Écran des outils : données de démo, puis remise à zéro des décisions, confirmée par un dialogue. */
@Composable
internal fun DeveloperToolsScreen(
    initialDemoData: Boolean,
    onDemoDataChange: (Boolean) -> Unit,
    onResetDecisions: suspend () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val resources = LocalResources.current
    var confirmingReset by remember { mutableStateOf(false) }
    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .safeDrawingPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Text(
                text = stringResource(R.string.developer_tools_title),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.semantics { heading() },
            )
            DemoDataRow(initialDemoData, onDemoDataChange)
            ToolDescription(R.string.developer_tools_reset, R.string.developer_tools_reset_hint)
            Button(
                onClick = { confirmingReset = true },
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
            ) { Text(stringResource(R.string.developer_tools_reset_action)) }
        }
    }
    if (confirmingReset) {
        AlertDialog(
            onDismissRequest = { confirmingReset = false },
            title = { Text(stringResource(R.string.developer_tools_reset_confirm_title)) },
            text = { Text(stringResource(R.string.developer_tools_reset_confirm_text)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmingReset = false
                    scope.launch {
                        onResetDecisions()
                        snackbarHostState.showSnackbar(resources.getString(R.string.developer_tools_reset_done))
                    }
                }) { Text(stringResource(R.string.developer_tools_reset_action)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmingReset = false }) {
                    Text(stringResource(R.string.developer_tools_reset_cancel))
                }
            },
        )
    }
}

/** Bascule des données de démo. */
@Composable
private fun DemoDataRow(
    initialDemoData: Boolean,
    onDemoDataChange: (Boolean) -> Unit,
) {
    var demoData by remember { mutableStateOf(initialDemoData) }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        ToolDescription(
            R.string.developer_tools_demo_data,
            R.string.developer_tools_demo_data_hint,
            Modifier.weight(1f),
        )
        Switch(
            checked = demoData,
            onCheckedChange = {
                demoData = it
                onDemoDataChange(it)
            },
        )
    }
}

/** Nom et explication d'un outil. */
@Composable
private fun ToolDescription(
    title: Int,
    hint: Int,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        Text(stringResource(title), style = MaterialTheme.typography.titleMedium)
        Text(stringResource(hint), style = MaterialTheme.typography.bodyMedium)
    }
}
