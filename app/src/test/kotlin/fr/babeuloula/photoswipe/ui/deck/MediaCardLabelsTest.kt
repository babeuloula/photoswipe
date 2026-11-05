package fr.babeuloula.photoswipe.ui.deck

import android.content.Context
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.time.Duration
import java.time.LocalDate

/** Étiquette et poids des cartes, dans les deux langues de l'application. */
@RunWith(RobolectricTestRunner::class)
class MediaCardLabelsTest {
    private val context: Context get() = RuntimeEnvironment.getApplication()

    @Test
    @Config(qualifiers = "fr")
    fun `formats the memory label in French`() {
        assertEquals("Il y a 3 ans · 14 mai 2023", memoryLabel(context, 3, LocalDate.of(2023, 5, 14)))
    }

    @Test
    @Config(qualifiers = "fr")
    fun `uses the singular for one year in French`() {
        assertEquals("Il y a 1 an · 14 mai 2025", memoryLabel(context, 1, LocalDate.of(2025, 5, 14)))
    }

    @Test
    @Config(qualifiers = "en")
    fun `formats the memory label in English`() {
        assertEquals("3 years ago · May 14, 2023", memoryLabel(context, 3, LocalDate.of(2023, 5, 14)))
    }

    @Test
    @Config(qualifiers = "en")
    fun `uses the singular for one year in English`() {
        assertEquals("1 year ago · May 14, 2025", memoryLabel(context, 1, LocalDate.of(2025, 5, 14)))
    }

    @Test
    @Config(qualifiers = "fr")
    fun `shows the file size in French units`() {
        val size = fileSizeLabel(context, 3_500_000)

        assertTrue(size, size.startsWith("3,5") && size.endsWith("Mo"))
    }

    @Test
    @Config(qualifiers = "en")
    fun `shows the file size in English units`() {
        assertEquals("3.5 MB", fileSizeLabel(context, 3_500_000))
    }

    @Test
    fun `formats a short video duration like a player`() {
        assertEquals("00:32", durationLabel(Duration.ofSeconds(32)))
    }

    @Test
    fun `formats durations in minutes and hours`() {
        assertEquals("25:00", durationLabel(Duration.ofMinutes(25)))
        assertEquals("1:02:03", durationLabel(Duration.ofHours(1).plusMinutes(2).plusSeconds(3)))
    }
}
