package io.github.macodaclub.services

import io.github.macodaclub.models.api.datasets.DatasetCriterionOperator
import io.github.macodaclub.models.api.datasets.DatasetSearchCriterion
import io.github.macodaclub.models.api.datasets.DatasetSearchRequest
import io.github.macodaclub.models.api.datasets.DatasetSourceType
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * Constrói queries para a OpenAIRE Graph API V3.
 *
 * Os nomes amigáveis do MyCODA são mantidos na UI e traduzidos aqui
 * quando a V3 usa outro parâmetro (authorOrcid -> authorId, sdg -> sdgLabel).
 */
object DatasetQueryBuilderService {

    private const val MAX_OFFSET_RESULTS = 10_000

    private val allowedFilteringFields = setOf(
        "mainTitle",
        "pid",
        "type",
        "fromPublicationDate",
        "toPublicationDate",
        "subjects",
        "countryCode",
        "authorFullName",
        "authorOrcid",
        "publisher",
        "influenceClass",
        "popularityClass",
        "impulseClass",
        "citationCountClass",
        "instanceType",
        "sdg",
        "fos",
        "relOrganizationId",
        "relProjectId",
        "relProjectCode",
        "hasProjectRel",
        "relProjectFundingShortName",
        "relProjectFundingStreamId",
        "relHostingDataSourceId",
        "relCollectedFromDatasourceId",
        "rorId"
    )

    private val openAireFieldAliases = mapOf(
        "authorOrcid" to "authorId",
        "sdg" to "sdgLabel"
    )

    private val allowedSortFields = setOf(
        "relevance",
        "publicationDate",
        "dateOfCollection",
        "influence",
        "popularity",
        "citationCount",
        "impulse"
    )

    private val fieldsSupportingExclude = setOf(
        "mainTitle",
        "subjects"
    )

    fun buildQueryString(request: DatasetSearchRequest): String {
        validateRequest(request)
        request.criteria.forEach(::validateCriterion)

        val queryParts = mutableListOf<String>()

        /*
         * Filtros diferentes são AND por omissão na V3.
         * Valores repetidos do mesmo filtro são agregados numa expressão OR.
         */
        request.criteria
            .groupBy { toOpenAireField(it.field) }
            .forEach { (apiField, criteria) ->
                val expression = buildFieldExpression(criteria)

                queryParts.add(
                    "${encode(apiField)}=${encode(expression)}"
                )
            }

        if (request.sort.isNotEmpty()) {
            request.sort.forEach { sort ->
                require(sort.field in allowedSortFields) {
                    "Unsupported sorting field: ${sort.field}."
                }
            }

            val sortExpression = request.sort.joinToString(",") {
                "${it.field} ${it.direction.name}"
            }

            queryParts.add(
                "sortBy=${encode(sortExpression)}"
            )
        }

        queryParts.add(
            "page=${request.page}"
        )

        queryParts.add(
            "pageSize=${request.pageSize}"
        )

        return queryParts.joinToString("&")
    }

    private fun buildFieldExpression(
        criteria: List<DatasetSearchCriterion>
    ): String {
        val includes = criteria
            .filter {
                it.operator == DatasetCriterionOperator.INCLUDE
            }
            .map {
                resolveCriterionValue(it)
            }

        val excludes = criteria
            .filter {
                it.operator == DatasetCriterionOperator.EXCLUDE
            }
            .map {
                resolveCriterionValue(it)
            }

        if (
            includes.size == 1 &&
            excludes.isEmpty()
        ) {
            return includes.single()
        }

        val includeExpression =
            buildOrExpression(includes)

        val excludeExpression =
            buildOrExpression(excludes)

        return when {
            includeExpression != null &&
                excludeExpression != null ->
                "$includeExpression AND NOT $excludeExpression"

            includeExpression != null ->
                includeExpression

            excludeExpression != null ->
                "NOT $excludeExpression"

            else ->
                throw IllegalArgumentException(
                    "A filtering criterion must contain a value."
                )
        }
    }

    private fun resolveCriterionValue(
        criterion: DatasetSearchCriterion
    ): String {
        return when (criterion.field) {

            "relHostingDataSourceId" -> {
                OpenAireDataSourceService.resolveJournalId(
                    criterion.value
                )
                    ?: throw IllegalArgumentException(
                        "OpenAIRE journal not found: ${criterion.value}"
                    )
            }

            else ->
                criterion.value.trim()
        }
    }

    private fun buildOrExpression(
        values: List<String>
    ): String? {
        if (values.isEmpty()) {
            return null
        }

        if (values.size == 1) {
            return quoteLogicalValue(
                values.single()
            )
        }

        return values.joinToString(
            separator = " OR ",
            prefix = "(",
            postfix = ")"
        ) { value ->
            quoteLogicalValue(value)
        }
    }

    private fun quoteLogicalValue(
        value: String
    ): String {
        val escaped = value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")

        return "\"$escaped\""
    }

    private fun toOpenAireField(
        myCodaField: String
    ): String =
        openAireFieldAliases[myCodaField]
            ?: myCodaField

    private fun validateRequest(
        request: DatasetSearchRequest
    ) {
        require(
            request.criteria.isNotEmpty()
        ) {
            "Please add at least one filtering criterion."
        }

        require(
            request.page >= 1
        ) {
            "Page must be greater than or equal to 1."
        }

        require(
            request.pageSize in 1..100
        ) {
            "Page size must be between 1 and 100."
        }

        val firstResultOffset =
            (request.page - 1).toLong() *
                request.pageSize.toLong()

        require(
            firstResultOffset < MAX_OFFSET_RESULTS
        ) {
            "OpenAIRE offset pagination is limited to the first " +
                "$MAX_OFFSET_RESULTS results. Refine the query to continue."
        }
    }

    private fun validateCriterion(
        criterion: DatasetSearchCriterion
    ) {
        require(
            criterion.source ==
                DatasetSourceType.OPENAIRE
        ) {
            "Dataset source ${criterion.source} is not supported by the OpenAIRE query builder."
        }

        require(
            criterion.field.isNotBlank()
        ) {
            "Please select a filtering criterion."
        }

        require(
            criterion.field in
                allowedFilteringFields
        ) {
            "Unsupported OpenAIRE filtering criterion: ${criterion.field}."
        }

        require(
            criterion.value.isNotBlank()
        ) {
            "Please assign a value to ${criterion.field}."
        }

        if (
            criterion.operator ==
            DatasetCriterionOperator.EXCLUDE
        ) {
            require(
                criterion.field in
                    fieldsSupportingExclude
            ) {
                "The exclude operator is only supported for mainTitle and subjects."
            }
        }
    }

    private fun encode(
        value: String
    ): String =
        URLEncoder.encode(
            value,
            StandardCharsets.UTF_8
        )
}