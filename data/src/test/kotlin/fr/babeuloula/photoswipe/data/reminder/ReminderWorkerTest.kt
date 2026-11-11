package fr.babeuloula.photoswipe.data.reminder

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import fr.babeuloula.photoswipe.domain.fixtures.FakeDeletionHistory
import fr.babeuloula.photoswipe.domain.fixtures.FakeMediaSource
import fr.babeuloula.photoswipe.domain.fixtures.FakeReminderNotifier
import fr.babeuloula.photoswipe.domain.fixtures.FakeReviewRepository
import fr.babeuloula.photoswipe.domain.fixtures.FixedClock
import fr.babeuloula.photoswipe.domain.fixtures.aMediaItem
import fr.babeuloula.photoswipe.domain.model.MediaItem
import fr.babeuloula.photoswipe.domain.port.MediaSource
import fr.babeuloula.photoswipe.domain.service.ReviewWindowProvider
import fr.babeuloula.photoswipe.domain.service.SameDayMatcher
import fr.babeuloula.photoswipe.domain.usecase.GetReviewDays
import fr.babeuloula.photoswipe.domain.usecase.SendDailyReminder
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.IOException
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime.NOON

/** [ReminderWorker] construit comme par Hilt, sur les fakes du domaine (Robolectric). */
@RunWith(RobolectricTestRunner::class)
class ReminderWorkerTest {
    private val context: Context = RuntimeEnvironment.getApplication()
    private val clock = FixedClock.at(LocalDateTime.of(2026, 7, 15, 18, 0))
    private val notifier = FakeReminderNotifier()

    private fun worker(gallery: MediaSource): ReminderWorker {
        val getReviewDays =
            GetReviewDays(
                ReviewWindowProvider(clock),
                SameDayMatcher(),
                gallery,
                FakeReviewRepository(),
                FakeDeletionHistory(),
                clock,
            )
        val sendDailyReminder = SendDailyReminder(getReviewDays, notifier)
        return TestListenableWorkerBuilder
            .from(context, ReminderWorker::class.java)
            .setWorkerFactory(
                object : WorkerFactory() {
                    override fun createWorker(
                        appContext: Context,
                        workerClassName: String,
                        workerParameters: WorkerParameters,
                    ): ListenableWorker = ReminderWorker(appContext, workerParameters, sendDailyReminder)
                },
            ).build()
    }

    private fun takenLastYear(id: Long): MediaItem =
        aMediaItem(
            id = id,
            takenAt = LocalDateTime.of(LocalDate.of(2025, 7, 15), NOON).atZone(FixedClock.PARIS).toInstant(),
        )

    @Test
    fun `sends nothing when no media is left to sort`() =
        runTest {
            val result = worker(FakeMediaSource()).doWork()

            assertEquals(ListenableWorker.Result.success(), result)
            assertEquals(emptyList<FakeReminderNotifier.Sent>(), notifier.sent)
        }

    @Test
    fun `sends the reminder when media are left to sort`() =
        runTest {
            val result = worker(FakeMediaSource(listOf(takenLastYear(1), takenLastYear(2)))).doWork()

            assertEquals(ListenableWorker.Result.success(), result)
            assertEquals(listOf(2 to LocalDate.of(2026, 7, 15)), notifier.sent.map { it.toSortCount to it.dayToSort })
        }

    @Test
    fun `retries later when the gallery cannot be read`() =
        runTest {
            val unavailable =
                object : MediaSource by FakeMediaSource() {
                    override suspend fun mediaTakenIn(ranges: List<OpenEndRange<Instant>>): List<MediaItem> =
                        throw IOException("MediaStore unavailable")
                }

            assertEquals(ListenableWorker.Result.retry(), worker(unavailable).doWork())
        }
}
