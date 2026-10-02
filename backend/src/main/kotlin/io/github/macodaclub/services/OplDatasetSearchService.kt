package io.github.macodaclub.services

import io.github.macodaclub.models.api.datasets.OplDatasetResult
import io.github.macodaclub.models.api.datasets.OplDatasetSearchCriterion
import io.github.macodaclub.models.api.datasets.OplDatasetSearchRequest
import io.github.macodaclub.models.api.datasets.OplDatasetSearchResponse

/**
 * Pesquisa da Optimisation Problem Library diretamente sobre o catálogo OPL.
 * Não depende da ontologia MyCODA nem de SQWRL.
 */
object OplDatasetSearchService {

    private val supportedFields = setOf(
        "id", "name", "longName", "description", "objectives", "variableTypes", "variableDimensions",
        "constraintTypes", "numberOfConstraints", "constraintHardness", "constraintEquality", "modality",
        "noiseType", "type", "source", "problemSources", "dynamicTypes", "fidelityLevels", "evaluationTimes",
        "problems", "instances", "codeExamples", "implementationIds", "implementationNames", "implementationLinks",
        "implementationLanguages", "implementationEvaluationTimes", "tags", "allowsPartialEvaluation",
        "canEvaluateObjectivesIndependently", "authors", "referenceTitles", "links"
    )

    private val supportedOperators = setOf("INCLUDE", "EXCLUDE")

    private val exactMatchFields = setOf(
        "objectives", "variableTypes", "constraintTypes", "constraintHardness", "constraintEquality",
        "type", "source", "problemSources", "dynamicTypes", "fidelityLevels",
        "allowsPartialEvaluation", "canEvaluateObjectivesIndependently"
    )

    fun search(request: OplDatasetSearchRequest): OplDatasetSearchResponse {
        validateRequest(request)

        val catalog = OplCatalogClient.load()
        val filteredResults = applyCriteria(catalog.results, request.criteria)
        val sortedResults = sortResults(filteredResults, request.sortField, request.sortOrder)
        val totalResults = sortedResults.size
        val fromIndex = (request.page - 1) * request.pageSize
        val toIndex = minOf(fromIndex + request.pageSize, totalResults)
        val pageResults = if (fromIndex >= totalResults) emptyList() else sortedResults.subList(fromIndex, toIndex)

        return OplDatasetSearchResponse(
            page = request.page,
            pageSize = request.pageSize,
            totalResults = totalResults,
            hasNextPage = toIndex < totalResults,
            results = pageResults,
            warnings = catalog.warnings,
            sourceUrl = catalog.sourceUrl
        )
    }

    /** Campos diferentes = AND; INCLUDE repetido no mesmo campo = OR; EXCLUDE remove qualquer correspondência. */
    private fun applyCriteria(results: List<OplDatasetResult>, criteria: List<OplDatasetSearchCriterion>): List<OplDatasetResult> {
        if (criteria.isEmpty()) return results

        val groupedCriteria = criteria.groupBy { it.field }
        return results.filter { result ->
            groupedCriteria.all { (_, fieldCriteria) ->
                val includeCriteria = fieldCriteria.filter { it.operator.equals("INCLUDE", ignoreCase = true) }
                val excludeCriteria = fieldCriteria.filter { it.operator.equals("EXCLUDE", ignoreCase = true) }

                val includeMatches = includeCriteria.isEmpty() || includeCriteria.any { matchesValue(result, it) }
                val excludeMatches = excludeCriteria.all { !matchesValue(result, it) }
                includeMatches && excludeMatches
            }
        }
    }

    private fun matchesValue(result: OplDatasetResult, criterion: OplDatasetSearchCriterion): Boolean {
        val searchedValue = criterion.value.trim()
        return getFieldValues(result, criterion.field).any { currentValue ->
            if (criterion.field in exactMatchFields) {
                currentValue.equals(searchedValue, ignoreCase = true)
            } else {
                currentValue.contains(searchedValue, ignoreCase = true)
            }
        }
    }

    private fun getFieldValues(result: OplDatasetResult, field: String): List<String> = when (field) {
        "id" -> listOf(result.id)
        "name" -> listOfNotNull(result.name)
        "longName" -> listOfNotNull(result.longName)
        "description" -> listOfNotNull(result.description)
        "objectives" -> result.objectives
        "variableTypes" -> result.variableTypes
        "variableDimensions" -> result.variableDimensions
        "constraintTypes" -> result.constraintTypes
        "numberOfConstraints" -> result.numberOfConstraints
        "constraintHardness" -> result.constraintHardness
        "constraintEquality" -> result.constraintEquality
        "modality" -> listOfNotNull(result.modality)
        "noiseType" -> listOfNotNull(result.noiseType)
        "type" -> listOfNotNull(result.type)
        "source" -> listOf(result.source)
        "problemSources" -> result.problemSources
        "dynamicTypes" -> result.dynamicTypes
        "fidelityLevels" -> result.fidelityLevels
        "evaluationTimes" -> result.evaluationTimes
        "problems" -> result.problems
        "instances" -> result.instances
        "codeExamples" -> result.codeExamples
        "implementationIds" -> result.implementationIds
        "implementationNames" -> result.implementationNames
        "implementationLinks" -> result.implementationLinks
        "implementationLanguages" -> result.implementationLanguages
        "implementationEvaluationTimes" -> result.implementationEvaluationTimes
        "tags" -> result.tags
        "allowsPartialEvaluation" -> listOfNotNull(result.allowsPartialEvaluation)
        "canEvaluateObjectivesIndependently" -> listOfNotNull(result.canEvaluateObjectivesIndependently)
        "authors" -> result.authors
        "referenceTitles" -> result.referenceTitles
        "links" -> result.links
        else -> throw IllegalArgumentException("Unsupported OPL search field: $field")
    }

    private fun sortResults(results: List<OplDatasetResult>, sortField: String?, sortOrder: String?): List<OplDatasetResult> {
        if (sortField.isNullOrBlank()) {
            return results.sortedWith(compareBy<OplDatasetResult> { it.name?.lowercase().orEmpty() }.thenBy { it.id.lowercase() })
        }

        val comparator = compareBy<OplDatasetResult> { getSortValue(it, sortField).lowercase() }
        return when (sortOrder?.trim()?.uppercase()) {
            null, "", "ASC", "ASCENDING" -> results.sortedWith(comparator)
            "DESC", "DESCENDING" -> results.sortedWith(comparator.reversed())
            else -> throw IllegalArgumentException("Unsupported OPL sort order: $sortOrder")
        }
    }

    private fun getSortValue(result: OplDatasetResult, field: String): String = getFieldValues(result, field).joinToString(", ")

    private fun validateRequest(request: OplDatasetSearchRequest) {
        require(request.page >= 1) { "Page must be greater than or equal to 1." }
        require(request.pageSize in 1..100) { "Page size must be between 1 and 100." }

        request.criteria.forEach { criterion ->
            require(criterion.field.isNotBlank()) { "The OPL criterion field is required." }
            require(criterion.field in supportedFields) { "Unsupported OPL search field: ${criterion.field}" }
            require(criterion.value.isNotBlank()) { "The OPL criterion value is required." }
            require(criterion.operator.trim().uppercase() in supportedOperators) { "Unsupported OPL operator: ${criterion.operator}" }
        }

        if (!request.sortField.isNullOrBlank()) {
            require(request.sortField in supportedFields) { "Unsupported OPL sort field: ${request.sortField}" }
        }
    }
}
