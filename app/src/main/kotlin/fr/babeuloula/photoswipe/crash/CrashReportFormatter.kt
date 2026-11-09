package fr.babeuloula.photoswipe.crash

import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.util.Collections
import java.util.IdentityHashMap
import java.util.UUID

/**
 * Ce que le rapport dit de l'application et du téléphone : rien qui identifie la personne.
 *
 * @property applicationId identifiant du paquet, par exemple `fr.babeuloula.photoswipe`.
 * @property versionName version affichée (`versionName`), tirée du dernier tag.
 * @property versionCode numéro de build (`versionCode`).
 * @property debuggable vrai pour un build debug : le rapport part dans l'environnement `debug`.
 * @property androidVersion version d'Android, par exemple `16`.
 * @property sdkInt niveau d'API d'Android.
 * @property manufacturer fabricant du téléphone.
 * @property model modèle du téléphone.
 */
data class CrashEnvironment(
    val applicationId: String,
    val versionName: String,
    val versionCode: Long,
    val debuggable: Boolean,
    val androidVersion: String,
    val sdkInt: Int,
    val manufacturer: String,
    val model: String,
)

/**
 * Met un plantage au format d'événement de Sentry, que GlitchTip lit tel quel (ADR 0033) : la page
 * d'envoi n'a plus qu'à le transmettre. Seuls figurent l'exception, ses causes, leur pile d'appels,
 * la version de l'application, le modèle du téléphone et la version d'Android.
 *
 * Les exceptions vont de la plus ancienne cause à l'exception levée, et chaque pile de l'appel le
 * plus ancien au plus récent, comme le veut le format de Sentry. Les piles et messages trop longs
 * sont coupés, pour que le rapport tienne dans une URL.
 */
class CrashReportFormatter(
    private val newEventId: () -> String = { UUID.randomUUID().toString().replace("-", "") },
) {
    /** Événement de Sentry, en JSON, pour [throwable] levée dans le fil [threadName] à l'instant [time]. */
    fun format(
        throwable: Throwable,
        threadName: String,
        time: Instant,
        environment: CrashEnvironment,
    ): String =
        JSONObject()
            .put("event_id", newEventId())
            .put("timestamp", time.toString())
            .put("platform", "java")
            .put("level", "fatal")
            .put("logger", "uncaught")
            .put("release", "${environment.applicationId}@${environment.versionName}+${environment.versionCode}")
            .put("dist", environment.versionCode.toString())
            .put("environment", if (environment.debuggable) "debug" else "release")
            .put("tags", JSONObject().put("thread", threadName))
            .put("contexts", contexts(environment))
            .put("exception", JSONObject().put("values", exceptions(throwable)))
            .toString()

    private fun contexts(environment: CrashEnvironment): JSONObject =
        JSONObject()
            .put(
                "os",
                JSONObject()
                    .put("name", "Android")
                    .put("version", environment.androidVersion)
                    .put("kernel_version", "API ${environment.sdkInt}"),
            ).put(
                "device",
                JSONObject().put("manufacturer", environment.manufacturer).put("model", environment.model),
            ).put(
                "app",
                JSONObject()
                    .put("app_identifier", environment.applicationId)
                    .put("app_version", environment.versionName)
                    .put("app_build", environment.versionCode.toString()),
            )

    private fun exceptions(throwable: Throwable): JSONArray {
        val chain = mutableListOf<Throwable>()
        val seen = Collections.newSetFromMap(IdentityHashMap<Throwable, Boolean>())
        var current: Throwable? = throwable
        while (current != null && chain.size < MAX_CAUSES && seen.add(current)) {
            chain += current
            current = current.cause
        }
        return JSONArray(chain.asReversed().map(::exception))
    }

    private fun exception(throwable: Throwable): JSONObject {
        val type = throwable.javaClass
        val value =
            JSONObject()
                .put("type", type.simpleName.ifEmpty { type.name })
                .put("module", type.`package`?.name.orEmpty())
                .put("stacktrace", JSONObject().put("frames", frames(throwable.stackTrace)))
        throwable.message?.let { value.put("value", it.take(MAX_MESSAGE_LENGTH)) }
        return value
    }

    private fun frames(stackTrace: Array<StackTraceElement>): JSONArray =
        JSONArray(
            stackTrace.take(MAX_FRAMES).asReversed().map { element ->
                val frame =
                    JSONObject()
                        .put("module", element.className)
                        .put("function", element.methodName)
                        .put("in_app", element.className.startsWith(APP_PACKAGE))
                element.fileName?.let { frame.put("filename", it) }
                if (element.lineNumber > 0) frame.put("lineno", element.lineNumber)
                frame
            },
        )

    private companion object {
        /** Paquet de l'application : ses appels sont mis en avant par GlitchTip (`in_app`). */
        const val APP_PACKAGE = "fr.babeuloula.photoswipe"

        /** Causes gardées au plus, exception levée comprise. */
        const val MAX_CAUSES = 5

        /** Appels gardés au plus par pile, les plus récents. */
        const val MAX_FRAMES = 60

        /** Longueur maximale d'un message d'exception. */
        const val MAX_MESSAGE_LENGTH = 1_000
    }
}
