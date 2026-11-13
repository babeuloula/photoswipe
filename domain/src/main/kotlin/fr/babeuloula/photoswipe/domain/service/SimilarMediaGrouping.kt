package fr.babeuloula.photoswipe.domain.service

import fr.babeuloula.photoswipe.domain.model.MediaId
import fr.babeuloula.photoswipe.domain.model.MediaItem
import fr.babeuloula.photoswipe.domain.model.MediaType
import fr.babeuloula.photoswipe.domain.model.SimilarGroup
import java.time.Duration

/**
 * Repère les photos proches (PS-140, ADR 0035) : des photos prises à la suite, chacune moins de
 * [maxGap] après la précédente, comme une rafale ou plusieurs essais de la même scène. Les vidéos ne
 * comptent pas ; les photos sont rangées par prise de vue, sans regarder leur contenu.
 *
 * Service pur : la règle se lit sur la seule date de prise de vue, sans rien décoder ni enregistrer.
 *
 * @param maxGap écart maximal entre deux photos consécutives d'un même groupe.
 */
class SimilarMediaGrouping(
    private val maxGap: Duration = DEFAULT_MAX_GAP,
) {
    init {
        require(!maxGap.isNegative) { "maxGap must not be negative: $maxGap" }
    }

    /**
     * Place de chaque photo de [media] qui appartient à un groupe d'au moins deux photos proches,
     * avec la photo prise juste avant elle dans le groupe.
     *
     * Avec les empreintes [fingerprints] (PS-165), deux copies d'une même image rejoignent le même
     * groupe même prises à des moments éloignés (copie reçue par une messagerie), et chacune est
     * dite doublon.
     */
    fun groups(
        media: List<MediaItem>,
        fingerprints: Map<MediaId, Long> = emptyMap(),
    ): Map<MediaId, SimilarGroup> {
        val photos = media.filter { it.type == MediaType.IMAGE }.distinctBy { it.id }.sortedBy { it.takenAt }
        val links = Links(photos.size)
        photos.zipWithNext().forEachIndexed { index, (previous, next) ->
            if (Duration.between(previous.takenAt, next.takenAt) <= maxGap) links.join(index, index + 1)
        }
        val duplicates = HashSet<Int>()
        for (i in photos.indices) {
            val a = fingerprints[photos[i].id] ?: continue
            for (j in i + 1 until photos.size) {
                val b = fingerprints[photos[j].id] ?: continue
                if (PhotoFingerprint.sameImage(a, b)) {
                    links.join(i, j)
                    duplicates += i
                    duplicates += j
                }
            }
        }
        return photos.indices
            .groupBy { links.root(it) }
            .values
            .filter { it.size >= 2 }
            .flatMap { group ->
                group.mapIndexed { position, index ->
                    photos[index].id to
                        SimilarGroup(
                            position = position + 1,
                            size = group.size,
                            previousUri = group.getOrNull(position - 1)?.let { photos[it].uri },
                            duplicate = index in duplicates,
                        )
                }
            }.toMap()
    }

    /** Ensembles de photos reliées (union-find). */
    private class Links(
        size: Int,
    ) {
        private val parent = IntArray(size) { it }

        fun root(index: Int): Int {
            var current = index
            while (parent[current] != current) {
                parent[current] = parent[parent[current]]
                current = parent[current]
            }
            return current
        }

        fun join(
            a: Int,
            b: Int,
        ) {
            val rootA = root(a)
            val rootB = root(b)
            // La racine reste la photo la plus ancienne : les groupes gardent l'ordre de prise de vue.
            if (rootA != rootB) parent[maxOf(rootA, rootB)] = minOf(rootA, rootB)
        }
    }

    companion object {
        /** Écart par défaut : 30 secondes entre deux photos consécutives. */
        val DEFAULT_MAX_GAP: Duration = Duration.ofSeconds(30)
    }
}
