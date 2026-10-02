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

/**
 * Pesquisa combinada OpenAIRE + OPL.
 *
 * A implementação segue o fluxo Node-RED atualmente ligado: os critérios são
 * separados por fonte, cada fonte com critérios é pesquisada de forma independente
 * (uma fonte sem critérios é ignorada) e os dois conjuntos de resultados são
 * concatenados.
 */
object HybridDatasetSearchService {

    private const val INTERNAL_PAGE_SIZE = 100
    private const val MAX_SOURCE_PAGES = 100

    private val supportedHybridSortFields = setOf(
        "source",

        // OpenAIRE
        "openAireId", "type", "instanceType", "title", "mainTitle", "abstract", "subjects", "authors",
        "publicationDate", "publisher", "scientificEvent", "citations", "citationCount", "influenceClass",
        "popularityClass", "impulseClass", "citationCountClass", "relatedMaterials", "countryCode", "sdg", "fos",

        // OPL (aliases e nomes não prefixados usados pelo frontend)
        "oplId", "name", "oplName", "longName", "oplLongName", "description", "oplDescription",
        "objectives", "oplObjectives", "variableTypes", "oplVariableTypes", "variableDimensions",
        "oplVariableDimensions", "constraintTypes", "oplConstraintTypes", "numberOfConstraints",
        "oplNumberOfConstraints", "modality", "oplModality", "noiseType", "oplNoiseType", "oplType",
        "oplAuthors", "referenceTitles", "oplReferenceTitles", "links", "oplLinks",
        "oplConstraintHardness", "oplConstraintEquality", "oplProblemSources", "oplDynamicTypes",
        "oplFidelityLevels", "oplEvaluationTimes", "oplProblems", "oplInstances", "oplCodeExamples",
        "oplImplementationIds", "oplImplementationNames", "oplImplementationLinks", "oplImplementationLanguages",
        "oplImplementationEvaluationTimes", "oplTags", "oplAllowsPartialEvaluation",
        "oplCanEvaluateObjectivesIndependently"
    )

