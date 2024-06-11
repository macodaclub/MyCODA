package io.github.macodaclub.models

import kotlinx.serialization.Serializable

@Serializable
data class PostSqwrlRequest(
    val queryString: String
)