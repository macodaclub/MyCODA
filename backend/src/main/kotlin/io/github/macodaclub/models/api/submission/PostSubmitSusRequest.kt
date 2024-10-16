package io.github.macodaclub.models.api.submission

import kotlinx.serialization.Serializable

@Serializable
data class PostSubmitSusRequest(
    val submissionId: Int,
    val answers: List<Int>,
)