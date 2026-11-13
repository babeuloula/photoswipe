package fr.babeuloula.photoswipe.domain.usecase

import fr.babeuloula.photoswipe.domain.fixtures.FakePhotoAnalyzer
import fr.babeuloula.photoswipe.domain.fixtures.FakePhotoMeasureCache
import fr.babeuloula.photoswipe.domain.fixtures.FakeSettingsRepository
import fr.babeuloula.photoswipe.domain.fixtures.aCheckerboard
import fr.babeuloula.photoswipe.domain.fixtures.aFlatImage
import fr.babeuloula.photoswipe.domain.fixtures.aMediaItem
import fr.babeuloula.photoswipe.domain.model.MediaId
import fr.babeuloula.photoswipe.domain.model.MediaType
import fr.babeuloula.photoswipe.domain.model.PhotoMeasure
import fr.babeuloula.photoswipe.domain.model.QualityHint
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class GetQualityHintsTest {
    private val settings = FakeSettingsRepository()
    private val cache = FakePhotoMeasureCache()
    private val analyzer =
        FakePhotoAnalyzer(
            mapOf(
                MediaId(1) to aFlatImage(150),
                MediaId(2) to aFlatImage(10),
                MediaId(3) to aCheckerboard(),
            ),
        )
    private val getQualityHints = GetQualityHints(analyzer, cache, settings)
    private val photos = (1L..4L).map { aMediaItem(id = it) }

    @Test
    fun `flags the blurry and the very dark photos, not the sharp nor the unreadable ones`() =
        runTest {
            assertEquals(
                mapOf(MediaId(1) to QualityHint.BLURRY, MediaId(2) to QualityHint.DARK),
                getQualityHints(photos),
            )
        }

    @Test
    fun `measures each photo once, then reads the kept measure`() =
        runTest {
            getQualityHints(photos)
            analyzer.read.clear()

            val hints = getQualityHints(photos)

            assertEquals(listOf(MediaId(4)), analyzer.read)
            assertEquals(QualityHint.BLURRY, hints[MediaId(1)])
            assertEquals(3, cache.measures.size)
        }

    @Test
    fun `reuses a measure kept from before`() =
        runTest {
            val hints =
                GetQualityHints(
                    analyzer,
                    FakePhotoMeasureCache(mapOf(MediaId(3) to PhotoMeasure(1.0, 200.0))),
                    settings,
                )(
                    listOf(aMediaItem(id = 3)),
                )

            assertEquals(mapOf(MediaId(3) to QualityHint.BLURRY), hints)
        }

    @Test
    fun `leaves videos out`() =
        runTest {
            assertEquals(
                emptyMap<MediaId, QualityHint>(),
                getQualityHints(listOf(aMediaItem(id = 1, type = MediaType.VIDEO))),
            )
            assertEquals(emptyList<MediaId>(), analyzer.read)
        }

    @Test
    fun `flags nothing once the detection is turned off`() =
        runTest {
            settings.saveQualityHints(false)

            assertEquals(emptyMap<MediaId, QualityHint>(), getQualityHints(photos))
            assertEquals(emptyList<MediaId>(), analyzer.read)
        }
}
