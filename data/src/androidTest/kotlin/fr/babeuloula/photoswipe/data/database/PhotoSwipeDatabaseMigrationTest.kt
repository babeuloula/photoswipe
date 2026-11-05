package fr.babeuloula.photoswipe.data.database

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
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
    fun `creates version 1 from the exported schema and keeps its rows`() {
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
    fun `migrates version 1 to 2 with the decision journal and keeps the decisions`() {
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

    private companion object {
        const val DATABASE = "migration-test.db"
    }
}
