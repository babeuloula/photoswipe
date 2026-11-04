package fr.babeuloula.photoswipe.ui.common

/** Action d'une coquille d'écran ([ShellScreen]) : son libellé et ce qu'elle déclenche. */
data class ShellAction(
    val label: String,
    val onClick: () -> Unit,
)
