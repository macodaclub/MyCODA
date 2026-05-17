package io.github.macodaclub.models.api

import kotlinx.serialization.Serializable

@Serializable
data class GetOntologyEntitiesResponse(
    val entities: List<Entity>
) {
    @Serializable
    data class Entity(
        val id: String,
        val type: String,
        val name: String,
        val details: String? = null,
        val comment: String? = null
    )
}