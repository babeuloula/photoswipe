package fr.babeuloula.photoswipe.ui.whatsnew

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import fr.babeuloula.photoswipe.R
import fr.babeuloula.photoswipe.ui.theme.PhotoSwipeTheme
import fr.babeuloula.photoswipe.ui.theme.monoLabel

/**
 * Feuille des nouveautés (PS-173) : sur l'accueil après une mise à jour, les versions manquées ; dans
 * les réglages, toutes. Elle s'ouvre en entier et défile quand les nouveautés dépassent l'écran.
 * Fermée (« Compris », glissement vers le bas, retour), elle appelle [onDismiss].
 *
 * @param releases versions à montrer, de la plus récente à la plus ancienne.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhatsNewSheet(
    releases: List<WhatsNewRelease>,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        WhatsNewContent(releases = releases, onClose = onDismiss)
    }
}

/** Contenu de la feuille : titre, puis chaque version et ses nouveautés, et « Compris » en bas. */
@Composable
internal fun WhatsNewContent(
    releases: List<WhatsNewRelease>,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(start = 24.dp, end = 24.dp, bottom = 24.dp)) {
        Text(
            text = stringResource(R.string.whats_new_title),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.semantics { heading() },
        )
        LazyColumn(
            modifier = Modifier.weight(1f, fill = false).padding(vertical = 16.dp).testTag(WHATS_NEW_LIST_TAG),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            releases.forEach { release ->
                item(key = release.version.toString()) {
                    Text(
                        text = stringResource(R.string.whats_new_version, release.version.toString()),
                        style = MaterialTheme.typography.monoLabel,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 8.dp).semantics { heading() },
                    )
                }
                items(release.items) { item -> WhatsNewRow(item) }
            }
        }
        Button(onClick = onClose, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.whats_new_close))
        }
    }
}

/** Une nouveauté : son icône dans une pastille, son titre et son texte. */
@Composable
private fun WhatsNewRow(item: WhatsNewItem) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.Top) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ) {
            Icon(
                painter = painterResource(item.icon),
                contentDescription = null,
                modifier = Modifier.padding(8.dp).size(24.dp),
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = stringResource(item.title), style = MaterialTheme.typography.titleMedium)
            Text(
                text = stringResource(item.text),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun WhatsNewContentPreview() {
    PhotoSwipeTheme {
        Surface { WhatsNewContent(releases = WHATS_NEW, onClose = {}) }
    }
}

/** Étiquette de test de la liste des nouveautés, qui défile. */
internal const val WHATS_NEW_LIST_TAG = "whats_new_list"
