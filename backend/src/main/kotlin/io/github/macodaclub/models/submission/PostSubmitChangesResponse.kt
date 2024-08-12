package io.github.macodaclub.models.submission

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class PostSubmitChangesResponse(
    val githubIssueUrl: String
)