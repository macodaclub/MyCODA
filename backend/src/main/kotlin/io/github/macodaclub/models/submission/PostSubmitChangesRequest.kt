package io.github.macodaclub.models.submission

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class PostSubmitChangesRequest(
    val addedEntities: List<JsonObject>,
    val editedEntities: List<EditedEntity>,
) {

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