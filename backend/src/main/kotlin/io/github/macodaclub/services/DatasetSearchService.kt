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

        println("1 - DatasetSearchService START")

        println("2 - Antes de resolver critérios")

        val resolvedRequest =
            DatasetCriterionResolverService.resolve(request)

        println("3 - Critérios resolvidos")

        println("4 - Antes de construir query")

        val generatedQueryString =
            DatasetQueryBuilderService.buildQueryString(
                resolvedRequest
            )

        println(
            "5 - Query construída: $generatedQueryString"
        )

        println(
            "6 - Antes OpenAireResearchProductClient.search"
        )

        val openAireStart =
            System.currentTimeMillis()

        val openAirePage =
            OpenAireResearchProductClient.search(
                generatedQueryString
            )

        println(
            "7 - Depois OpenAireResearchProductClient.search " +
                "(${System.currentTimeMillis() - openAireStart} ms)"
        )

        val totalResults =
            openAirePage.totalResults

        val currentLastIndex =
            resolvedRequest.page.toLong() *
                resolvedRequest.pageSize.toLong()

        val effectiveOffsetLimit =
            totalResults?.coerceAtMost(
                OPENAIRE_OFFSET_LIMIT
            )

        val hasNextPage =
            if (effectiveOffsetLimit != null) {
                currentLastIndex <
                    effectiveOffsetLimit
            } else {
                openAirePage.results.size ==
                    resolvedRequest.pageSize &&
                    currentLastIndex <
                    OPENAIRE_OFFSET_LIMIT
            }

        val warnings =
            buildList {
                if (
                    totalResults != null &&
                    totalResults >
                    OPENAIRE_OFFSET_LIMIT
                ) {
                    add(
                        DatasetSearchWarning(
                            source =
                                DatasetSourceType.OPENAIRE,
                            code =
                                "OPENAIRE_OFFSET_LIMIT",
                            message =
                                "OpenAIRE reports $totalResults matching results, " +
                                    "but offset pagination exposes at most the first " +
                                    "$OPENAIRE_OFFSET_LIMIT. Refine the filters for " +
                                    "complete interactive browsing."
                        )
                    )
                }
            }

        println(
            "8 - DatasetSearchService END"
        )

        return DatasetSearchResponse(
            queryString =
                generatedQueryString,
            page =
                openAirePage.page
                    ?: resolvedRequest.page,
            pageSize =
                openAirePage.pageSize
                    ?: resolvedRequest.pageSize,
            totalResults =
                totalResults,
            hasNextPage =
                hasNextPage,
            selectedFields =
                request.selectedFields,
            results =
                openAirePage.results,
            warnings =
                warnings
        )
    }
}