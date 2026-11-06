package fr.babeuloula.photoswipe.domain.model

/**
 * Identifiant d'un média, égal à l'ID MediaStore (`media_id`, SPEC §6).
 *
 * Classe de valeur : elle évite de confondre un identifiant avec un autre `Long`
 * (taille, date…) sans coût à l'exécution.
 *
 * @property value valeur de la colonne `_ID` de MediaStore.
 */
@JvmInline
value class MediaId(
    val value: Long,
)
