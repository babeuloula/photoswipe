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
 * Tant que la base est en version 1, le test vérifie que la base créée depuis le schéma exporté
 * est exactement celle que décrit l'entité, et que ses données restent lisibles. Chaque nouvelle
 * version ajoute ici le passage de la précédente avec sa migration.
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

    private companion object {
        const val DATABASE = "migration-test.db"
    }
}
