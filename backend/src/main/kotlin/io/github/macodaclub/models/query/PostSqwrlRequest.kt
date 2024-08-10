package io.github.macodaclub.models.query

import kotlinx.serialization.Serializable

@Serializable
data class PostSqwrlRequest(
    val queryString: String
)