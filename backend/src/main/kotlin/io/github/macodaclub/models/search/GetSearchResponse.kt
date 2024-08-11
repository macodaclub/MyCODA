package io.github.macodaclub.models.search

import io.github.macodaclub.models.Pagination
import kotlinx.serialization.Serializable

@Serializable
data class GetSearchResponse(
    val entities: List<Entity>,
    val pagination: Pagination
) {
    @Serializable
    data class Entity(
        val iri: String,
        val label: String,
        val type: String,
    )
}