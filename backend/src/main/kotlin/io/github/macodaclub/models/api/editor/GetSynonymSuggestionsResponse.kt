package io.github.macodaclub.models.api.editor

import kotlinx.serialization.Serializable

@Serializable
data class GetSynonymSuggestionsResponse(
    val synonymSuggestions: List<Entity>
) {
    @Serializable
    data class Entity(
        val iri: String,
        val label: String,
        val type: String,
    )
}