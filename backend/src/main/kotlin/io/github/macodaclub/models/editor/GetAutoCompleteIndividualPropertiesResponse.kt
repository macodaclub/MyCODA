package io.github.macodaclub.models.editor

import kotlinx.serialization.Serializable

@Serializable
data class GetAutoCompleteIndividualPropertiesResponse(
    val properties: List<Property>,
) {
    @Serializable
    data class Property(
        val property: Entity,
        val range: Entity,
        val rangeIsLiteral: Boolean,
    ) {

        @Serializable
        data class Entity(
            val iri: String,
            val label: String,
            val type: String
        )
    }
}