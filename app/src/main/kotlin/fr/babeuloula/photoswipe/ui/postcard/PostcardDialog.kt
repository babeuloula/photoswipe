package fr.babeuloula.photoswipe.ui.postcard

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import fr.babeuloula.photoswipe.R
import fr.babeuloula.photoswipe.ui.deck.launchShare
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Carte postale de la photo [uri] (PS-177) : la lit à taille bornée, compose la carte avec le
 * repère [label], la montre en aperçu, puis la partage. [onDone] reçoit le message à afficher si
 * Android refuse le partage, `null` sinon ; il est aussi appelé, sans message, à l'abandon.
 */
@Composable
fun PostcardDialog(
    uri: String,
    label: String,
    onDone: (failure: String?) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var withBrand by rememberSaveable { mutableStateOf(true) }
    val style = remember(context) { PostcardStyle.of(context) }
    val brand = stringResource(R.string.app_name)
    val photo by produceState<Result<Bitmap>?>(null, uri) {
        value =
            withContext(Dispatchers.IO) { runCatching { decodePostcardPhoto(context.contentResolver, Uri.parse(uri)) } }
    }
    val postcard by produceState<Bitmap?>(null, photo, withBrand) {
        value = null
        val source = photo?.getOrNull() ?: return@produceState
        value = withContext(Dispatchers.Default) { renderPostcard(source, label, brand.takeIf { withBrand }, style) }
    }
    val preview =
        when {
            photo?.isFailure == true -> PostcardPreview.Failed
            else -> postcard?.let { PostcardPreview.Ready(it.asImageBitmap(), label) } ?: PostcardPreview.Loading
        }
    val chooserTitle = stringResource(R.string.deck_share)
    val failure = stringResource(R.string.deck_share_failed)
    PostcardPreviewDialog(
        preview = preview,
        withBrand = withBrand,
        onWithBrandChange = { withBrand = it },
        onShare = {
            val ready = postcard ?: return@PostcardPreviewDialog
            scope.launch {
                val shared =
                    runCatching {
                        val card =
                            withContext(
                                Dispatchers.IO,
                            ) { PostcardFiles(context).let { it.shareableUri(it.write(ready)) } }
                        launchShare(postcardShareIntent(card, chooserTitle), context::startActivity)
                    }.getOrDefault(false)
                onDone(if (shared) null else failure)
            }
        },
        onDismiss = { onDone(null) },
    )
}

/**
 * Aperçu de la carte postale avant l'envoi : la carte, l'interrupteur de la mention « PhotoSwipe »
 * et « Partager ». Sans état, pour les aperçus et les tests.
 */
@Composable
fun PostcardPreviewDialog(
    preview: PostcardPreview,
    withBrand: Boolean,
    onWithBrandChange: (Boolean) -> Unit,
    onShare: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.postcard_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                PostcardImage(preview)
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .toggleable(value = withBrand, role = Role.Switch, onValueChange = onWithBrandChange),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(
                        text = stringResource(R.string.postcard_brand),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f),
                    )
                    Switch(checked = withBrand, onCheckedChange = null)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onShare, enabled = preview is PostcardPreview.Ready) {
                Text(stringResource(R.string.deck_share))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.postcard_cancel)) }
        },
    )
}

/** La carte en aperçu, à sa forme ; une roue pendant la préparation, un message si elle échoue. */
@Composable
private fun PostcardImage(preview: PostcardPreview) {
    when (preview) {
        is PostcardPreview.Ready -> {
            Image(
                bitmap = preview.image,
                contentDescription = stringResource(R.string.postcard_preview, preview.label),
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp),
            )
        }

        PostcardPreview.Loading -> {
            Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        PostcardPreview.Failed -> {
            Text(
                text = stringResource(R.string.postcard_failed),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}
