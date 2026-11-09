package fr.babeuloula.photoswipe.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import fr.babeuloula.photoswipe.crash.CrashReportFormatter
import fr.babeuloula.photoswipe.crash.CrashReportStore
import kotlinx.coroutines.Dispatchers
import java.io.File
import javax.inject.Singleton

/** Fournit le rapport de plantage en attente et son format (ADR 0033). */
@Module
@InstallIn(SingletonComponent::class)
object CrashReportModule {
    @Provides
    @Singleton
    fun provideCrashReportStore(
        @ApplicationContext context: Context,
    ): CrashReportStore = CrashReportStore(File(context.noBackupFilesDir, "crash"), Dispatchers.IO)

    @Provides
    fun provideCrashReportFormatter(): CrashReportFormatter = CrashReportFormatter()
}
