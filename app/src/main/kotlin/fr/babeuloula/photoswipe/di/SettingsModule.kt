package fr.babeuloula.photoswipe.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import fr.babeuloula.photoswipe.domain.port.ReminderNotifier
import fr.babeuloula.photoswipe.domain.port.ReminderScheduler
import fr.babeuloula.photoswipe.domain.port.SettingsRepository
import fr.babeuloula.photoswipe.domain.port.UsageStatisticsRepository
import fr.babeuloula.photoswipe.domain.usecase.GetReviewDays
import fr.babeuloula.photoswipe.domain.usecase.GetUsageStatistics
import fr.babeuloula.photoswipe.domain.usecase.MarkRatingAsked
import fr.babeuloula.photoswipe.domain.usecase.RecordTodaySorted
import fr.babeuloula.photoswipe.domain.usecase.RescheduleReminder
import fr.babeuloula.photoswipe.domain.usecase.SendDailyReminder
import fr.babeuloula.photoswipe.domain.usecase.SnoozeReminderToday
import fr.babeuloula.photoswipe.domain.usecase.UpdateReminderSettings
import fr.babeuloula.photoswipe.reminder.AndroidReminderNotifier
import java.time.Clock

/** Fournit les use cases des réglages et du rappel quotidien (SPEC §3.6), à part de ceux du tri (`DomainModule`). */
@Module
@InstallIn(SingletonComponent::class)
object SettingsModule {
    @Provides
    fun provideUpdateReminderSettings(
        settingsRepository: SettingsRepository,
        reminderScheduler: ReminderScheduler,
    ): UpdateReminderSettings = UpdateReminderSettings(settingsRepository, reminderScheduler)

    @Provides
    fun provideRescheduleReminder(
        settingsRepository: SettingsRepository,
        reminderScheduler: ReminderScheduler,
    ): RescheduleReminder = RescheduleReminder(settingsRepository, reminderScheduler)

    @Provides
    fun provideSnoozeReminderToday(
        reminderScheduler: ReminderScheduler,
        clock: Clock,
    ): SnoozeReminderToday = SnoozeReminderToday(reminderScheduler, clock)

    @Provides
    fun provideRecordTodaySorted(
        settingsRepository: SettingsRepository,
        clock: Clock,
    ): RecordTodaySorted = RecordTodaySorted(settingsRepository, clock)

    @Provides
    fun provideMarkRatingAsked(settingsRepository: SettingsRepository): MarkRatingAsked =
        MarkRatingAsked(settingsRepository)

    @Provides
    fun provideGetUsageStatistics(repository: UsageStatisticsRepository): GetUsageStatistics =
        GetUsageStatistics(repository)

    @Provides
    fun provideSendDailyReminder(
        getReviewDays: GetReviewDays,
        notifier: ReminderNotifier,
    ): SendDailyReminder = SendDailyReminder(getReviewDays, notifier)

    @Provides
    fun provideReminderNotifier(implementation: AndroidReminderNotifier): ReminderNotifier = implementation
}
