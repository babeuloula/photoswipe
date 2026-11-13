package fr.babeuloula.photoswipe.widget

import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.ui.unit.DpSize
import androidx.glance.appwidget.testing.unit.hasStartActivityClickAction
import androidx.glance.appwidget.testing.unit.runGlanceAppWidgetUnitTest
import androidx.glance.testing.unit.hasContentDescription
import androidx.glance.testing.unit.hasText
import fr.babeuloula.photoswipe.MainActivity
import fr.babeuloula.photoswipe.domain.fixtures.aMediaItem
import fr.babeuloula.photoswipe.domain.model.TodayWidget
import fr.babeuloula.photoswipe.tile.SortTodayTileService
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.time.LocalDate

/** Rendu du widget « Ce jour-là » (PS-170) par l'outil de test de Glance, sous Robolectric. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "fr")
class ThisDayWidgetContentTest {
    private val application = RuntimeEnvironment.getApplication()
    private val today = LocalDate.of(2026, 11, 13)
    private val toSort = TodayWidget.ToSort(today, remainingCount = 23, years = 2016..2025, preview = aMediaItem())
    private val photo = Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888)

    private fun render(
        data: ThisDayWidgetData,
        size: DpSize = ThisDayWidget.MEDIUM,
        checks: androidx.glance.appwidget.testing.unit.GlanceAppWidgetUnitTest.() -> Unit,
    ) = runGlanceAppWidgetUnitTest {
        setContext(application)
        setAppWidgetSize(size)
        provideComposable { ThisDayWidgetContent(data) }
        checks()
    }

    @Test
    fun `shows the date, the count and the years of the memories left to sort`() =
        render(ThisDayWidgetData(toSort, photo)) {
            onNode(hasText("13 novembre")).assertExists()
            onNode(hasText("23 à trier")).assertExists()
            onNode(hasText("2016–2025")).assertExists()
            onNode(hasContentDescription("Un souvenir de ce jour")).assertExists()
        }

    @Test
    fun `puts the short date and the count on the photo in the small size`() =
        render(ThisDayWidgetData(toSort, photo), size = ThisDayWidget.SMALL) {
            onNode(hasText("13 nov.")).assertExists()
            onNode(hasText("23 à trier")).assertExists()
            onNode(hasText("2016–2025")).assertDoesNotExist()
        }

    @Test
    fun `shows no photo when the widget hides it`() =
        render(ThisDayWidgetData(toSort, photo = null)) {
            onNode(hasText("23 à trier")).assertExists()
            onNode(hasContentDescription("Un souvenir de ce jour")).assertDoesNotExist()
        }

    @Test
    fun `opens the day's stack when memories are left to sort`() =
        render(ThisDayWidgetData(toSort)) {
            onNode(hasStartActivityClickAction(SortTodayTileService.todayDeckIntent(application))).assertExists()
        }

    @Test
    fun `says everything is sorted and opens the home screen`() =
        render(ThisDayWidgetData(TodayWidget.AllSorted(today))) {
            onNode(hasText("Tout est trié")).assertExists()
            onNode(hasText("23 à trier")).assertDoesNotExist()
            onNode(
                hasStartActivityClickAction(
                    Intent(application, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                ),
            ).assertExists()
        }

    @Test
    fun `says there is no memory on this day`() =
        render(ThisDayWidgetData(TodayWidget.NoMemories(today))) {
            onNode(hasText("Aucun souvenir ce jour-là")).assertExists()
        }

    @Test
    fun `invites to open the app without media access`() =
        render(ThisDayWidgetData(TodayWidget.NoAccess)) {
            onNode(hasText("Ouvrez PhotoSwipe pour voir vos souvenirs")).assertExists()
        }
}
