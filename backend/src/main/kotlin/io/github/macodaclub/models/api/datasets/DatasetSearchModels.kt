package io.github.macodaclub.models.api.datasets

import kotlinx.serialization.Serializable

/**
 * Fonte onde o critério deve ser aplicado.
 *
 * OPL e BBOB ficam preparados para as próximas implementações.
 */
@Serializable
enum class DatasetSourceType {
    OPENAIRE,
    OPL,
    BBOB
}
/**
 * restcountrys api.
 */
@Serializable
data class DatasetVocabularyOption(
    val code: String,
    val title: String
)

/**
 * Operador aplicado ao valor do critério.
 */
@Serializable
enum class DatasetCriterionOperator {
    INCLUDE,
    EXCLUDE
}

/**
 * Direção da ordenação.
 */
@Serializable
enum class DatasetSortDirection {
    ASC,
    DESC
}

/**
 * Critério de pesquisa.
 *
 * Exemplo:
 * authorFullName INCLUDE "Carola Doerr"
 */
@Serializable
data class DatasetSearchCriterion(
    val source: DatasetSourceType = DatasetSourceType.OPENAIRE,
    val field: String,
    val operator: DatasetCriterionOperator = DatasetCriterionOperator.INCLUDE,
    val value: String
)

/**
 * Configuração de ordenação.
 *
 * Exemplo:
 * publicationDate DESC
 */
@Serializable
data class DatasetSort(
    val field: String,
    val direction: DatasetSortDirection = DatasetSortDirection.DESC
)

/**
 * Pedido enviado pelo frontend.
 *
 * A queryString deixa de ser enviada pelo frontend.
 */
@Serializable
data class DatasetSearchRequest(
    val criteria: List<DatasetSearchCriterion>,
    val sort: List<DatasetSort> = emptyList(),
    val selectedFields: List<String> = emptyList(),
    val page: Int = 1,
    val pageSize: Int = 20
)

@Serializable
data class DatasetAuthor(
    val name: String,
    val orcid: String? = null
)

/**
 * Resultado normalizado devolvido ao frontend.
 */
@Serializable
data class DatasetSearchResult(
    val id: String,

    val type: String,

    val instanceType: String? = null,

    val title: String,
    
    val abstract: String? = null,

    val subjects: List<String> = emptyList(),

    /*
     * Mantido para compatibilidade com
     * HybridDatasetSearchService e frontend atual.
     */
    val authors: String,

    val authorDetails: List<DatasetAuthor> = emptyList(),

    val publicationDate: String,

    val publisher: String? = null,

    val scientificEvent: String? = null,

    val citations: Int? = null,

    val influenceClass: String? = null,
    val popularityClass: String? = null,
    val impulseClass: String? = null,
    val citationCountClass: String? = null,

    /*
     * Mantido como String para não quebrar
     * o Hybrid atual.
     */
    val relatedMaterials: String? = null,

    val countryCode: String? = null,

    val sdg: List<String> = emptyList(),

    val fos: List<String> = emptyList()
)

/**
 * Aviso relativo a uma fonte externa.
 *
 */
@Serializable
data class DatasetSearchWarning(
    val source: DatasetSourceType,
    val code: String,
    val message: String
)

/**
 * Resposta devolvida ao frontend.
 */
@Serializable
data class DatasetSearchResponse(
    val queryString: String,
    val page: Int,
    val pageSize: Int,
    val totalResults: Long? = null,
    val hasNextPage: Boolean = false,
    val selectedFields: List<String> = emptyList(),
    val results: List<DatasetSearchResult>,
    val warnings: List<DatasetSearchWarning> = emptyList()
)