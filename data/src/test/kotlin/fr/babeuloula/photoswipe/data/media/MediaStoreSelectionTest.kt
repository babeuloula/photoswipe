package fr.babeuloula.photoswipe.data.media

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class MediaStoreSelectionTest {
    @Test
    fun `passes each range as milliseconds then as seconds rounded up`() {
        val start = Instant.parse("2025-07-14T22:00:00Z")
        val end = Instant.parse("2025-07-15T22:00:00.500Z")

        val selection = takenInSelection(listOf(start..<end))

        assertEquals(
            listOf(
                start.toEpochMilli(),
                end.toEpochMilli(),
                start.epochSecond,
                end.epochSecond + 1,
            ).map(Long::toString),
            selection.args,
        )
    }

    @Test
    fun `adds one clause and four arguments per range`() {
        val ranges = (2020..2022).map { Instant.parse("$it-07-15T00:00:00Z")..<Instant.parse("$it-07-16T00:00:00Z") }

        val selection = takenInSelection(ranges)

        assertEquals(12, selection.args.size)
        assertEquals(12, selection.sql.count { it == '?' })
    }

    @Test
    fun `selects ids with one placeholder each`() {
        assertEquals(MediaStoreSelection("_id IN (?,?,?)", listOf("1", "2", "3")), idsSelection(listOf(1, 2, 3)))
    }
}
