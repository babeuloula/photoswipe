package fr.babeuloula.photoswipe.domain.usecase

import fr.babeuloula.photoswipe.domain.fixtures.FakeSettingsRepository
import fr.babeuloula.photoswipe.domain.model.ManageMediaTip
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class OfferManageMediaTipTest {
    @Test
    fun `offers the media management tip after the first system confirmation`() =
        runTest {
            val settings = FakeSettingsRepository()

            OfferManageMediaTip(settings)()

            assertEquals(ManageMediaTip.OFFERED, settings.manageMediaTip)
        }

    @Test
    fun `never offers again a tip the user dismissed`() =
        runTest {
            val settings = FakeSettingsRepository(manageMediaTip = ManageMediaTip.DISMISSED)

            OfferManageMediaTip(settings)()

            assertEquals(ManageMediaTip.DISMISSED, settings.manageMediaTip)
        }
}
