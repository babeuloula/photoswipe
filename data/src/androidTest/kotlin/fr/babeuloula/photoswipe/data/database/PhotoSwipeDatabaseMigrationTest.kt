package fr.babeuloula.photoswipe.data.database

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Migrations de [PhotoSwipeDatabase], vérifiées contre les schémas exportés dans `data/schemas/`
 * (ajoutés aux assets des tests instrumentés par le plugin Room).
 *
 * Chaque version vérifie que la base créée depuis le schéma exporté est exactement celle que
 * décrivent les entités, et que les données de la version précédente restent lisibles après sa
 * migration.
 */
@RunWith(AndroidJUnit4::class)
class PhotoSwipeDatabaseMigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), PhotoSwipeDatabase::class.java)

    @Test
    fun createsVersion1FromTheExportedSchemaAndKeepsItsRows() {
        helper.createDatabase(DATABASE, 1).use { database ->
            database.execSQL(
                "INSERT INTO review_entry " +
                    "(media_id, review_year, decision, favorite_pending, size_bytes, decided_at) " +
                    "VALUES (42, 2026, 'MARKED_FOR_DELETION', 0, 3500000, 1784138400000)",
            )
        }

        helper.runMigrationsAndValidate(DATABASE, 1, true).use { database ->
            database.query("SELECT media_id, decision, size_bytes FROM review_entry").use { cursor ->
                cursor.moveToFirst()
                assertEquals(42L, cursor.getLong(0))
                assertEquals("MARKED_FOR_DELETION", cursor.getString(1))
                assertEquals(3_500_000L, cursor.getLong(2))
            }
        }
    }

    @Test
    fun migratesVersion1To2WithTheDecisionJournalAndKeepsTheDecisions() {
        helper.createDatabase(DATABASE, 1).use { database ->
            database.execSQL(
                "INSERT INTO review_entry " +
                    "(media_id, review_year, decision, favorite_pending, size_bytes, decided_at) " +
                    "VALUES (42, 2026, 'KEPT', 1, 3500000, 1784138400000)",
            )
        }

        helper.runMigrationsAndValidate(DATABASE, 2, true).use { database ->
            database.query("SELECT media_id, decision, favorite_pending FROM review_entry").use { cursor ->
                cursor.moveToFirst()
                assertEquals(42L, cursor.getLong(0))
                assertEquals("KEPT", cursor.getString(1))
                assertEquals(1, cursor.getInt(2))
            }
            database.query("SELECT COUNT(*) FROM decision_journal").use { cursor ->
                cursor.moveToFirst()
                assertEquals(0, cursor.getInt(0))
            }
        }
    }

    @Test
    fun migratesVersion2To3WithTheReopenedFlagAndKeepsMissingReplacedEntriesMissing() {
        helper.createDatabase(DATABASE, 2).use { database ->
            database.execSQL(
                "INSERT INTO review_entry " +
                    "(media_id, review_year, decision, favorite_pending, size_bytes, decided_at) " +
                    "VALUES (42, 2026, 'KEPT', 0, 3500000, 1784138400000)",
            )
            database.execSQL(
                "INSERT INTO decision_journal (id, day, made_media_id, made_review_year, made_decision, " +
                    "made_favorite_pending, made_size_bytes, made_decided_at) " +
                    "VALUES (1, 20649, 41, 2026, 'KEPT', 0, 100, 1784138400000)",
            )
            database.execSQL(
                "INSERT INTO decision_journal (id, day, made_media_id, made_review_year, made_decision, " +
                    "made_favorite_pending, made_size_bytes, made_decided_at, previous_media_id, " +
                    "previous_review_year, previous_decision, previous_favorite_pending, previous_size_bytes, " +
                    "previous_decided_at) " +
                    "VALUES (2, 20649, 42, 2026, 'KEPT', 0, 100, 1784138400000, " +
                    "42, 2026, 'MARKED_FOR_DELETION', 0, 100, 1784138300000)",
            )
        }

        helper.runMigrationsAndValidate(DATABASE, 3, true, MIGRATION_2_3).use { database ->
            database.query("SELECT reopened FROM review_entry").use { cursor ->
                cursor.moveToFirst()
                assertEquals(0, cursor.getInt(0))
            }
            database.query("SELECT made_reopened, previous_reopened FROM decision_journal ORDER BY id").use { cursor ->
                cursor.moveToFirst()
                assertEquals(0, cursor.getInt(0))
                assertTrue(cursor.isNull(1))
                cursor.moveToNext()
                assertEquals(0, cursor.getInt(0))
                assertEquals(0, cursor.getInt(1))
            }
        }
    }

    @Test
    fun migratesVersion3To4WithAnEmptyDeletionHistoryAndKeepsTheDecisions() {
        helper.createDatabase(DATABASE, 3).use { database ->
            database.execSQL(
                "INSERT INTO review_entry " +
                    "(media_id, review_year, decision, favorite_pending, size_bytes, decided_at, reopened) " +
                    "VALUES (42, 2026, 'MARKED_FOR_DELETION', 0, 3500000, 1784138400000, 0)",
            )
        }

        helper.runMigrationsAndValidate(DATABASE, 4, true).use { database ->
            database.query("SELECT media_id, decision FROM review_entry").use { cursor ->
                cursor.moveToFirst()
                assertEquals(42L, cursor.getLong(0))
                assertEquals("MARKED_FOR_DELETION", cursor.getString(1))
            }
            database.query("SELECT COUNT(*) FROM confirmed_deletion").use { cursor ->
                cursor.moveToFirst()
                assertEquals(0, cursor.getInt(0))
            }
        }
    }

    @Test
    fun migratesVersion4To5WithAnEmptyActivityHistoryAndKeepsTheDeletionHistory() {
        helper.createDatabase(DATABASE, 4).use { database ->
            database.execSQL(
                "INSERT INTO confirmed_deletion (id, media_id, day, size_bytes, confirmed_at) " +
                    "VALUES (1, 42, 20649, 3500000, 1784138400000)",
            )
        }

        helper.runMigrationsAndValidate(DATABASE, 5, true).use { database ->
            database.query("SELECT media_id, size_bytes FROM confirmed_deletion").use { cursor ->
                cursor.moveToFirst()
                assertEquals(42L, cursor.getLong(0))
                assertEquals(3_500_000L, cursor.getLong(1))
            }
            database.query("SELECT COUNT(*) FROM daily_activity").use { cursor ->
                cursor.moveToFirst()
                assertEquals(0, cursor.getInt(0))
            }
        }
    }

    @Test
    fun migratesVersion5To6WithNoPhotoMeasureAndKeepsTheActivity() {
        helper.createDatabase(DATABASE, 5).use { database ->
            database.execSQL(
                "INSERT INTO daily_activity (day, kept_count, favorite_count, trashed_count, freed_bytes) " +
                    "VALUES (20649, 3, 1, 2, 500)",
            )
        }

        helper.runMigrationsAndValidate(DATABASE, 6, true).use { database ->
            database.query("SELECT kept_count FROM daily_activity").use { cursor ->
                cursor.moveToFirst()
                assertEquals(3, cursor.getInt(0))
            }
            database.query("SELECT COUNT(*) FROM photo_measure").use { cursor ->
                cursor.moveToFirst()
                assertEquals(0, cursor.getInt(0))
            }
        }
    }

    @Test
    fun migratesVersion6To7WithNoFingerprintOnTheMeasuresAlreadyTaken() {
        helper.createDatabase(DATABASE, 6).use { database ->
            database.execSQL("INSERT INTO photo_measure (media_id, sharpness, brightness) VALUES (42, 120.0, 90.0)")
        }

        helper.runMigrationsAndValidate(DATABASE, 7, true).use { database ->
            database.query("SELECT sharpness, fingerprint FROM photo_measure").use { cursor ->
                cursor.moveToFirst()
                assertEquals(120.0, cursor.getDouble(0), 0.0)
                assertTrue(cursor.isNull(1))
            }
        }
    }

    private companion object {
        const val DATABASE = "migration-test.db"
    }
}
