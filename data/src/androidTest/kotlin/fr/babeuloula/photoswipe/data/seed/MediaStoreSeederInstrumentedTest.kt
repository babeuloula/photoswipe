package fr.babeuloula.photoswipe.data.seed

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import fr.babeuloula.photoswipe.data.media.MediaStoreMediaSource
import fr.babeuloula.photoswipe.domain.model.MediaType
import fr.babeuloula.photoswipe.domain.service.SameDayMatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId

/**
 * [MediaStoreSeeder] face au vrai MediaStore : les médias insérés sont relus par
 * [MediaStoreMediaSource] à leur date, puis retirés. Les dates (années 1990) évitent toute
 * confusion avec de vraies photos ; le test retire aussi les médias d'un seeding manuel antérieur.
 */
@RunWith(AndroidJUnit4::class)
class MediaStoreSeederInstrumentedTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val zone = ZoneId.systemDefault()
    private val seeder = MediaStoreSeeder(context, Clock.system(zone), Dispatchers.IO)
    private val source = MediaStoreMediaSource(context.contentResolver, Dispatchers.IO)
    private val day = LocalDate.of(1997, 4, 12)

    @Before
    fun removeLeftovers() =
        runTest {
            seeder.removeSeeded()
        }

    @After
    fun removeSeededMedia() =
        runTest {
            seeder.removeSeeded()
        }

    /** Médias de la galerie pris le jour du test ; MediaStore les met à jour un peu après l'insertion. */
    private suspend fun mediaOfTheDay() = source.mediaTakenIn(SameDayMatcher().rangesFor(day.plusYears(1), zone))

    /** Attend jusqu'à 10 secondes que [read] renvoie [expected], et renvoie la dernière valeur lue. */
    private suspend fun <T> eventually(
        expected: T,
        read: suspend () -> T,
    ): T {
        var value = read()
        repeat(POLL_ATTEMPTS) {
            if (value == expected) return value
            withContext(Dispatchers.IO) { Thread.sleep(POLL_MILLIS) }
            value = read()
        }
        return value
    }

    @Test
    fun `seeds photos and videos that the gallery reads at their date`() =
        runTest {
            val plan =
                listOf(
                    SeedMedia(MediaType.IMAGE, day.atTime(10, 0).atZone(zone)),
                    SeedMedia(MediaType.VIDEO, day.minusYears(2).atTime(19, 30).atZone(zone)),
                )

            assertEquals(2, seeder.seed(plan))

            val expected = plan.map { it.type to it.takenAt.toInstant() }.toSet()
            assertEquals(expected, eventually(expected) { mediaOfTheDay().map { it.type to it.takenAt }.toSet() })
        }

    @Test
    fun `removes every seeded media`() =
        runTest {
            seeder.seed(listOf(SeedMedia(MediaType.IMAGE, day.atTime(11, 0).atZone(zone))))

            assertEquals(1, seeder.removeSeeded())

            assertEquals(emptyList<Any>(), eventually(emptyList()) { mediaOfTheDay() })
        }

    private companion object {
        const val POLL_ATTEMPTS = 50
        const val POLL_MILLIS = 200L
    }
}
