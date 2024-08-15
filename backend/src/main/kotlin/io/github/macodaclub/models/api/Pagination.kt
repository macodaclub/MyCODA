package io.github.macodaclub.models.api

import kotlinx.serialization.Serializable

const val DefaultOffset = 10

@Serializable
data class Pagination(
    val totalRecords: Int,
    val currentOffset: Int,
    val currentLimit: Int,
    val nextOffset: Int?,
    val previousOffset: Int?,
)