package io.github.macodaclub.models.submission

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class PostSubmitChangesRequest(
    val addedEntities: List<JsonObject>,
    val edits: List<JsonObject>,
)