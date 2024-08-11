package io.github.macodaclub.models.editor

import kotlinx.serialization.Serializable

@Serializable
data class GetEditorIndividualPropertiesResponse(
    val properties: List<Property>,
) {
    @Serializable
    data class Property(
        val property: Entity,
        val range: Entity,
    ) {

        @Serializable
        data class Entity(
            val iri: String,
            val label: String,
            val type: String
        )
    }
}