package io.github.macodaclub.services

import io.github.macodaclub.models.api.datasets.DatasetSearchRequest
import io.github.macodaclub.models.api.datasets.DatasetSearchResponse
import io.github.macodaclub.models.api.datasets.DatasetSearchWarning
import io.github.macodaclub.models.api.datasets.DatasetSourceType

/**
 * Orquestra a pesquisa OpenAIRE:
 * 1. normaliza/resove identificadores;
 * 2. constrói a query V3;
 * 3. executa a pesquisa;
 * 4. devolve paginação baseada no header oficial da OpenAIRE.
 */
object DatasetSearchService {

    private const val OPENAIRE_OFFSET_LIMIT = 10_000L

    fun search(request: DatasetSearchRequest): DatasetSearchResponse {
        val resolvedRequest =
            DatasetCriterionResolverService.resolve(request)

        val generatedQueryString =
            DatasetQueryBuilderService.buildQueryString(resolvedRequest)

        val openAirePage =
            OpenAireResearchProductClient.search(generatedQueryString)

        val totalResults = openAirePage.totalResults
        val currentLastIndex =
            resolvedRequest.page.toLong() *
                resolvedRequest.pageSize.toLong()

        val effectiveOffsetLimit = totalResults
            ?.coerceAtMost(OPENAIRE_OFFSET_LIMIT)

        val hasNextPage = if (effectiveOffsetLimit != null) {
            currentLastIndex < effectiveOffsetLimit
        } else {
            openAirePage.results.size == resolvedRequest.pageSize &&
                currentLastIndex < OPENAIRE_OFFSET_LIMIT
        }

        val warnings = buildList {
            if (
                totalResults != null &&
                totalResults > OPENAIRE_OFFSET_LIMIT
            ) {
                add(
                    DatasetSearchWarning(
                        source = DatasetSourceType.OPENAIRE,
                        code = "OPENAIRE_OFFSET_LIMIT",
                        message =
                            "OpenAIRE reports $totalResults matching results, " +
                                "but offset pagination exposes at most the first " +
                                "$OPENAIRE_OFFSET_LIMIT. Refine the filters for " +
                                "complete interactive browsing."
                    )
                )
            }
        }

        return DatasetSearchResponse(
            queryString = generatedQueryString,
            page = openAirePage.page ?: resolvedRequest.page,
            pageSize = openAirePage.pageSize ?: resolvedRequest.pageSize,
            totalResults = totalResults,
            hasNextPage = hasNextPage,
            selectedFields = request.selectedFields,
            results = openAirePage.results,
            warnings = warnings
        )
    }
}
