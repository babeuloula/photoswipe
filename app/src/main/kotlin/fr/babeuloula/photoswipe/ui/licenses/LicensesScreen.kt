package fr.babeuloula.photoswipe.ui.licenses

import androidx.annotation.RawRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import fr.babeuloula.photoswipe.R
import fr.babeuloula.photoswipe.ui.common.CenteredContent
import fr.babeuloula.photoswipe.ui.theme.PhotoSwipeTheme

/**
 * Licences (« À propos » des réglages) : la licence de PhotoSwipe, les bibliothèques et polices
 * embarquées, puis le texte complet des licences Apache 2.0 et SIL Open Font 1.1. Écran statique,
 * sans ViewModel : il n'a ni état ni donnée à charger.
 *
 * @param licenseText texte de la licence Apache 2.0 ; lu par défaut dans `res/raw`.
 * @param fontLicenseText texte de la licence SIL Open Font 1.1 ; lu par défaut dans `res/raw`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LicensesScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    licenseText: String = rawText(R.raw.apache_license_2_0),
    fontLicenseText: String = rawText(R.raw.sil_open_font_license_1_1),
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.licenses_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painterResource(R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.navigate_back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        CenteredContent(Modifier.padding(innerPadding)) {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.app_name)) },
                    supportingContent = { Text(stringResource(R.string.licenses_app_notice)) },
                )
                SectionTitle(stringResource(R.string.licenses_libraries))
                OPEN_SOURCE_LIBRARIES.forEach { library ->
                    ListItem(
                        headlineContent = { Text(library.name) },
                        supportingContent = {
                            Text(
                                stringResource(
                                    R.string.licenses_library_notice,
                                    library.author,
                                    licenseName(library.license),
                                ),
                            )
                        },
                    )
                }
                HorizontalDivider()
                SectionTitle(stringResource(R.string.licenses_apache_text))
                LicenseText(licenseText)
                HorizontalDivider()
                SectionTitle(stringResource(R.string.licenses_ofl_text))
                LicenseText(fontLicenseText)
            }
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

/** Texte complet d'une licence, en chasse fixe pour garder sa mise en page. */
@Composable
private fun LicenseText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        fontFamily = FontFamily.Monospace,
        modifier = Modifier.padding(16.dp),
    )
}

/** Nom localisé de [license]. */
@Composable
private fun licenseName(license: OpenSourceLicense): String =
    stringResource(
        when (license) {
            OpenSourceLicense.APACHE_2_0 -> R.string.licenses_name_apache
            OpenSourceLicense.OFL_1_1 -> R.string.licenses_name_ofl
        },
    )

/** Texte d'une licence, lu une fois dans `res/raw` (en anglais, langue de la licence). */
@Composable
private fun rawText(
    @RawRes id: Int,
): String {
    val resources = LocalResources.current
    return remember(resources, id) { resources.openRawResource(id).bufferedReader().use { it.readText() } }
}

@PreviewLightDark
@Composable
private fun LicensesScreenPreview() {
    PhotoSwipeTheme {
        LicensesScreen(
            onBack = {},
            licenseText = "Apache License, Version 2.0",
            fontLicenseText = "SIL Open Font License",
        )
    }
}
