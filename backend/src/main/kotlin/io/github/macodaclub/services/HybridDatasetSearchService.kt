package io.github.macodaclub.services

import io.github.macodaclub.models.api.datasets.DatasetCriterionOperator
import io.github.macodaclub.models.api.datasets.DatasetSearchCriterion
import io.github.macodaclub.models.api.datasets.DatasetSearchRequest
import io.github.macodaclub.models.api.datasets.DatasetSearchResponse
import io.github.macodaclub.models.api.datasets.DatasetSearchResult
import io.github.macodaclub.models.api.datasets.DatasetSearchWarning
import io.github.macodaclub.models.api.datasets.DatasetSort
import io.github.macodaclub.models.api.datasets.DatasetSortDirection
import io.github.macodaclub.models.api.datasets.DatasetSourceType
import io.github.macodaclub.models.api.datasets.HybridDatasetResult
import io.github.macodaclub.models.api.datasets.HybridDatasetSearchRequest
import io.github.macodaclub.models.api.datasets.HybridDatasetSearchResponse
import io.github.macodaclub.models.api.datasets.OplDatasetResult
import io.github.macodaclub.models.api.datasets.OplDatasetSearchCriterion
import io.github.macodaclub.models.api.datasets.OplDatasetSearchRequest
import io.github.macodaclub.models.api.datasets.OplDatasetSearchResponse
import io.github.macodaclub.plugins.OntologyManager
import java.text.Normalizer

/**
 * Serviço responsável pela pesquisa híbrida OpenAIRE + OPL.
 *
 * Fluxo P0:
 * 1. separar critérios OpenAIRE e OPL;
 * 2. percorrer as páginas internas de cada fonte;
 * 3. associar os resultados OpenAIRE ao melhor problema OPL;
 * 4. ordenar as linhas híbridas finais;
 * 5. aplicar a paginação pedida pelo frontend apenas depois do join.
 */
object HybridDatasetSearchService {

    private const val INTERNAL_PAGE_SIZE = 100
    private const val MAX_SOURCE_PAGES = 100

    private val openAireExcludeFields = setOf(
        "mainTitle",
        "subjects"
    )

    /**
     * Sorts suportados no resultado já associado.
     *
     * Inclui aliases antigos/não prefixados para compatibilidade com o
     * frontend durante a migração.
     */
    private val supportedHybridSortFields = setOf(
        // OpenAIRE
        "openAireId",
        "type",
        "title",
        "mainTitle",
        "authors",
        "publicationDate",
        "publisher",
        "citations",
        "citationCount",
        "relatedMaterials",

        // OPL
        "oplId",
        "name",
        "oplName",
        "longName",
        "oplLongName",
        "description",
        "oplDescription",
        "objectives",
        "oplObjectives",
        "variableTypes",
        "oplVariableTypes",
        "variableDimensions",
        "oplVariableDimensions",
        "constraintTypes",
        "oplConstraintTypes",
        "numberOfConstraints",
        "oplNumberOfConstraints",
        "modality",
        "oplModality",
        "noiseType",
        "oplNoiseType",
        "oplType",
        "oplAuthors",
        "referenceTitles",
        "oplReferenceTitles",
        "links",
        "oplLinks"
    )

