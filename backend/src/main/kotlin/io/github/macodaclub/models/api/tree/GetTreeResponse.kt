package io.github.macodaclub.models.api.tree

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
            val iri: String,
            val label: String,
            val type: String,
            val directChildrenCount: Int,
            val allChildrenCount: Int,
            val expandedChildren: List<Entity>? = null,
        )
    }
}