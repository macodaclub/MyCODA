package io.github.macodaclub.models.api.datasets

import kotlinx.serialization.Serializable

/**
 * Pedido de pesquisa híbrida OpenAIRE + OPL.
 */
@Serializable
data class HybridDatasetSearchRequest(
    val criteria: List<DatasetSearchCriterion>,
    val sort: List<DatasetSort> = emptyList(),
    val selectedFields: List<String> = emptyList(),
    val page: Int = 1,
    val pageSize: Int = 20
)

/**
 * Resultado associado entre um research product OpenAIRE
 * e um problema existente na OPL.
 *
 * Os campos OpenAIRE e OPL permanecem separados semanticamente,
 * apesar de serem devolvidos na mesma linha.
 */
@Serializable
data class HybridDatasetResult(
    val id: String,

    // OpenAIRE
    val openAireId: String,
    val type: String,
    val title: String,
    val authors: String,
    val publicationDate: String,
    val publisher: String? = null,
    val citations: Int? = null,
    val relatedMaterials: String? = null,

    // OPL
    val oplId: String,
    val oplName: String? = null,
    val oplLongName: String? = null,
    val oplDescription: String? = null,
    val oplObjectives: List<String> = emptyList(),
    val oplVariableTypes: List<String> = emptyList(),
    val oplVariableDimensions: List<String> = emptyList(),
    val oplConstraintTypes: List<String> = emptyList(),
    val oplNumberOfConstraints: List<String> = emptyList(),
    val oplModality: String? = null,
    val oplNoiseType: String? = null,
    val oplType: String? = null,
    val oplAuthors: List<String> = emptyList(),
    val oplReferenceTitles: List<String> = emptyList(),
    val oplLinks: List<String> = emptyList()
)

/**
 * Resposta da pesquisa híbrida.
 *
 * results contém as linhas OpenAIRE + OPL já associadas.
 *
 * openAire e opl são mantidos temporariamente para permitir
 * análise e compatibilidade durante a migração do frontend.
 */
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