    fun search(
        ontologyManager: OntologyManager,
        request: HybridDatasetSearchRequest
    ): HybridDatasetSearchResponse {
        validateRequest(request)

        val openAireCriteria = request.criteria.filter { criterion ->
            criterion.source == DatasetSourceType.OPENAIRE
        }

        val oplCriteria = request.criteria.filter { criterion ->
            criterion.source == DatasetSourceType.OPL
        }

        require(openAireCriteria.isNotEmpty()) {
            "A hybrid search requires at least one OpenAIRE criterion."
        }

        require(oplCriteria.isNotEmpty()) {
            "A hybrid search requires at least one OPL criterion."
        }

        /*
         * O sort da pesquisa HYBRID não é reenviado para as fontes.
         * É aplicado apenas depois do join, evitando colisões semânticas
         * entre campos OpenAIRE e OPL (por exemplo type e authors).
         */
        val openAireLoadResult = loadAllOpenAireResults(
            criteria = openAireCriteria,
            selectedFields = request.selectedFields
        )

        val oplLoadResult = loadAllOplResults(
            ontologyManager = ontologyManager,
            criteria = oplCriteria
        )

        val joinedResults = joinResults(
            openAireResults = openAireLoadResult.results,
            oplResults = oplLoadResult.results
        )

        val sortedResults = sortHybridResults(
            results = joinedResults,
            sort = request.sort
        )

        val totalResults = sortedResults.size

        val fromIndex =
            (request.page - 1) * request.pageSize

        val toIndex = minOf(
            fromIndex + request.pageSize,
            totalResults
        )

        val pageResults =
            if (fromIndex >= totalResults) {
                emptyList()
            } else {
                sortedResults.subList(
                    fromIndex,
                    toIndex
                )
            }

        val unmatchedOpenAireCount =
            (openAireLoadResult.results.size - joinedResults.size)
                .coerceAtLeast(0)

        val warnings = buildList {
            addAll(openAireLoadResult.warnings)

            oplLoadResult.warnings.forEach { warning ->
                add(
                    DatasetSearchWarning(
                        source = DatasetSourceType.OPL,
                        code = "OPL_RESULT_WARNING",
                        message = warning
                    )
                )
            }

            if (unmatchedOpenAireCount > 0) {
                add(
                    DatasetSearchWarning(
                        source = DatasetSourceType.OPENAIRE,
                        code = "HYBRID_UNMATCHED_RESULTS",
                        message =
                            "$unmatchedOpenAireCount OpenAIRE result(s) " +
                                "could not be associated with an OPL problem."
                    )
                )
            }
        }.distinct()

        return HybridDatasetSearchResponse(
            page = request.page,
            pageSize = request.pageSize,
            totalResults = totalResults,
            hasNextPage = toIndex < totalResults,
            selectedFields = request.selectedFields,
            results = pageResults,
            openAire = buildOpenAireSummary(openAireLoadResult),
            opl = buildOplSummary(oplLoadResult),
            warnings = warnings
        )
    }

    /**
     * Percorre a paginação OpenAIRE em blocos de 100 até não existir página
     * seguinte ou até atingir o limite de segurança.
     */
    private fun loadAllOpenAireResults(
        criteria: List<DatasetSearchCriterion>,
        selectedFields: List<String>
    ): OpenAireLoadResult {
        val results = mutableListOf<DatasetSearchResult>()
        val seenResults = mutableSetOf<String>()
        val warnings = mutableListOf<DatasetSearchWarning>()

        var page = 1
        var firstResponse: DatasetSearchResponse? = null

        while (page <= MAX_SOURCE_PAGES) {
            val response = DatasetSearchService.search(
                DatasetSearchRequest(
                    criteria = criteria,
                    sort = emptyList(),
                    selectedFields = selectedFields,
                    page = page,
                    pageSize = INTERNAL_PAGE_SIZE
                )
            )

            if (firstResponse == null) {
                firstResponse = response
            }

            warnings.addAll(response.warnings)

            val sizeBefore = results.size

            response.results.forEach { result ->
                val key = result.id.ifBlank {
                    listOf(
                        result.title,
                        result.publicationDate,
                        result.authors
                    ).joinToString("|")
                }

                if (seenResults.add(key)) {
                    results.add(result)
                }
            }

            if (
                !response.hasNextPage ||
                response.results.isEmpty()
            ) {
                break
            }

            if (results.size == sizeBefore) {
                warnings.add(
                    DatasetSearchWarning(
                        source = DatasetSourceType.OPENAIRE,
                        code = "HYBRID_OPENAIRE_PAGINATION_STALLED",
                        message =
                            "OpenAIRE pagination stopped because a new page " +
                                "did not provide new results."
                    )
                )
                break
            }

            if (page == MAX_SOURCE_PAGES) {
                warnings.add(
                    DatasetSearchWarning(
                        source = DatasetSourceType.OPENAIRE,
                        code = "HYBRID_OPENAIRE_RESULT_LIMIT",
                        message =
                            "The hybrid search reached the internal OpenAIRE " +
                                "limit of " +
                                "${MAX_SOURCE_PAGES * INTERNAL_PAGE_SIZE} results."
                    )
                )
                break
            }

            page++
        }

        return OpenAireLoadResult(
            results = results,
            firstResponse = firstResponse,
            warnings = warnings.distinct()
        )
    }

