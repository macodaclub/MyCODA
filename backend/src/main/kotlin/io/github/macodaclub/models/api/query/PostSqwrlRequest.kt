package io.github.macodaclub.models.api.query

import kotlinx.serialization.Serializable

@Serializable
data class PostSqwrlRequest(
    val queryString: String
)