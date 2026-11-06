package fr.babeuloula.photoswipe.ui.tutorial

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import fr.babeuloula.photoswipe.R
import fr.babeuloula.photoswipe.ui.common.CenteredContent
import fr.babeuloula.photoswipe.ui.theme.PhotoSwipeTheme
import kotlinx.coroutines.launch

/**
 * Tutoriel en carrousel (SPEC §4) : une page par [pages], un indicateur de page, « Passer » et
 * « Suivant » sur chaque page, « C'est parti » sur la dernière. On change de page en glissant ou
 * avec les boutons ; la page affichée survit à une recréation de l'activité.
 *
 * @param onDone appelé par « Passer » ou par « C'est parti ».
 * @param animate faux pour des illustrations statiques.
 */
@Composable
fun TutorialScreen(
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    pages: List<TutorialPage> = TutorialPage.entries,
    animate: Boolean = true,
) {
    val pagerState = rememberPagerState { pages.size }
    val scope = rememberCoroutineScope()
    val isLast = pagerState.currentPage == pages.lastIndex
    CenteredContent(modifier) {
        Column(Modifier.fillMaxSize().padding(vertical = 24.dp)) {
            HorizontalPager(state = pagerState, modifier = Modifier.weight(1f).fillMaxWidth()) { index ->
                TutorialPageContent(pages[index], animate = animate)
            }
            PageIndicator(
                current = pagerState.currentPage,
                count = pages.size,
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(vertical = 16.dp),
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (isLast) {
                    Spacer(Modifier)
                } else {
                    TextButton(onClick = onDone, modifier = Modifier.heightIn(min = 48.dp)) {
                        Text(stringResource(R.string.tutorial_skip))
                    }
                }
                Button(
                    onClick = {
                        if (isLast) {
                            onDone()
                        } else {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        }
                    },
                    modifier = Modifier.heightIn(min = 56.dp).widthIn(min = 120.dp),
                ) {
                    Text(stringResource(if (isLast) R.string.tutorial_start else R.string.tutorial_next))
                }
            }
        }
    }
}

/** Contenu d'une page : illustration ([TutorialIllustration]), titre et texte, défilables pour une grande police. */
@Composable
private fun TutorialPageContent(
    page: TutorialPage,
    animate: Boolean,
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        TutorialIllustration(
            page = page,
            animate = animate,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(ILLUSTRATION_RATIO)
                    .clip(MaterialTheme.shapes.extraLarge)
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .clearAndSetSemantics {},
        )
        Text(
            text = stringResource(page.title),
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            text = stringResource(page.body),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Points de l'indicateur de page, lus par TalkBack comme « Page 2 sur 5 ». */
@Composable
private fun PageIndicator(
    current: Int,
    count: Int,
    modifier: Modifier = Modifier,
) {
    val description = stringResource(R.string.tutorial_page_indicator, current + 1, count)
    Row(
        modifier = modifier.clearAndSetSemantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        repeat(count) { index ->
            val selected = index == current
            Box(
                Modifier
                    .size(width = if (selected) 24.dp else 8.dp, height = 8.dp)
                    .clip(CircleShape)
                    .background(
                        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                    ),
            )
        }
    }
}

/** Proportions de l'illustration d'une page. */
private const val ILLUSTRATION_RATIO = 4f / 3f

@PreviewLightDark
@Composable
private fun TutorialScreenPreview() {
    PhotoSwipeTheme {
        TutorialScreen(onDone = {})
    }
}
