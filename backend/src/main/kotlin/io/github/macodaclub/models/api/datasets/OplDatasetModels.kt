package io.github.macodaclub.models.api.datasets

import kotlinx.serialization.Serializable

/**
 * Pedido enviado pelo frontend para pesquisar problemas OPL.
 */
@Serializable
data class OplDatasetSearchRequest(
    val criteria: List<OplDatasetSearchCriterion> = emptyList(),
    val page: Int = 1,
    val pageSize: Int = 20,
    val sortField: String? = null,
    val sortOrder: String? = null
)

/**
 * Critério individual da pesquisa OPL.
 */
@Serializable
data class OplDatasetSearchCriterion(
    val field: String,
    val operator: String = "INCLUDE",
    val value: String
)

/**
 * Resultado de um problema de otimização da OPL.
 */
@Serializable
data class OplDatasetResult(
    val id: String,
    val name: String? = null,
    val longName: String? = null,
    val description: String? = null,
    val objectives: List<String> = emptyList(),
    val variableTypes: List<String> = emptyList(),
    val variableDimensions: List<String> = emptyList(),
    val constraintTypes: List<String> = emptyList(),
    val numberOfConstraints: List<String> = emptyList(),
    val modality: String? = null,
    val noiseType: String? = null,
    val type: String? = null,
    val source: String = "OPL",
    val authors: List<String> = emptyList(),
    val referenceTitles: List<String> = emptyList(),
    val links: List<String> = emptyList()
)

/**
 * Resposta devolvida para o frontend.
 */
@Serializable
data class OplDatasetSearchResponse(
    val page: Int,
    val pageSize: Int,
    val totalResults: Int,
    val hasNextPage: Boolean,
    val results: List<OplDatasetResult>,
    val warnings: List<String> = emptyList()
)