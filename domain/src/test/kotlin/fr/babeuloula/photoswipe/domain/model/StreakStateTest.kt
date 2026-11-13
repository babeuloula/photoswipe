package fr.babeuloula.photoswipe.domain.model

import fr.babeuloula.photoswipe.domain.model.DayStatus.EMPTY
import fr.babeuloula.photoswipe.domain.model.DayStatus.PENDING
import fr.babeuloula.photoswipe.domain.model.DayStatus.SORTED
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

class StreakStateTest {
    private val today = LocalDate.of(2026, 7, 15)

    /** Fenêtre de sept jours finissant [on], états donnés du plus ancien à ce jour-là. */
    private fun window(
        vararg statuses: DayStatus,
        on: LocalDate = today,
    ): Map<LocalDate, DayStatus> =
        statuses.withIndex().associate { (index, status) ->
            on.minusDays((statuses.size - 1 - index).toLong()) to status
        }

    @Test
    fun `has no streak before anything is sorted`() {
        val state = StreakState().updated(today, window(EMPTY, EMPTY, PENDING, EMPTY, EMPTY, EMPTY, PENDING))

        assertEquals(Streak(current = 0, best = 0), state.streak)
    }

    @Test
    fun `counts the calendar days from the first sorted day to today once today is sorted`() {
        val state = StreakState().updated(today, window(PENDING, PENDING, PENDING, PENDING, SORTED, SORTED, SORTED))

        assertEquals(Streak(current = 3, best = 3), state.streak)
    }

    @Test
    fun `stops at yesterday while today is not sorted yet, without breaking`() {
        val state = StreakState().updated(today, window(PENDING, PENDING, PENDING, PENDING, SORTED, SORTED, PENDING))

        assertEquals(2, state.streak.current)
    }

    @Test
    fun `goes through a day without any memory but cannot start on one`() {
        val state = StreakState().updated(today, window(PENDING, EMPTY, EMPTY, SORTED, EMPTY, EMPTY, SORTED))

        assertEquals(4, state.streak.current)
    }

    @Test
    fun `is broken by a missed day, and repaired when that day is caught up within the window`() {
        val missed = StreakState().updated(today, window(SORTED, SORTED, SORTED, PENDING, SORTED, SORTED, SORTED))
        assertEquals(3, missed.streak.current)

        val caughtUp = missed.updated(today, window(SORTED, SORTED, SORTED, SORTED, SORTED, SORTED, SORTED))

        assertEquals(Streak(current = 7, best = 7), caughtUp.streak)
    }

    @Test
    fun `keeps growing beyond the window as days leave it sorted`() {
        var state = StreakState()
        (0L..9L).forEach { offset ->
            val day = today.plusDays(offset)
            state = state.updated(day, window(SORTED, SORTED, SORTED, SORTED, SORTED, SORTED, SORTED, on = day))
        }

        assertEquals(Streak(current = 16, best = 16), state.streak)
        assertEquals(today.minusDays(6), state.runStart)
    }

    @Test
    fun `is broken for good by a day that left the window unsorted, the best streak staying`() {
        val before = StreakState().updated(today, window(SORTED, PENDING, SORTED, SORTED, SORTED, SORTED, SORTED))
        assertEquals(5, before.streak.current)
        val later = today.plusDays(6)

        val state = before.updated(later, window(SORTED, SORTED, PENDING, SORTED, SORTED, SORTED, SORTED, on = later))

        // La meilleure série reste celle qui a été vue : cinq jours.
        assertEquals(Streak(current = 4, best = 5), state.streak)
    }

    @Test
    fun `is lost when the app stays closed longer than the window`() {
        val before = StreakState().updated(today, window(SORTED, SORTED, SORTED, SORTED, SORTED, SORTED, SORTED))
        val later = today.plusDays(30)

        val state = before.updated(later, window(EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, SORTED, on = later))

        assertEquals(Streak(current = 1, best = 7), state.streak)
    }

    @Test
    fun `survives a week away when the days in between were all seen sorted or empty`() {
        val before = StreakState().updated(today, window(SORTED, SORTED, SORTED, SORTED, SORTED, SORTED, SORTED))
        val later = today.plusDays(3)

        val state = before.updated(later, window(SORTED, SORTED, SORTED, SORTED, EMPTY, EMPTY, SORTED, on = later))

        assertEquals(10, state.streak.current)
    }

    @Test
    fun `changes nothing without any day`() {
        val state = StreakState(streak = Streak(current = 2, best = 5))

        assertEquals(state, state.updated(today, emptyMap()))
    }
}
