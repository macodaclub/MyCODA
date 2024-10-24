package io.github.macodaclub.models.api.submission

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class PostSubmitChangesResponse(
    val submissionId: Int,
    val githubIssueUrl: String
)