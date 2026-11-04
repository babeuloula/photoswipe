// Fixture jamais compilée : chaque classe ou objet ci-dessous est interdit dans les ports du domaine.
package fr.babeuloula.photoswipe.domain.port.sub

interface StillAllowedPort

class InMemoryPort : StillAllowedPort

abstract class AbstractPort

data class PortValue(
    val value: Int,
)

object SingletonPort

enum class PortKind { A }
