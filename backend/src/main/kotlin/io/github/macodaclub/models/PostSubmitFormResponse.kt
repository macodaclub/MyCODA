package io.github.macodaclub.models

import kotlinx.serialization.Serializable

@Serializable
data class PostSubmitFormResponse(
    val referencedEntities: List<Entity>,
    val titleSegments: List<TextSegment>,
    val abstractSegments: List<TextSegment>,
    val keywordSegments: List<TextSegment>,
) {

    @Serializable
    data class Entity(
        val label: String,
        val iri: String,
        val type: String,
    )

    @Serializable
    data class TextSegment(
        val text: String,
        val entityReferenceIndex: Int?
    )
}