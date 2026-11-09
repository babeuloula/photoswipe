package fr.babeuloula.photoswipe.crash

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Rapport du dernier plantage, dans un fichier privé de l'application (ADR 0033). Il ne quitte le
 * téléphone que si la personne choisit de l'envoyer, puis il est effacé, envoyé ou non.
 *
 * [save] est synchrone : il est appelé au moment du plantage, quand plus rien d'autre ne tourne.
 * Un nouveau plantage remplace le rapport précédent.
 *
 * @param directory dossier du rapport, hors des sauvegardes d'Android (`noBackupFilesDir`).
 * @param ioDispatcher dispatcher des lectures et effacements.
 */
class CrashReportStore(
    private val directory: File,
    private val ioDispatcher: CoroutineDispatcher,
) {
    private val file: File get() = File(directory, FILE_NAME)

    /** Enregistre [report] à la place du précédent ; une erreur d'écriture est ignorée. */
    fun save(report: String) {
        runCatching {
            directory.mkdirs()
            val temporary = File(directory, "$FILE_NAME.tmp")
            temporary.writeText(report)
            temporary.renameTo(file)
        }
    }

    /** Rapport en attente, `null` s'il n'y en a pas ou s'il est illisible. */
    suspend fun pending(): String? =
        withContext(ioDispatcher) {
            runCatching { file.takeIf { it.isFile }?.readText() }.getOrNull()?.takeIf { it.isNotBlank() }
        }

    /** Efface le rapport en attente. */
    suspend fun clear() {
        withContext(ioDispatcher) { runCatching { file.delete() } }
    }

    private companion object {
        const val FILE_NAME = "last-crash.json"
    }
}
