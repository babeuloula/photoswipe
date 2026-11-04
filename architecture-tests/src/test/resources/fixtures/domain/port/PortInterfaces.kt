// Fixture jamais compilée : déclarations autorisées dans le package des ports du domaine.
package fr.babeuloula.photoswipe.domain.port

interface AllowedPort {
    suspend fun read(): List<String>
}

fun interface AllowedFunctionalPort {
    fun run()
}
