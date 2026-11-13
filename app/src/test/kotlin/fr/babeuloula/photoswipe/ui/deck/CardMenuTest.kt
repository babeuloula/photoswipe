package fr.babeuloula.photoswipe.ui.deck

import android.content.ComponentName
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import coil3.ImageLoader
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import fr.babeuloula.photoswipe.MainActivity
import fr.babeuloula.photoswipe.domain.model.MediaType
import fr.babeuloula.photoswipe.ui.common.LocalImageLoader
import fr.babeuloula.photoswipe.ui.theme.PhotoSwipeTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Duration
import java.time.LocalDate

/** Menu « Plus d'actions » de la carte du dessus : ajouter ou retirer le favori (PS-124). */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class)
class CardMenuTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val events = mutableListOf<DeckEvent>()

    private fun showMenu(
        card: MediaCardUiState,
        shareTarget: String? = null,
    ) {
        composeRule.activity.setContent {
            CompositionLocalProvider(LocalImageLoader provides ImageLoader(composeRule.activity)) {
                PhotoSwipeTheme {
                    DeckScreen(
                        day = LocalDate.of(2026, 5, 14),
                        state = DeckUiState.Loaded(listOf(card), menuOpen = true, shareTarget = shareTarget),
                        onEvent = { events += it },
                    )
                }
            }
        }
    }

    @Test
    fun `offers to remove a favorite card from the favorites`() {
        showMenu(PREVIEW_CARD.copy(favorite = true))

        composeRule.onNodeWithText("Add to favorites").assertDoesNotExist()
        composeRule.onNodeWithText("Remove from favorites").performClick()

        assertEquals(listOf<DeckEvent>(DeckEvent.Unfavorite), events)
    }

    @Test
    fun `offers to open the card in another app`() {
        showMenu(PREVIEW_CARD)

        composeRule.onNodeWithText("Open with").performClick()

        assertEquals(listOf<DeckEvent>(DeckEvent.OpenWith), events)
    }

    @Test
    fun `offers to send the card again with the last app chosen, while it is installed`() {
        val messenger = ComponentName("org.example.messenger", "org.example.messenger.Share")
        shadowOf(composeRule.activity.packageManager).addOrUpdateActivity(
            ActivityInfo().apply {
                packageName = messenger.packageName
                name = messenger.className
                nonLocalizedLabel = "Messenger"
                applicationInfo = ApplicationInfo().apply { packageName = messenger.packageName }
            },
        )
        showMenu(PREVIEW_CARD, shareTarget = messenger.flattenToString())

        composeRule.onNodeWithText("Send again with Messenger").performClick()

        assertEquals(listOf<DeckEvent>(DeckEvent.ShareAgain), events)
    }

    @Test
    fun `offers nothing to send again once the last app is uninstalled`() {
        showMenu(PREVIEW_CARD, shareTarget = "org.example.gone/.Share")

        composeRule.onNodeWithText("Send again with", substring = true).assertDoesNotExist()
    }

    @Test
    fun `offers to add a card that is not a favorite yet`() {
        showMenu(PREVIEW_CARD)

        composeRule.onNodeWithText("Remove from favorites").assertDoesNotExist()
        composeRule.onNodeWithText("Add to favorites").assertExists()
    }

    @Test
    fun `offers to share a photo as a postcard`() {
        showMenu(PREVIEW_CARD.copy(type = MediaType.IMAGE))

        composeRule.onNodeWithText("Share as a postcard").performClick()

        assertEquals(listOf<DeckEvent>(DeckEvent.SharePostcard), events)
    }

    @Test
    fun `offers no postcard for a video`() {
        showMenu(PREVIEW_CARD.copy(type = MediaType.VIDEO, duration = Duration.ofSeconds(12)))

        composeRule.onNodeWithText("Share as a postcard").assertDoesNotExist()
    }
}
