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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dagger.hilt.android.AndroidEntryPoint
import fr.babeuloula.photoswipe.R
import fr.babeuloula.photoswipe.ui.theme.PhotoSwipeTheme
import javax.inject.Inject

/**
 * Outils développeur (SPEC §10), présents seulement dans la variante debug : une entrée à part
 * dans le lanceur, déclarée par le manifeste debug. Ils activent les données de démo.
 */
@AndroidEntryPoint
class DeveloperToolsActivity : ComponentActivity() {
    @Inject
    lateinit var settings: DeveloperSettings

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            PhotoSwipeTheme {
                DeveloperToolsScreen(initialDemoData = settings.demoDataEnabled) { settings.demoDataEnabled = it }
            }
        }
    }
}

@Composable
private fun DeveloperToolsScreen(
    initialDemoData: Boolean,
    onDemoDataChange: (Boolean) -> Unit,
) {
    var demoData by remember { mutableStateOf(initialDemoData) }
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.safeDrawingPadding().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.developer_tools_title),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.semantics { heading() },
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.developer_tools_demo_data),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        stringResource(R.string.developer_tools_demo_data_hint),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Switch(
                    checked = demoData,
                    onCheckedChange = {
                        demoData = it
                        onDemoDataChange(it)
                    },
                )
            }
        }
    }
}
