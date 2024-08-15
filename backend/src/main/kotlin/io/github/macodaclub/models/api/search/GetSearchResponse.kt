package io.github.macodaclub.models.api.search

import io.github.macodaclub.models.api.Pagination
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