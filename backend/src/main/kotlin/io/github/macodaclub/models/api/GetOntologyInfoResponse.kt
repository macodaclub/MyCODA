package io.github.macodaclub.models.api

import kotlinx.serialization.Serializable

@Serializable
data class GetOntologyInfoResponse(
    val annotations: List<Annotation>,
    val counts: Counts
) {
    @Serializable
    data class Counts(
        val classes: Int,
        val properties: Int,
        val individuals: Int,
    )

    @Serializable
    data class Annotation(
        val property: String,
        val value: String
    )
}