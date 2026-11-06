package fr.babeuloula.photoswipe.debug

/**
 * Seeder de la vraie galerie (outils développeur, SPEC §10, niveau 3) : ajoute des photos et des
 * vidéos d'exemple datées dans MediaStore, ou les retire.
 */
interface GallerySeeder {
    /** Ajoute les médias d'exemple et renvoie leur nombre. */
    suspend fun seed(): Int

    /** Retire les médias d'exemple et renvoie leur nombre. */
    suspend fun removeSeeded(): Int
}
