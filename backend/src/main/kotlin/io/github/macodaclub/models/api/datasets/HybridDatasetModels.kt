package io.github.macodaclub.models.api.datasets

import kotlinx.serialization.Serializable

@Serializable
data class HybridDatasetSearchRequest(
    val criteria: List<DatasetSearchCriterion>,
    val sort: List<DatasetSort> = emptyList(),
    val selectedFields: List<String> = emptyList(),
    val page: Int = 1,
    val pageSize: Int = 20
)

/**
 * Linha da pesquisa OpenAIRE + OPL.
 *
 * O fluxo Node-RED ativo concatena os resultados das duas fontes; não exige
 * que um registo OpenAIRE tenha uma correspondência OPL. Por isso cada linha
 * identifica a sua fonte e preenche apenas os campos dessa fonte.
 */
@Serializable
data class HybridDatasetResult(
    val id: String,
    val source: DatasetSourceType,

    // OpenAIRE
    val openAireId: String? = null,
    val type: String? = null,
    val instanceType: String? = null,
    val title: String? = null,
    val abstract: String? = null,
    val subjects: List<String> = emptyList(),
    val authors: String? = null,
    val authorDetails: List<DatasetAuthor> = emptyList(),
    val publicationDate: String? = null,
    val publisher: String? = null,
    val scientificEvent: String? = null,
    val citations: Int? = null,
    val influenceClass: String? = null,
    val popularityClass: String? = null,
    val impulseClass: String? = null,
    val citationCountClass: String? = null,
    val relatedMaterials: String? = null,
    val countryCode: String? = null,
    val sdg: List<String> = emptyList(),
    val fos: List<String> = emptyList(),

    // OPL
    val oplId: String? = null,
    val oplName: String? = null,
    val oplLongName: String? = null,
    val oplDescription: String? = null,
    val oplObjectives: List<String> = emptyList(),
    val oplVariableTypes: List<String> = emptyList(),
    val oplVariableDimensions: List<String> = emptyList(),
    val oplConstraintTypes: List<String> = emptyList(),
    val oplNumberOfConstraints: List<String> = emptyList(),
    val oplConstraintHardness: List<String> = emptyList(),
    val oplConstraintEquality: List<String> = emptyList(),
    val oplModality: String? = null,
    val oplNoiseType: String? = null,
    val oplType: String? = null,
    val oplProblemSources: List<String> = emptyList(),
    val oplDynamicTypes: List<String> = emptyList(),
    val oplFidelityLevels: List<String> = emptyList(),
    val oplEvaluationTimes: List<String> = emptyList(),
    val oplProblems: List<String> = emptyList(),
    val oplInstances: List<String> = emptyList(),
    val oplCodeExamples: List<String> = emptyList(),
    val oplImplementationIds: List<String> = emptyList(),
    val oplImplementationNames: List<String> = emptyList(),
    val oplImplementationLinks: List<String> = emptyList(),
    val oplImplementationLanguages: List<String> = emptyList(),
    val oplImplementationEvaluationTimes: List<String> = emptyList(),
    val oplTags: List<String> = emptyList(),
    val oplAllowsPartialEvaluation: String? = null,
    val oplCanEvaluateObjectivesIndependently: String? = null,
    val oplAuthors: List<String> = emptyList(),
    val oplReferenceTitles: List<String> = emptyList(),
    val oplLinks: List<String> = emptyList()
)

@Serializable
data class HybridDatasetSearchResponse(
    val page: Int,
    val pageSize: Int,
    val totalResults: Int,
    val hasNextPage: Boolean,
    val selectedFields: List<String> = emptyList(),
    val results: List<HybridDatasetResult>,
    val openAire: DatasetSearchResponse,
    val opl: OplDatasetSearchResponse,
    val warnings: List<DatasetSearchWarning> = emptyList()
)
