package io.github.macodaclub.models.api.submission

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class PostSubmitChangesRequest(
    val formData: FormData,
    val addedEntities: List<JsonObject>,
    val editedEntities: List<EditedEntity>,
) {

    @Serializable
    data class FormData(
        val articleTitle: String,
        val articleAbstract: String,
        val articleKeywords: String,
        val articleAuthors: String,
        val emailAddress: String,
    )

    @Serializable
    data class EditedEntity(
        val entity: Entity,
        val edits: List<JsonObject>,
    )

    @Serializable
    data class Entity(
        val iri: String,
        val label: String,
        val type: String,
    )
}