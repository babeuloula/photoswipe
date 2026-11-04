package fr.babeuloula.photoswipe.architecture

import com.lemonappdev.konsist.api.container.KoScope
import com.lemonappdev.konsist.api.declaration.KoImportDeclaration
import com.lemonappdev.konsist.api.provider.KoNameProvider

/** Package racine du projet : celui de `:app`, préfixe de tous les autres. */
private const val ROOT_PACKAGE = "fr.babeuloula.photoswipe"
private const val DOMAIN_PACKAGE = "$ROOT_PACKAGE.domain"
private const val DATA_PACKAGE = "$ROOT_PACKAGE.data"
private const val DI_PACKAGE = "$ROOT_PACKAGE.di"
private const val PORT_PACKAGE = "$DOMAIN_PACKAGE.port"

/**
 * Bibliothèques interdites dans `:domain` : le framework Android et tout Jetpack
 * (`androidx` couvre Room et Compose), ainsi que les bibliothèques Android de Google.
 */
private val ANDROID_PACKAGES = listOf("android", "androidx", "com.google.android")

/**
 * Imports qui feraient dépendre `:domain` d'Android ou d'un module extérieur (SPEC §5.1).
 * Le domaine n'importe, du projet, que ses propres packages : ni `:data`, ni `:app`.
 */
internal fun KoScope.forbiddenDomainImports(): List<KoImportDeclaration> =
    imports.filter { import ->
        ANDROID_PACKAGES.any { import.name.isInPackage(it) } ||
            (import.name.isInPackage(ROOT_PACKAGE) && !import.name.isInPackage(DOMAIN_PACKAGE))
    }

/**
 * Imports de `:data` faits depuis `:app` hors du package `di` : seul le câblage Hilt
 * a le droit de connaître les implémentations (docs/architecture.md §1).
 */
internal fun KoScope.dataImportsOutsideDi(): List<KoImportDeclaration> =
    files
        .filterNot { it.packagee?.name?.isInPackage(DI_PACKAGE) == true }
        .flatMap { it.imports }
        .filter { it.name.isInPackage(DATA_PACKAGE) }

/**
 * Classes et objets (énumérations comprises) déclarés dans les ports du domaine : ce package ne
 * contient que des interfaces, que `:data` implémente (ADR 0007). Une implémentation ou un modèle
 * a sa place ailleurs (`model`, ou `:data`).
 */
internal fun KoScope.nonInterfacePortDeclarations(): List<KoNameProvider> =
    files
        .filter { it.packagee?.name?.isInPackage(PORT_PACKAGE) == true }
        .flatMap { file -> file.classes(includeNested = true) + file.objects(includeNested = true) }

/** Vrai si ce nom qualifié est le package donné ou l'un de ses sous-packages (`di` ≠ `diagnostics`). */
private fun String.isInPackage(packageName: String): Boolean = this == packageName || startsWith("$packageName.")
