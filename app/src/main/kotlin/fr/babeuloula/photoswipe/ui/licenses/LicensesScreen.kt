package fr.babeuloula.photoswipe.ui.licenses

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import fr.babeuloula.photoswipe.R
import fr.babeuloula.photoswipe.ui.theme.PhotoSwipeTheme

/**
 * Licences (« À propos » des réglages) : la licence de PhotoSwipe, les bibliothèques embarquées,
 * puis le texte complet de la licence Apache 2.0 qu'elles partagent. Écran statique, sans
 * ViewModel : il n'a ni état ni donnée à charger.
 *
 * @param licenseText texte de la licence ; lu par défaut dans `res/raw`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LicensesScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    licenseText: String = apacheLicenseText(),
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.licenses_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.navigate_back),
                        )
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
                    .verticalScroll(rememberScrollState()),
        ) {
            ListItem(
                headlineContent = { Text(stringResource(R.string.app_name)) },
                supportingContent = { Text(stringResource(R.string.licenses_app_notice)) },
            )
            SectionTitle(stringResource(R.string.licenses_libraries))
            OPEN_SOURCE_LIBRARIES.forEach { library ->
                ListItem(
                    headlineContent = { Text(library.name) },
                    supportingContent = { Text(stringResource(R.string.licenses_library_notice, library.author)) },
                )
            }
            HorizontalDivider()
            SectionTitle(stringResource(R.string.licenses_apache_text))
            Text(
                text = licenseText,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

/** Titre de section, annoncé comme tel par TalkBack. */
@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier =
            Modifier
                .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 8.dp)
                .semantics { heading() },
    )
}

/** Texte de la licence Apache 2.0, lu une fois dans `res/raw` (en anglais, langue de la licence). */
@Composable
private fun apacheLicenseText(): String {
    val resources = LocalResources.current
    return remember(resources) {
        resources.openRawResource(R.raw.apache_license_2_0).bufferedReader().use { it.readText() }
    }
}

@PreviewLightDark
@Composable
private fun LicensesScreenPreview() {
    PhotoSwipeTheme { LicensesScreen(onBack = {}, licenseText = "Apache License, Version 2.0") }
}