    /**
     * Percorre a paginação OPL em blocos de 100.
     */
    private fun loadAllOplResults(
        ontologyManager: OntologyManager,
        criteria: List<DatasetSearchCriterion>
    ): OplLoadResult {
        val results = mutableListOf<OplDatasetResult>()
        val seenIds = mutableSetOf<String>()
        val warnings = mutableListOf<String>()

        var page = 1
        var firstResponse: OplDatasetSearchResponse? = null

        while (page <= MAX_SOURCE_PAGES) {
            val response = OplDatasetSearchService.search(
                ontologyManager = ontologyManager,
                request = OplDatasetSearchRequest(
                    criteria = criteria.map { criterion ->
                        OplDatasetSearchCriterion(
                            field = criterion.field,
                            operator = criterion.operator.name,
                            value = criterion.value
                        )
                    },
                    page = page,
                    pageSize = INTERNAL_PAGE_SIZE,
                    sortField = null,
                    sortOrder = null
                )
            )

            if (firstResponse == null) {
                firstResponse = response
            }

            warnings.addAll(response.warnings)

            val sizeBefore = results.size

            response.results.forEach { result ->
                if (seenIds.add(result.id)) {
                    results.add(result)
                }
            }

            if (!response.hasNextPage) {
                break
            }

            if (results.size == sizeBefore) {
                warnings.add(
                    "OPL pagination stopped because a new page did not " +
                        "provide new results."
                )
                break
            }

            if (page == MAX_SOURCE_PAGES) {
                warnings.add(
                    "The hybrid search reached the internal OPL limit of " +
                        "${MAX_SOURCE_PAGES * INTERNAL_PAGE_SIZE} results."
                )
                break
            }

            page++
        }

        return OplLoadResult(
            results = results,
            firstResponse = firstResponse,
            warnings = warnings.distinct()
        )
    }

    /**
     * Mantém os objetos openAire/opl existentes no contrato HTTP sem devolver
     * todos os resultados internos usados pelo join.
     */
    private fun buildOpenAireSummary(
        loadResult: OpenAireLoadResult
    ): DatasetSearchResponse {
        val firstResponse = loadResult.firstResponse

        return DatasetSearchResponse(
            queryString = firstResponse?.queryString.orEmpty(),
            page = 1,
            pageSize = INTERNAL_PAGE_SIZE,
            totalResults = loadResult.results.size.toLong(),
            hasNextPage = loadResult.results.size > INTERNAL_PAGE_SIZE,
            selectedFields = firstResponse?.selectedFields ?: emptyList(),
            results = loadResult.results.take(INTERNAL_PAGE_SIZE),
            warnings = loadResult.warnings
        )
    }

    private fun buildOplSummary(
        loadResult: OplLoadResult
    ): OplDatasetSearchResponse =
        OplDatasetSearchResponse(
            page = 1,
            pageSize = INTERNAL_PAGE_SIZE,
            totalResults = loadResult.results.size,
            hasNextPage = loadResult.results.size > INTERNAL_PAGE_SIZE,
            results = loadResult.results.take(INTERNAL_PAGE_SIZE),
            warnings = loadResult.warnings
        )

    /**
     * Associa cada resultado OpenAIRE ao melhor problema OPL.
     *
     * A associação considera:
     * - name;
     * - longName;
     * - referenceTitles.
     *
     * Um valor OPL tem de estar contido no título OpenAIRE.
     * Em caso de múltiplos candidatos, vence o valor mais comprido.
     */
    private fun joinResults(
        openAireResults: List<DatasetSearchResult>,
        oplResults: List<OplDatasetResult>
    ): List<HybridDatasetResult> =
        openAireResults.mapNotNull { openAireResult ->
            val oplMatch = findBestOplMatch(
                openAireResult = openAireResult,
                oplResults = oplResults
            )

            oplMatch?.let { oplResult ->
                mapHybridResult(
                    openAireResult = openAireResult,
                    oplResult = oplResult
                )
            }
        }

    /**
     * Procura a correspondência OPL mais específica.
     */
    private fun findBestOplMatch(
        openAireResult: DatasetSearchResult,
        oplResults: List<OplDatasetResult>
    ): OplDatasetResult? {
        val normalizedTitle =
            normalizeText(openAireResult.title)

        if (normalizedTitle.isBlank()) {
            return null
        }

        return oplResults
            .mapNotNull { oplResult ->
                val score = calculateMatchScore(
                    normalizedTitle = normalizedTitle,
                    oplResult = oplResult
                )

                if (score > 0) {
                    OplMatchCandidate(
                        result = oplResult,
                        score = score
                    )
                } else {
                    null
                }
            }
            .maxWithOrNull(
                compareBy<OplMatchCandidate> { candidate ->
                    candidate.score
                }.thenBy { candidate ->
                    candidate.result.name?.length ?: 0
                }
            )
            ?.result
    }

