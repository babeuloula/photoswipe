package fr.babeuloula.photoswipe.ui.postcard

import android.graphics.Bitmap
import androidx.activity.compose.setContent
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import fr.babeuloula.photoswipe.MainActivity
import fr.babeuloula.photoswipe.ui.theme.PhotoSwipeTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Aperçu de la carte postale (PS-177) : il reste en place quand on bascule la mention. */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class)
class PostcardPreviewDialogTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val ready =
        PostcardPreview.Ready(
            Bitmap.createBitmap(300, 240, Bitmap.Config.ARGB_8888).asImageBitmap(),
            "5 years ago · Nov 6, 2021",
        )

    private fun show(rendering: Boolean) {
        composeRule.activity.setContent {
            PhotoSwipeTheme {
                PostcardPreviewDialog(
                    preview = ready,
                    withBrand = false,
                    onWithBrandChange = {},
                    onShare = {},
                    onDismiss = {},
                    rendering = rendering,
                )
            }
        }
    }

    @Test
    fun `keeps showing the postcard while it is redrawn, with share disabled until it is ready`() {
        show(rendering = true)

        composeRule.onNodeWithContentDescription("Postcard: 5 years ago · Nov 6, 2021").assertIsDisplayed()
        composeRule.onNodeWithText("Share").assertIsNotEnabled()
    }

    @Test
    fun `shares the postcard once it is drawn`() {
        show(rendering = false)

        composeRule.onNodeWithText("Share").assertIsEnabled()
    }
}
