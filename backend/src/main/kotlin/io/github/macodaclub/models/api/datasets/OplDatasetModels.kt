package io.github.macodaclub.models.api.datasets

import kotlinx.serialization.Serializable

@Serializable
data class OplDatasetSearchRequest(
    val criteria: List<OplDatasetSearchCriterion> = emptyList(),
    val page: Int = 1,
    val pageSize: Int = 20,
    val sortField: String? = null,
    val sortOrder: String? = null
)

@Serializable
data class OplDatasetSearchCriterion(
    val field: String,
    val operator: String = "INCLUDE",
    val value: String
)

/**
 * Entidade pesquisável da OPL oficial (problem, suite ou generator).
 *
 * source identifica a origem técnica do registo; problemSources representa
 * o campo `source` do problems.yaml (por exemplo artificial / real-world).
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
    val constraintHardness: List<String> = emptyList(),
    val constraintEquality: List<String> = emptyList(),
    val modality: String? = null,
    val noiseType: String? = null,
    val type: String? = null,
    val source: String = "OPL",
    val problemSources: List<String> = emptyList(),
    val dynamicTypes: List<String> = emptyList(),
    val fidelityLevels: List<String> = emptyList(),
    val evaluationTimes: List<String> = emptyList(),
    val problems: List<String> = emptyList(),
    val instances: List<String> = emptyList(),
    val codeExamples: List<String> = emptyList(),
    val implementationIds: List<String> = emptyList(),
    val implementationNames: List<String> = emptyList(),
    val implementationLinks: List<String> = emptyList(),
    val implementationLanguages: List<String> = emptyList(),
    val implementationEvaluationTimes: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val allowsPartialEvaluation: String? = null,
    val canEvaluateObjectivesIndependently: String? = null,
    val authors: List<String> = emptyList(),
    val referenceTitles: List<String> = emptyList(),
    val links: List<String> = emptyList()
)

@Serializable
data class OplDatasetSearchResponse(
    val page: Int,
    val pageSize: Int,
    val totalResults: Int,
    val hasNextPage: Boolean,
    val results: List<OplDatasetResult>,
    val warnings: List<String> = emptyList(),
    val sourceUrl: String? = null
)