    /**
     * Calcula a qualidade da associação.
     *
     * name recebe maior prioridade.
     * longName recebe prioridade intermédia.
     * referenceTitles recebem prioridade inferior.
     */
    private fun calculateMatchScore(
        normalizedTitle: String,
        oplResult: OplDatasetResult
    ): Int {
        var bestScore = 0

        val normalizedName =
            normalizeText(oplResult.name)

        if (
            normalizedName.length >= 3 &&
            normalizedTitle.contains(normalizedName)
        ) {
            bestScore = maxOf(
                bestScore,
                10_000 + normalizedName.length
            )
        }

        val normalizedLongName =
            normalizeText(oplResult.longName)

        if (
            normalizedLongName.length >= 3 &&
            normalizedTitle.contains(normalizedLongName)
        ) {
            bestScore = maxOf(
                bestScore,
                5_000 + normalizedLongName.length
            )
        }

        oplResult.referenceTitles.forEach { referenceTitle ->
            val normalizedReference =
                normalizeText(referenceTitle)

            if (
                normalizedReference.length >= 5 &&
                (
                    normalizedTitle.contains(normalizedReference) ||
                        normalizedReference.contains(normalizedTitle)
                    )
            ) {
                bestScore = maxOf(
                    bestScore,
                    1_000 + minOf(
                        normalizedReference.length,
                        normalizedTitle.length
                    )
                )
            }
        }

        return bestScore
    }

    /**
     * Cria uma linha que contém campos das duas fontes.
     */
    private fun mapHybridResult(
        openAireResult: DatasetSearchResult,
        oplResult: OplDatasetResult
    ): HybridDatasetResult =
        HybridDatasetResult(
            id =
                "${openAireResult.id}::${oplResult.id}",

            openAireId = openAireResult.id,
            type = openAireResult.type,
            title = openAireResult.title,
            authors = openAireResult.authors,
            publicationDate =
                openAireResult.publicationDate,
            publisher = openAireResult.publisher,
            citations = openAireResult.citations,
            relatedMaterials =
                openAireResult.relatedMaterials,

            oplId = oplResult.id,
            oplName = oplResult.name,
            oplLongName = oplResult.longName,
            oplDescription = oplResult.description,
            oplObjectives = oplResult.objectives,
            oplVariableTypes = oplResult.variableTypes,
            oplVariableDimensions =
                oplResult.variableDimensions,
            oplConstraintTypes =
                oplResult.constraintTypes,
            oplNumberOfConstraints =
                oplResult.numberOfConstraints,
            oplModality = oplResult.modality,
            oplNoiseType = oplResult.noiseType,
            oplType = oplResult.type,
            oplAuthors = oplResult.authors,
            oplReferenceTitles =
                oplResult.referenceTitles,
            oplLinks = oplResult.links
        )

    /**
     * Ordena os resultados híbridos depois do join.
     *
     * Suporta vários critérios de ordenação na ordem em que foram enviados.
     */
    private fun sortHybridResults(
        results: List<HybridDatasetResult>,
        sort: List<DatasetSort>
    ): List<HybridDatasetResult> {
        if (sort.isEmpty()) {
            return results
        }

        return results.sortedWith(
            Comparator { left, right ->
                for (sortItem in sort) {
                    val comparison = compareHybridField(
                        left = left,
                        right = right,
                        sort = sortItem
                    )

                    if (comparison != 0) {
                        return@Comparator comparison
                    }
                }

                left.id.compareTo(
                    right.id,
                    ignoreCase = true
                )
            }
        )
    }

    private fun compareHybridField(
        left: HybridDatasetResult,
        right: HybridDatasetResult,
        sort: DatasetSort
    ): Int {
        if (
            sort.field == "citations" ||
            sort.field == "citationCount"
        ) {
            return compareNullableNumbers(
                left = left.citations,
                right = right.citations,
                direction = sort.direction
            )
        }

        val leftValue = getHybridSortValue(
            result = left,
            field = sort.field
        )

        val rightValue = getHybridSortValue(
            result = right,
            field = sort.field
        )

        // Valores vazios ficam sempre no fim, independentemente da direção.
        if (leftValue.isBlank() && rightValue.isBlank()) {
            return 0
        }

        if (leftValue.isBlank()) {
            return 1
        }

        if (rightValue.isBlank()) {
            return -1
        }

        val comparison = leftValue.compareTo(
            rightValue,
            ignoreCase = true
        )

        return if (sort.direction == DatasetSortDirection.ASC) {
            comparison
        } else {
            -comparison
        }
    }

    private fun compareNullableNumbers(
        left: Int?,
        right: Int?,
        direction: DatasetSortDirection
    ): Int {
        if (left == null && right == null) {
            return 0
        }

        if (left == null) {
            return 1
        }

        if (right == null) {
            return -1
        }

        val comparison = left.compareTo(right)

        return if (direction == DatasetSortDirection.ASC) {
            comparison
        } else {
            -comparison
        }
    }

