package fr.babeuloula.photoswipe.data.seed

import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.media.ExifInterface
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import dagger.hilt.android.qualifiers.ApplicationContext
import fr.babeuloula.photoswipe.data.R
import fr.babeuloula.photoswipe.data.di.IoDispatcher
import fr.babeuloula.photoswipe.domain.model.MediaType
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File
import java.time.Clock
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import javax.inject.Inject

/**
 * Seeder MediaStore de la variante debug (SPEC §10, niveau 3) : insère de vraies photos et vidéos
 * datées dans la galerie de l'appareil, pour tester le vrai chemin de requête sur émulateur.
 *
 * Les photos sont des JPEG générés (une couleur par année, la date écrite dessus) datés par leur
 * EXIF ; les vidéos, des copies de `res/raw/seed_video.mp4` dont la date de création est réécrite
 * ([Mp4CreationTime]). MediaStore recalcule en effet `DATE_TAKEN` depuis le fichier quand il
 * l'indexe. Tous les médias vont dans un dossier à part ([SEED_FOLDER]), ce qui permet de les retirer.
 *
 * Limite : les médias insérés appartiennent à PhotoSwipe, qui peut les modifier sans demander.
 * Les dialogues système de corbeille et de favori peuvent donc ne pas apparaître pour eux ; pour
 * tester ces dialogues, il faut des médias poussés par `adb` (script `scripts/seed-emulator.sh`).
 */
