package fr.babeuloula.photoswipe.domain.model

import fr.babeuloula.photoswipe.domain.fixtures.aConfirmedDeletion
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class ConfirmedDeletionTest {
    @Test
    fun `rejects a negative size`() {
        assertThrows<IllegalArgumentException> { aConfirmedDeletion(sizeBytes = -1) }
    }
}