    private fun getHybridSortValue(
        result: HybridDatasetResult,
        field: String
    ): String =
        when (field) {
            // OpenAIRE
            "openAireId" -> result.openAireId
            "type" -> result.type
            "title",
            "mainTitle" -> result.title
            "authors" -> result.authors
            "publicationDate" -> result.publicationDate
            "publisher" -> result.publisher.orEmpty()
            "citations",
            "citationCount" -> result.citations?.toString().orEmpty()
            "relatedMaterials" -> result.relatedMaterials.orEmpty()

            // OPL - aliases e nomes não prefixados para compatibilidade
            "oplId" -> result.oplId
            "name",
            "oplName" -> result.oplName.orEmpty()
            "longName",
            "oplLongName" -> result.oplLongName.orEmpty()
            "description",
            "oplDescription" -> result.oplDescription.orEmpty()
            "objectives",
            "oplObjectives" -> result.oplObjectives.joinToString(", ")
            "variableTypes",
            "oplVariableTypes" -> result.oplVariableTypes.joinToString(", ")
            "variableDimensions",
            "oplVariableDimensions" ->
                result.oplVariableDimensions.joinToString(", ")
            "constraintTypes",
            "oplConstraintTypes" -> result.oplConstraintTypes.joinToString(", ")
            "numberOfConstraints",
            "oplNumberOfConstraints" ->
                result.oplNumberOfConstraints.joinToString(", ")
            "modality",
            "oplModality" -> result.oplModality.orEmpty()
            "noiseType",
            "oplNoiseType" -> result.oplNoiseType.orEmpty()
            "oplType" -> result.oplType.orEmpty()
            "oplAuthors" -> result.oplAuthors.joinToString(", ")
            "referenceTitles",
            "oplReferenceTitles" ->
                result.oplReferenceTitles.joinToString(", ")
            "links",
            "oplLinks" -> result.oplLinks.joinToString(", ")

            else -> ""
        }

    /**
     * Normaliza texto para permitir correspondência consistente.
     */
    private fun normalizeText(
        value: String?
    ): String {
        if (value.isNullOrBlank()) {
            return ""
        }

        val withoutAccents =
            Normalizer.normalize(
                value,
                Normalizer.Form.NFD
            )
                .replace(
                    Regex("\\p{M}+"),
                    ""
                )

        return withoutAccents
            .lowercase()
            .replace(
                Regex("[^a-z0-9]+"),
                " "
            )
            .trim()
            .replace(
                Regex("\\s+"),
                " "
            )
    }

    private fun validateRequest(
        request: HybridDatasetSearchRequest
    ) {
        require(request.page >= 1) {
            "Page must be greater than or equal to 1."
        }

        require(request.pageSize in 1..100) {
            "Page size must be between 1 and 100."
        }

        require(request.criteria.isNotEmpty()) {
            "At least one search criterion is required."
        }

        request.criteria.forEachIndexed { index, criterion ->
            require(
                criterion.source == DatasetSourceType.OPENAIRE ||
                    criterion.source == DatasetSourceType.OPL
            ) {
                "Criterion ${index + 1} must use OPENAIRE or OPL."
            }

            require(criterion.field.isNotBlank()) {
                "The field of criterion ${index + 1} is required."
            }

            require(criterion.value.isNotBlank()) {
                "The value of criterion ${index + 1} is required."
            }

            require(
                criterion.operator == DatasetCriterionOperator.INCLUDE ||
                    criterion.operator == DatasetCriterionOperator.EXCLUDE
            ) {
                "Unsupported operator in criterion ${index + 1}."
            }

            if (
                criterion.source == DatasetSourceType.OPENAIRE &&
                criterion.operator == DatasetCriterionOperator.EXCLUDE
            ) {
                require(criterion.field in openAireExcludeFields) {
                    "OpenAIRE EXCLUDE is supported only for mainTitle and subjects."
                }
            }
        }

        request.sort.forEachIndexed { index, sort ->
            require(sort.field in supportedHybridSortFields) {
                "Unsupported hybrid sort field in sort ${index + 1}: ${sort.field}."
            }
        }
    }

    private data class OpenAireLoadResult(
        val results: List<DatasetSearchResult>,
        val firstResponse: DatasetSearchResponse?,
        val warnings: List<DatasetSearchWarning>
    )

    private data class OplLoadResult(
        val results: List<OplDatasetResult>,
        val firstResponse: OplDatasetSearchResponse?,
        val warnings: List<String>
    )

    private data class OplMatchCandidate(
        val result: OplDatasetResult,
        val score: Int
    )
}