class MediaStoreSeeder
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
        private val clock: Clock,
        @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) {
        private val resolver: ContentResolver = context.contentResolver

        /** Insère les médias de [SeedCatalog] pour la fenêtre du jour et renvoie leur nombre. */
        suspend fun seed(): Int = seed(SeedCatalog.plan(LocalDate.now(clock), clock.zone))

        /** Insère les médias de [plan] et renvoie leur nombre. */
        suspend fun seed(plan: List<SeedMedia>): Int =
            withContext(ioDispatcher) {
                val video by lazy { context.resources.openRawResource(R.raw.seed_video).use { it.readBytes() } }
                plan.forEach { media ->
                    when (media.type) {
                        MediaType.IMAGE -> {
                            insert(media, Collection.IMAGES, jpegOf(media.takenAt))
                        }

                        MediaType.VIDEO -> {
                            insert(
                                media,
                                Collection.VIDEOS,
                                Mp4CreationTime.stamp(video, media.takenAt.toInstant()),
                            )
                        }
                    }
                }
                plan.size
            }

        /**
         * Retire de la galerie tous les médias du dossier du seeder, y compris ceux mis à la
         * corbeille, et renvoie leur nombre. Un média qui n'appartient plus à l'app (après une
         * désinstallation) est laissé en place.
         */
        suspend fun removeSeeded(): Int =
            withContext(ioDispatcher) {
                Collection.entries.sumOf { collection ->
                    seededIds(collection).count { id -> delete(ContentUris.withAppendedId(collection.uri, id)) }
                }
            }

        private fun insert(
            media: SeedMedia,
            collection: Collection,
            bytes: ByteArray,
        ) {
            val values =
                ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, media.displayName)
                    put(MediaStore.MediaColumns.MIME_TYPE, collection.mimeType)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, collection.relativePath)
                    put(MediaStore.MediaColumns.DATE_TAKEN, media.takenAt.toInstant().toEpochMilli())
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
            val uri = checkNotNull(resolver.insert(collection.uri, values)) { "insert of ${media.displayName} failed" }
            resolver.openOutputStream(uri).use { checkNotNull(it).write(bytes) }
            resolver.update(uri, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null)
        }

        private fun seededIds(collection: Collection): List<Long> {
            val arguments =
                Bundle().apply {
                    // Dossier ou nom : MediaStore peut mettre le dossier à jour un peu après l'insertion.
                    putString(
                        ContentResolver.QUERY_ARG_SQL_SELECTION,
                        "${MediaStore.MediaColumns.RELATIVE_PATH} = ? OR ${MediaStore.MediaColumns.DISPLAY_NAME} LIKE ?",
                    )
                    putStringArray(
                        ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS,
                        arrayOf(collection.relativePath, "${SeedMedia.FILE_PREFIX}%"),
                    )
                    putInt(MediaStore.QUERY_ARG_MATCH_TRASHED, MediaStore.MATCH_INCLUDE)
                    putInt(MediaStore.QUERY_ARG_MATCH_PENDING, MediaStore.MATCH_INCLUDE)
                }
            return resolver
                .query(collection.uri, arrayOf(MediaStore.MediaColumns._ID), arguments, null)
                ?.use { cursor -> buildList { while (cursor.moveToNext()) add(cursor.getLong(0)) } }
                .orEmpty()
        }

        /** Supprime [uri] ; faux si le média n'appartient pas à l'app. */
        private fun delete(uri: Uri): Boolean =
            try {
                resolver.delete(uri, null, null) > 0
            } catch (_: SecurityException) {
                false
            }

        /** JPEG d'une couleur propre à l'année, la date écrite dessus, daté par son EXIF. */
        private fun jpegOf(takenAt: ZonedDateTime): ByteArray {
            val file = File.createTempFile("seed", ".jpg", context.cacheDir)
            try {
                file.outputStream().use { drawPhoto(takenAt).compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it) }
                ExifInterface(file).apply {
                    setAttribute(ExifInterface.TAG_DATETIME_ORIGINAL, EXIF_DATE.format(takenAt))
                    setAttribute(ExifInterface.TAG_OFFSET_TIME_ORIGINAL, EXIF_OFFSET.format(takenAt))
                    saveAttributes()
                }
                return file.readBytes()
            } finally {
                file.delete()
            }
        }

        private fun drawPhoto(takenAt: ZonedDateTime): Bitmap {
            val bitmap = Bitmap.createBitmap(PHOTO_WIDTH, PHOTO_HEIGHT, Bitmap.Config.ARGB_8888)
            val hue = (takenAt.year * HUE_STEP) % FULL_TURN
            Canvas(bitmap).apply {
                drawColor(Color.HSVToColor(floatArrayOf(hue.toFloat(), SATURATION, BRIGHTNESS)))
                val paint =
                    Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.WHITE
                        textAlign = Paint.Align.CENTER
                        textSize = DATE_TEXT_SIZE
                    }
                drawText(CAPTION_DATE.format(takenAt), PHOTO_WIDTH / 2f, PHOTO_HEIGHT / 2f, paint)
                paint.textSize = TIME_TEXT_SIZE
                drawText(CAPTION_TIME.format(takenAt), PHOTO_WIDTH / 2f, PHOTO_HEIGHT / 2f + TIME_TEXT_SIZE * 2, paint)
            }
            return bitmap
        }

        /** Collections de MediaStore et dossier du seeder dans chacune. */
        private enum class Collection(
            val uri: Uri,
            val mimeType: String,
            val relativePath: String,
        ) {
            IMAGES(
                MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY),
                "image/jpeg",
                "Pictures/$SEED_FOLDER/",
            ),
            VIDEOS(
                MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY),
                "video/mp4",
                "Movies/$SEED_FOLDER/",
            ),
        }

        companion object {
            /** Dossier des médias du seeder, dans `Pictures/` et `Movies/`. */
            const val SEED_FOLDER: String = "PhotoSwipe seed"

            private const val PHOTO_WIDTH = 900
            private const val PHOTO_HEIGHT = 1200
            private const val JPEG_QUALITY = 85
            private const val HUE_STEP = 47
            private const val FULL_TURN = 360
            private const val SATURATION = 0.55f
            private const val BRIGHTNESS = 0.7f
            private const val DATE_TEXT_SIZE = 72f
            private const val TIME_TEXT_SIZE = 48f
            private val EXIF_DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss")
            private val EXIF_OFFSET: DateTimeFormatter = DateTimeFormatter.ofPattern("xxx")
            private val CAPTION_DATE: DateTimeFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)
            private val CAPTION_TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
        }
    }