    fun search(request: HybridDatasetSearchRequest): HybridDatasetSearchResponse {
        validateRequest(request)

        val openAireCriteria = request.criteria.filter { it.source == DatasetSourceType.OPENAIRE }
        val oplCriteria = request.criteria.filter { it.source == DatasetSourceType.OPL }

        val openAireLoadResult = if (openAireCriteria.isEmpty()) {
            OpenAireLoadResult(emptyList(), null, emptyList())
        } else {
            loadAllOpenAireResults(openAireCriteria, request.selectedFields)
        }
        val oplLoadResult = if (oplCriteria.isEmpty()) {
            OplLoadResult(emptyList(), null, emptyList())
        } else {
            loadAllOplResults(oplCriteria)
        }

        val combinedResults = buildList {
            addAll(openAireLoadResult.results.map(::mapOpenAireResult))
            addAll(oplLoadResult.results.map(::mapOplResult))
        }

        val sortedResults = sortHybridResults(combinedResults, request.sort)
        val totalResults = sortedResults.size
        val fromIndex = (request.page - 1) * request.pageSize
        val toIndex = minOf(fromIndex + request.pageSize, totalResults)
        val pageResults = if (fromIndex >= totalResults) emptyList() else sortedResults.subList(fromIndex, toIndex)

        val warnings = buildList {
            addAll(openAireLoadResult.warnings)
            oplLoadResult.warnings.forEach { warning ->
                add(DatasetSearchWarning(DatasetSourceType.OPL, "OPL_RESULT_WARNING", warning))
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

            if (firstResponse == null) firstResponse = response
            warnings.addAll(response.warnings)
            val sizeBefore = results.size

            response.results.forEach { result ->
                val key = result.id.ifBlank { listOf(result.title, result.publicationDate, result.authors).joinToString("|") }
                if (seenResults.add(key)) results.add(result)
            }

            if (!response.hasNextPage || response.results.isEmpty()) break

            if (results.size == sizeBefore) {
                warnings.add(
                    DatasetSearchWarning(
                        source = DatasetSourceType.OPENAIRE,
                        code = "HYBRID_OPENAIRE_PAGINATION_STALLED",
                        message = "OpenAIRE pagination stopped because a new page did not provide new results."
                    )
                )
                break
            }

            if (page == MAX_SOURCE_PAGES) {
                warnings.add(
                    DatasetSearchWarning(
                        source = DatasetSourceType.OPENAIRE,
                        code = "HYBRID_OPENAIRE_RESULT_LIMIT",
                        message = "The combined search reached the internal OpenAIRE limit of ${MAX_SOURCE_PAGES * INTERNAL_PAGE_SIZE} results."
                    )
                )
                break
            }

            page++
        }

        return OpenAireLoadResult(results, firstResponse, warnings.distinct())
    }

    private fun loadAllOplResults(criteria: List<DatasetSearchCriterion>): OplLoadResult {
        val results = mutableListOf<OplDatasetResult>()
        val seenIds = mutableSetOf<String>()
        val warnings = mutableListOf<String>()
        var page = 1
        var firstResponse: OplDatasetSearchResponse? = null

        while (page <= MAX_SOURCE_PAGES) {
            val response = OplDatasetSearchService.search(
                OplDatasetSearchRequest(
                    criteria = criteria.map { criterion ->
                        OplDatasetSearchCriterion(criterion.field, criterion.operator.name, criterion.value)
                    },
                    page = page,
                    pageSize = INTERNAL_PAGE_SIZE
                )
            )

            if (firstResponse == null) firstResponse = response
            warnings.addAll(response.warnings)
            val sizeBefore = results.size

            response.results.forEach { result -> if (seenIds.add(result.id)) results.add(result) }
            if (!response.hasNextPage) break

            if (results.size == sizeBefore) {
                warnings.add("OPL pagination stopped because a new page did not provide new results.")
                break
            }

            if (page == MAX_SOURCE_PAGES) {
                warnings.add("The combined search reached the internal OPL limit of ${MAX_SOURCE_PAGES * INTERNAL_PAGE_SIZE} results.")
                break
            }

            page++
        }

        return OplLoadResult(results, firstResponse, warnings.distinct())
    }

    private fun buildOpenAireSummary(loadResult: OpenAireLoadResult): DatasetSearchResponse {
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

    private fun buildOplSummary(loadResult: OplLoadResult): OplDatasetSearchResponse =
        OplDatasetSearchResponse(
            page = 1,
            pageSize = INTERNAL_PAGE_SIZE,
            totalResults = loadResult.results.size,
            hasNextPage = loadResult.results.size > INTERNAL_PAGE_SIZE,
            results = loadResult.results.take(INTERNAL_PAGE_SIZE),
            warnings = loadResult.warnings,
            sourceUrl = loadResult.firstResponse?.sourceUrl
        )

    private fun mapOpenAireResult(result: DatasetSearchResult): HybridDatasetResult =
        HybridDatasetResult(
            id = "OPENAIRE::${result.id}",
            source = DatasetSourceType.OPENAIRE,
            openAireId = result.id,
            type = result.type,
            instanceType = result.instanceType,
            title = result.title,
            abstract = result.abstract,
            subjects = result.subjects,
            authors = result.authors,
            authorDetails = result.authorDetails,
            publicationDate = result.publicationDate,
            publisher = result.publisher,
            scientificEvent = result.scientificEvent,
            citations = result.citations,
            influenceClass = result.influenceClass,
            popularityClass = result.popularityClass,
            impulseClass = result.impulseClass,
            citationCountClass = result.citationCountClass,
            relatedMaterials = result.relatedMaterials,
            countryCode = result.countryCode,
            sdg = result.sdg,
            fos = result.fos
        )

    private fun mapOplResult(result: OplDatasetResult): HybridDatasetResult =
        HybridDatasetResult(
            id = "OPL::${result.id}",
            source = DatasetSourceType.OPL,
            oplId = result.id,
            oplName = result.name,
            oplLongName = result.longName,
            oplDescription = result.description,
            oplObjectives = result.objectives,
            oplVariableTypes = result.variableTypes,
            oplVariableDimensions = result.variableDimensions,
            oplConstraintTypes = result.constraintTypes,
            oplNumberOfConstraints = result.numberOfConstraints,
            oplConstraintHardness = result.constraintHardness,
            oplConstraintEquality = result.constraintEquality,
            oplModality = result.modality,
            oplNoiseType = result.noiseType,
            oplType = result.type,
            oplProblemSources = result.problemSources,
            oplDynamicTypes = result.dynamicTypes,
            oplFidelityLevels = result.fidelityLevels,
            oplEvaluationTimes = result.evaluationTimes,
            oplProblems = result.problems,
            oplInstances = result.instances,
            oplCodeExamples = result.codeExamples,
            oplImplementationIds = result.implementationIds,
            oplImplementationNames = result.implementationNames,
            oplImplementationLinks = result.implementationLinks,
            oplImplementationLanguages = result.implementationLanguages,
            oplImplementationEvaluationTimes = result.implementationEvaluationTimes,
            oplTags = result.tags,
            oplAllowsPartialEvaluation = result.allowsPartialEvaluation,
            oplCanEvaluateObjectivesIndependently = result.canEvaluateObjectivesIndependently,
            oplAuthors = result.authors,
            oplReferenceTitles = result.referenceTitles,
            oplLinks = result.links
        )

    private fun sortHybridResults(results: List<HybridDatasetResult>, sort: List<DatasetSort>): List<HybridDatasetResult> {
        if (sort.isEmpty()) return results

        return results.sortedWith(
            Comparator { left, right ->
                for (sortItem in sort) {
                    val comparison = compareHybridField(left, right, sortItem)
                    if (comparison != 0) return@Comparator comparison
                }
                left.id.compareTo(right.id, ignoreCase = true)
            }
        )
    }

    private fun compareHybridField(left: HybridDatasetResult, right: HybridDatasetResult, sort: DatasetSort): Int {
        if (sort.field == "citations" || sort.field == "citationCount") {
            return compareNullableNumbers(left.citations, right.citations, sort.direction)
        }

        val leftValue = getHybridSortValue(left, sort.field)
        val rightValue = getHybridSortValue(right, sort.field)
        if (leftValue.isBlank() && rightValue.isBlank()) return 0
        if (leftValue.isBlank()) return 1
        if (rightValue.isBlank()) return -1

        val comparison = leftValue.compareTo(rightValue, ignoreCase = true)
        return if (sort.direction == DatasetSortDirection.ASC) comparison else -comparison
    }

    private fun compareNullableNumbers(left: Int?, right: Int?, direction: DatasetSortDirection): Int {
        if (left == null && right == null) return 0
        if (left == null) return 1
        if (right == null) return -1
        val comparison = left.compareTo(right)
        return if (direction == DatasetSortDirection.ASC) comparison else -comparison
    }

    private fun getHybridSortValue(result: HybridDatasetResult, field: String): String = when (field) {
        "source" -> result.source.name
        "openAireId" -> result.openAireId.orEmpty()
        "type" -> result.type.orEmpty()
        "instanceType" -> result.instanceType.orEmpty()
        "title", "mainTitle" -> result.title.orEmpty()
        "abstract" -> result.abstract.orEmpty()
        "subjects" -> result.subjects.joinToString(", ")
        "authors" -> result.authors.orEmpty()
        "publicationDate" -> result.publicationDate.orEmpty()
        "publisher" -> result.publisher.orEmpty()
        "scientificEvent" -> result.scientificEvent.orEmpty()
        "citations", "citationCount" -> result.citations?.toString().orEmpty()
        "influenceClass" -> result.influenceClass.orEmpty()
        "popularityClass" -> result.popularityClass.orEmpty()
        "impulseClass" -> result.impulseClass.orEmpty()
        "citationCountClass" -> result.citationCountClass.orEmpty()
        "relatedMaterials" -> result.relatedMaterials.orEmpty()
        "countryCode" -> result.countryCode.orEmpty()
        "sdg" -> result.sdg.joinToString(", ")
        "fos" -> result.fos.joinToString(", ")
        "oplId" -> result.oplId.orEmpty()
        "name", "oplName" -> result.oplName.orEmpty()
        "longName", "oplLongName" -> result.oplLongName.orEmpty()
        "description", "oplDescription" -> result.oplDescription.orEmpty()
        "objectives", "oplObjectives" -> result.oplObjectives.joinToString(", ")
        "variableTypes", "oplVariableTypes" -> result.oplVariableTypes.joinToString(", ")
        "variableDimensions", "oplVariableDimensions" -> result.oplVariableDimensions.joinToString(", ")
        "constraintTypes", "oplConstraintTypes" -> result.oplConstraintTypes.joinToString(", ")
        "numberOfConstraints", "oplNumberOfConstraints" -> result.oplNumberOfConstraints.joinToString(", ")
        "modality", "oplModality" -> result.oplModality.orEmpty()
        "noiseType", "oplNoiseType" -> result.oplNoiseType.orEmpty()
        "oplType" -> result.oplType.orEmpty()
        "oplAuthors" -> result.oplAuthors.joinToString(", ")
        "referenceTitles", "oplReferenceTitles" -> result.oplReferenceTitles.joinToString(", ")
        "links", "oplLinks" -> result.oplLinks.joinToString(", ")
        "oplConstraintHardness" -> result.oplConstraintHardness.joinToString(", ")
        "oplConstraintEquality" -> result.oplConstraintEquality.joinToString(", ")
        "oplProblemSources" -> result.oplProblemSources.joinToString(", ")
        "oplDynamicTypes" -> result.oplDynamicTypes.joinToString(", ")
        "oplFidelityLevels" -> result.oplFidelityLevels.joinToString(", ")
        "oplEvaluationTimes" -> result.oplEvaluationTimes.joinToString(", ")
        "oplProblems" -> result.oplProblems.joinToString(", ")
        "oplInstances" -> result.oplInstances.joinToString(", ")
        "oplCodeExamples" -> result.oplCodeExamples.joinToString(", ")
        "oplImplementationIds" -> result.oplImplementationIds.joinToString(", ")
        "oplImplementationNames" -> result.oplImplementationNames.joinToString(", ")
        "oplImplementationLinks" -> result.oplImplementationLinks.joinToString(", ")
        "oplImplementationLanguages" -> result.oplImplementationLanguages.joinToString(", ")
        "oplImplementationEvaluationTimes" -> result.oplImplementationEvaluationTimes.joinToString(", ")
        "oplTags" -> result.oplTags.joinToString(", ")
        "oplAllowsPartialEvaluation" -> result.oplAllowsPartialEvaluation.orEmpty()
        "oplCanEvaluateObjectivesIndependently" -> result.oplCanEvaluateObjectivesIndependently.orEmpty()
        else -> ""
    }

    private fun validateRequest(request: HybridDatasetSearchRequest) {
        require(request.page >= 1) { "Page must be greater than or equal to 1." }
        require(request.pageSize in 1..100) { "Page size must be between 1 and 100." }
        require(request.criteria.isNotEmpty()) { "At least one search criterion is required." }

        request.criteria.forEachIndexed { index, criterion ->
            require(criterion.source == DatasetSourceType.OPENAIRE || criterion.source == DatasetSourceType.OPL) {
                "Criterion ${index + 1} must use OPENAIRE or OPL."
            }
            require(criterion.field.isNotBlank()) { "The field of criterion ${index + 1} is required." }
            require(criterion.value.isNotBlank()) { "The value of criterion ${index + 1} is required." }
            require(criterion.operator == DatasetCriterionOperator.INCLUDE || criterion.operator == DatasetCriterionOperator.EXCLUDE) {
                "Unsupported operator in criterion ${index + 1}."
            }
        }

        request.sort.forEach { sort ->
            require(sort.field in supportedHybridSortFields) { "Unsupported hybrid sort field: ${sort.field}" }
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
}
