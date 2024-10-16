package io.github.macodaclub.models.api.submission

import kotlinx.serialization.Serializable

@Serializable
data class PostSubmitFeedbackRequest(
    val submissionId: Int,
    val feedback: String,
)