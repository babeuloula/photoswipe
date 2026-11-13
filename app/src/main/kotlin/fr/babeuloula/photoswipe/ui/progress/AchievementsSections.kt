package fr.babeuloula.photoswipe.ui.progress

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import fr.babeuloula.photoswipe.R
import fr.babeuloula.photoswipe.domain.model.Badge
import fr.babeuloula.photoswipe.domain.model.BadgeKind
import fr.babeuloula.photoswipe.domain.model.BadgeStatus
import fr.babeuloula.photoswipe.domain.model.GIGABYTE
import fr.babeuloula.photoswipe.domain.model.GoalProgress
import fr.babeuloula.photoswipe.ui.common.segmentShape
import fr.babeuloula.photoswipe.ui.deck.fileSizeLabel
import fr.babeuloula.photoswipe.ui.settings.PreferenceRow
import fr.babeuloula.photoswipe.ui.settings.PreferenceSection
import fr.babeuloula.photoswipe.ui.settings.TrailingIcon

/**
 * Section « Objectif » de la page « Progression » (PS-158) : sans objectif, une ligne pour s'en
 * fixer un ; sinon la place libérée depuis qu'il est fixé, sa barre, et « Changer » / « Retirer ».
 */
@Composable
internal fun GoalSection(
    goal: GoalProgress?,
    onEvent: (ProgressEvent) -> Unit,
) {
    PreferenceSection(stringResource(R.string.progress_goal_section)) {
        if (goal == null) {
            PreferenceRow(
                shape = segmentShape(0, 1),
                title = stringResource(R.string.progress_goal_none),
                summary = stringResource(R.string.progress_goal_none_summary),
                trailing = { TrailingIcon(R.drawable.ic_chevron_right) },
                modifier = Modifier.clickable(role = Role.Button) { onEvent(ProgressEvent.PickGoal) },
            )
        } else {
            val context = LocalContext.current
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(segmentShape(0, 1))
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(
                    Modifier.semantics(mergeDescendants = true) {},
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        stringResource(
                            if (goal.reached) R.string.progress_goal_reached else R.string.progress_goal_progress,
                            fileSizeLabel(context, goal.freedBytes),
                            fileSizeLabel(context, goal.goal.targetBytes),
                        ),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    LinearProgressIndicator(progress = { goal.fraction }, modifier = Modifier.fillMaxWidth())
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { onEvent(ProgressEvent.PickGoal) }) {
                        Text(stringResource(R.string.progress_goal_change))
                    }
                    TextButton(onClick = { onEvent(ProgressEvent.RemoveGoal) }) {
                        Text(stringResource(R.string.progress_goal_remove))
                    }
                }
            }
        }
    }
}

/** Choix de l'objectif : quelques tailles de place à libérer, en boutons radio. */
@Composable
internal fun GoalDialog(onEvent: (ProgressEvent) -> Unit) {
    val context = LocalContext.current
    var picked by rememberSaveable { mutableLongStateOf(GOAL_CHOICES[1]) }
    AlertDialog(
        onDismissRequest = { onEvent(ProgressEvent.GoalDialogDismissed) },
        title = { Text(stringResource(R.string.progress_goal_dialog_title)) },
        text = {
            Column(Modifier.selectableGroup()) {
                GOAL_CHOICES.forEach { bytes ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .selectable(selected = picked == bytes, role = Role.RadioButton) { picked = bytes }
                                .padding(vertical = 4.dp),
                    ) {
                        RadioButton(selected = picked == bytes, onClick = null)
                        Text(fileSizeLabel(context, bytes), modifier = Modifier.padding(start = 12.dp))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onEvent(ProgressEvent.GoalPicked(picked)) },
            ) { Text(stringResource(android.R.string.ok)) }
        },
        dismissButton = {
            TextButton(onClick = { onEvent(ProgressEvent.GoalDialogDismissed) }) {
                Text(stringResource(android.R.string.cancel))
            }
        },
    )
}

/**
 * Section « Badges » (PS-158) : chaque badge avec son nom, puis « Obtenu » ou ce qu'il manque
 * pour l'obtenir. Un badge obtenu a une icône pleine en couleur, les autres un cadenas.
 */
@Composable
internal fun BadgesSection(badges: List<BadgeStatus>) {
    val earned = badges.count { it.earned }
    PreferenceSection(stringResource(R.string.progress_badges_section, earned, badges.size)) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            badges.forEachIndexed { index, status ->
                PreferenceRow(
                    shape = segmentShape(index, badges.size),
                    title = stringResource(badgeName(status.badge)),
                    summary = badgeSummary(status),
                    leading = {
                        Icon(
                            painterResource(if (status.earned) R.drawable.ic_badge_filled else R.drawable.ic_lock),
                            contentDescription = null,
                            tint =
                                if (status.earned) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                        )
                    },
                    modifier = Modifier.semantics(mergeDescendants = true) {},
                )
            }
        }
    }
}

/** « Obtenu », ou ce qu'il manque pour obtenir le badge. */
@Composable
private fun badgeSummary(status: BadgeStatus): String {
    if (status.earned) return stringResource(R.string.progress_badge_earned)
    val left = status.remaining
    return when (status.badge.kind) {
        BadgeKind.SORTED -> pluralStringResource(R.plurals.progress_badge_sorted_left, left.toQuantity(), left)
        BadgeKind.STREAK -> pluralStringResource(R.plurals.progress_badge_streak_left, left.toQuantity(), left)
        BadgeKind.FREED -> stringResource(R.string.progress_badge_freed_left, fileSizeLabel(LocalContext.current, left))
        BadgeKind.WEEK -> stringResource(R.string.progress_badge_week_left)
    }
}

private fun Long.toQuantity(): Int = coerceAtMost(Int.MAX_VALUE.toLong()).toInt()

/** Nom du badge [badge]. */
@StringRes
internal fun badgeName(badge: Badge): Int =
    when (badge) {
        Badge.FIRST_SORT -> R.string.progress_badge_first_sort
        Badge.SORTED_100 -> R.string.progress_badge_sorted_100
        Badge.SORTED_1000 -> R.string.progress_badge_sorted_1000
        Badge.STREAK_7 -> R.string.progress_badge_streak_7
        Badge.STREAK_30 -> R.string.progress_badge_streak_30
        Badge.STREAK_100 -> R.string.progress_badge_streak_100
        Badge.FREED_1_GB -> R.string.progress_badge_freed_1
        Badge.FREED_5_GB -> R.string.progress_badge_freed_5
        Badge.FREED_10_GB -> R.string.progress_badge_freed_10
        Badge.FREED_50_GB -> R.string.progress_badge_freed_50
        Badge.WEEK_CAUGHT_UP -> R.string.progress_badge_week
    }

/** Objectifs proposés : 1, 5, 10 et 20 Go. */
@Suppress("MagicNumber")
private val GOAL_CHOICES = listOf(1L, 5L, 10L, 20L).map { it * GIGABYTE }
