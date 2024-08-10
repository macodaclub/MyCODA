package io.github.macodaclub.models.tree

import kotlinx.serialization.Serializable

@Serializable
data class GetTreeResponse(
    val taxonomyTree: TaxonomyTree,
    val entityType: String,
) {
    @Serializable
    data class TaxonomyTree(
        val rootEntries: List<Entity>
    ) {
        @Serializable
        data class Entity(
            val label: String,
            val type: String,
            val iri: String,
            val directChildrenCount: Int,
            val allChildrenCount: Int,
            val expandedChildren: List<Entity>? = null,
        )
    }
}