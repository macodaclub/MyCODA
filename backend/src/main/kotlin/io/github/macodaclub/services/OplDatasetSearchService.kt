package io.github.macodaclub.services

import io.github.macodaclub.models.api.datasets.OplDatasetResult
import io.github.macodaclub.models.api.datasets.OplDatasetSearchCriterion
import io.github.macodaclub.models.api.datasets.OplDatasetSearchRequest
import io.github.macodaclub.models.api.datasets.OplDatasetSearchResponse
import io.github.macodaclub.plugins.OntologyManager
import org.semanticweb.owlapi.model.OWLNamedIndividual

/**
 * Serviço responsável pela pesquisa de problemas de otimização
 * existentes na OPL representada na ontologia MyCODA.
 *
 */
object OplDatasetSearchService {

    private const val OPL_CLASS =
        "OWLClass_OPLOptimisationProblem"

    private val supportedFields = setOf(
        "id",
        "name",
        "longName",
        "description",
        "objectives",
        "variableTypes",
        "variableDimensions",
        "constraintTypes",
        "numberOfConstraints",
        "modality",
        "noiseType",
        "type",
        "authors",
        "referenceTitles",
        "links"
    )

    private val supportedOperators = setOf(
        "INCLUDE",
        "EXCLUDE"
    )

    /**
     * Executa a pesquisa, aplica critérios, ordenação e paginação.
     */
    fun search(
        ontologyManager: OntologyManager,
        request: OplDatasetSearchRequest
    ): OplDatasetSearchResponse {
        validateRequest(request)

        val loadResult = loadOplResults(
            ontologyManager = ontologyManager
        )

        val filteredResults = applyCriteria(
            results = loadResult.results,
            criteria = request.criteria
        )

        val sortedResults = sortResults(
            results = filteredResults,
            sortField = request.sortField,
            sortOrder = request.sortOrder
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

        return OplDatasetSearchResponse(
            page = request.page,
            pageSize = request.pageSize,
            totalResults = totalResults,
            hasNextPage = toIndex < totalResults,
            results = pageResults,
            warnings = loadResult.warnings
        )
    }

    /**
     * Obtém os problemas OPL através de SQWRL e associa-os
     * aos indivíduos nomeados existentes na ontologia.
     */
    private fun loadOplResults(
        ontologyManager: OntologyManager
    ): OplLoadResult {
        val ontology = ontologyManager.mergedOntology

        val queryName =
            "opl_dataset_search_${System.nanoTime()}"

        val queryString =
            "$OPL_CLASS(?problem) -> sqwrl:select(?problem)"

        val queryResults =
            ontologyManager.sqwrlQueryEngine.runSQWRLQuery(
                queryName,
                queryString
            )

        val oplShortNames = queryResults
            .getColumn(0)
            .mapNotNull { result ->
                val rawValue =
                    if (result.isEntity) {
                        result
                            .asEntityResult()
                            .shortName
                    } else {
                        result.toString()
                    }

                normalizeName(rawValue)
                    .takeIf { it.isNotBlank() }
            }
            .distinct()

        /*
         * Recolhe os indivíduos nomeados de toda a cadeia
         * de imports da ontologia.
         */
        val allOntologyIndividuals = ontology.importsClosure
            .flatMap { importedOntology ->
                importedOntology.individualsInSignature
            }
            .distinctBy { individual ->
                individual.iri
            }

        /*
         * Cria um índice com várias representações possíveis
         * do nome de cada indivíduo.
         */
        val individualsByName =
            buildMap<String, OWLNamedIndividual> {
                allOntologyIndividuals.forEach { individual ->
                    val shortForm =
                        individual.iri.shortForm.trim()

                    val normalizedShortForm =
                        normalizeName(shortForm)

                    put(shortForm, individual)
                    put(shortForm.lowercase(), individual)

                    put(
                        normalizedShortForm,
                        individual
                    )

                    put(
                        normalizedShortForm.lowercase(),
                        individual
                    )
                }
            }

        val mappedIndividuals = oplShortNames
            .mapNotNull { shortName ->
                findIndividual(
                    shortName = shortName,
                    individualsByName = individualsByName
                )
            }
            .distinctBy { individual ->
                individual.iri
            }

        if (
            oplShortNames.isNotEmpty() &&
            mappedIndividuals.isEmpty()
        ) {
            throw IllegalStateException(
                "SQWRL found ${oplShortNames.size} OPL problems, " +
                    "but none could be mapped to named ontology individuals."
            )
        }

        val mappedNames = mappedIndividuals
            .flatMap { individual ->
                val shortForm =
                    individual.iri.shortForm.trim()

                listOf(
                    shortForm,
                    shortForm.lowercase(),
                    normalizeName(shortForm),
                    normalizeName(shortForm).lowercase()
                )
            }
            .toSet()

        val missingNames = oplShortNames
            .filterNot { shortName ->
                shortName in mappedNames ||
                    shortName.lowercase() in mappedNames ||
                    normalizeName(shortName) in mappedNames ||
                    normalizeName(shortName).lowercase() in mappedNames
            }
            .distinct()

        val results = mappedIndividuals
            .map { individual ->
                mapIndividual(
                    ontologyManager = ontologyManager,
                    individual = individual
                )
            }

        val warnings = buildList {
            if (missingNames.isNotEmpty()) {
                add(
                    "${missingNames.size} SQWRL result(s) were excluded " +
                        "because they are not available as named ontology " +
                        "individuals: ${missingNames.joinToString(", ")}."
                )
            }
        }

        return OplLoadResult(
            results = results,
            warnings = warnings
        )
    }

    /**
     * Procura um indivíduo através das várias formas possíveis
     * do respetivo nome.
     */
    private fun findIndividual(
        shortName: String,
        individualsByName: Map<String, OWLNamedIndividual>
    ): OWLNamedIndividual? {
        val normalized =
            normalizeName(shortName)

        return individualsByName[shortName]
            ?: individualsByName[shortName.lowercase()]
            ?: individualsByName[normalized]
            ?: individualsByName[normalized.lowercase()]
    }

    /**
     * Remove prefixos e representações adicionais dos nomes
     * devolvidos pelo motor SQWRL.
     */
    private fun normalizeName(
        value: String
    ): String =
        value
            .trim()
            .removePrefix("<")
            .removeSuffix(">")
            .substringAfterLast("#")
            .substringAfterLast("/")
            .substringAfterLast(":")
            .trim()

    /**
     * Converte um indivíduo da ontologia num resultado OPL.
     */
    private fun mapIndividual(
        ontologyManager: OntologyManager,
        individual: OWLNamedIndividual
    ): OplDatasetResult {
        val values = getDataPropertyValues(
            ontologyManager = ontologyManager,
            individual = individual
        )

        return OplDatasetResult(
            id = individual.iri.shortForm,

            name = values.firstValue(
                "opl_has_name"
            ),

            longName = values.firstValue(
                "opl_has_long_name"
            ),

            description = values.firstValue(
                "opl_has_description"
            ),

            objectives = values.values(
                "opl_has_objectives"
            ),

            variableTypes = values.values(
                "opl_has_variables_type"
            ),

            variableDimensions = values.values(
                "opl_has_variables_dim"
            ),

            constraintTypes = values.values(
                "opl_has_constraints_type"
            ),

            numberOfConstraints = values.values(
                "opl_has_constraints_number"
            ),

            modality = values.firstValue(
                "opl_has_modality"
            ),

            noiseType = values.firstValue(
                "opl_has_noise_type"
            ),

            type = values.firstValue(
                "opl_has_type"
            ),

            source = "OPL",

            authors = values.values(
                "opl_has_references_authors"
            ),

            referenceTitles = values.values(
                "opl_has_references_title"
            ),

            links = buildList {
                addAll(
                    values.values(
                        "opl_has_links"
                    )
                )

                addAll(
                    values.values(
                        "opl_has_links_url"
                    )
                )

                addAll(
                    values.values(
                        "opl_has_references_link_url"
                    )
                )
            }
                .map { value -> value.trim() }
                .filter { value -> value.isNotBlank() }
                .distinct()
        )
    }

    /**
     * Obtém as data properties do indivíduo em toda
     * a cadeia de imports da ontologia.
     */
    private fun getDataPropertyValues(
        ontologyManager: OntologyManager,
        individual: OWLNamedIndividual
    ): Map<String, List<String>> {
        val ontology =
            ontologyManager.mergedOntology

        return ontology.importsClosure
            .flatMap { importedOntology ->
                importedOntology
                    .getDataPropertyAssertionAxioms(
                        individual
                    )
            }
            .groupBy(
                keySelector = { axiom ->
                    axiom.property
                        .asOWLDataProperty()
                        .iri
                        .shortForm
                },
                valueTransform = { axiom ->
                    axiom.`object`.literal
                }
            )
    }

    /**
     * Aplica os critérios da pesquisa.
     *
     * Campos diferentes são combinados com AND.
     * Valores repetidos para o mesmo campo são combinados com OR.
     */
    private fun applyCriteria(
        results: List<OplDatasetResult>,
        criteria: List<OplDatasetSearchCriterion>
    ): List<OplDatasetResult> {
        if (criteria.isEmpty()) {
            return results
        }

        val groupedCriteria =
            criteria.groupBy { criterion ->
                criterion.field
            }

        return results.filter { result ->
            groupedCriteria.all { (_, fieldCriteria) ->
                fieldCriteria.any { criterion ->
                    matchesCriterion(
                        result = result,
                        criterion = criterion
                    )
                }
            }
        }
    }

    /**
     * Verifica se um resultado corresponde a um critério.
     */
    private fun matchesCriterion(
        result: OplDatasetResult,
        criterion: OplDatasetSearchCriterion
    ): Boolean {
        val searchedValue =
            criterion.value.trim()

        val fieldValues = getFieldValues(
            result = result,
            field = criterion.field
        )

        val containsValue =
            fieldValues.any { currentValue ->
                currentValue.contains(
                    other = searchedValue,
                    ignoreCase = true
                )
            }

        return when (
            criterion.operator.trim().uppercase()
        ) {
            "INCLUDE" -> containsValue
            "EXCLUDE" -> !containsValue

            else -> throw IllegalArgumentException(
                "Unsupported OPL operator: " +
                    criterion.operator
            )
        }
    }

    /**
     * Obtém os valores pesquisáveis de um campo.
     */
    private fun getFieldValues(
        result: OplDatasetResult,
        field: String
    ): List<String> =
        when (field) {
            "id" ->
                listOf(result.id)

            "name" ->
                listOfNotNull(result.name)

            "longName" ->
                listOfNotNull(result.longName)

            "description" ->
                listOfNotNull(result.description)

            "objectives" ->
                result.objectives

            "variableTypes" ->
                result.variableTypes

            "variableDimensions" ->
                result.variableDimensions

            "constraintTypes" ->
                result.constraintTypes

            "numberOfConstraints" ->
                result.numberOfConstraints

            "modality" ->
                listOfNotNull(result.modality)

            "noiseType" ->
                listOfNotNull(result.noiseType)

            "type" ->
                listOfNotNull(result.type)

            "authors" ->
                result.authors

            "referenceTitles" ->
                result.referenceTitles

            "links" ->
                result.links

            else -> throw IllegalArgumentException(
                "Unsupported OPL search field: $field"
            )
        }

    /**
     * Ordena os resultados.
     */
    private fun sortResults(
        results: List<OplDatasetResult>,
        sortField: String?,
        sortOrder: String?
    ): List<OplDatasetResult> {
        if (sortField.isNullOrBlank()) {
            return results.sortedBy { result ->
                result.name
                    ?.lowercase()
                    ?: result.id.lowercase()
            }
        }

        val comparator =
            compareBy<OplDatasetResult> { result ->
                getSortValue(
                    result = result,
                    field = sortField
                ).lowercase()
            }

        return when (
            sortOrder?.trim()?.uppercase()
        ) {
            null,
            "",
            "ASC",
            "ASCENDING" ->
                results.sortedWith(comparator)

            "DESC",
            "DESCENDING" ->
                results.sortedWith(
                    comparator.reversed()
                )

            else -> throw IllegalArgumentException(
                "Unsupported OPL sort order: $sortOrder"
            )
        }
    }

    /**
     * Obtém o valor utilizado para ordenar cada resultado.
     */
    private fun getSortValue(
        result: OplDatasetResult,
        field: String
    ): String =
        when (field) {
            "id" ->
                result.id

            "name" ->
                result.name.orEmpty()

            "longName" ->
                result.longName.orEmpty()

            "description" ->
                result.description.orEmpty()

            "type" ->
                result.type.orEmpty()

            "modality" ->
                result.modality.orEmpty()

            "noiseType" ->
                result.noiseType.orEmpty()

            "objectives" ->
                result.objectives.joinToString(", ")

            "variableTypes" ->
                result.variableTypes.joinToString(", ")

            "variableDimensions" ->
                result.variableDimensions.joinToString(", ")

            "constraintTypes" ->
                result.constraintTypes.joinToString(", ")

            "numberOfConstraints" ->
                result.numberOfConstraints.joinToString(", ")

            "authors" ->
                result.authors.joinToString(", ")

            "referenceTitles" ->
                result.referenceTitles.joinToString(", ")

            "links" ->
                result.links.joinToString(", ")

            else -> throw IllegalArgumentException(
                "Unsupported OPL sort field: $field"
            )
        }

    /**
     * Valida o pedido recebido pelo endpoint.
     */
    private fun validateRequest(
        request: OplDatasetSearchRequest
    ) {
        require(request.page >= 1) {
            "Page must be greater than or equal to 1."
        }

        require(request.pageSize in 1..100) {
            "Page size must be between 1 and 100."
        }

        request.criteria.forEach { criterion ->
            require(criterion.field.isNotBlank()) {
                "The OPL criterion field is required."
            }

            require(criterion.field in supportedFields) {
                "Unsupported OPL search field: " +
                    criterion.field
            }

            require(criterion.value.isNotBlank()) {
                "The OPL criterion value is required."
            }

            require(
                criterion.operator.trim().uppercase() in
                    supportedOperators
            ) {
                "Unsupported OPL operator: " +
                    criterion.operator
            }
        }

        if (!request.sortField.isNullOrBlank()) {
            require(
                request.sortField in supportedFields
            ) {
                "Unsupported OPL sort field: " +
                    request.sortField
            }
        }
    }

    private fun Map<String, List<String>>.firstValue(
        property: String
    ): String? =
        this[property]
            ?.map { value -> value.trim() }
            ?.firstOrNull { value ->
                value.isNotBlank()
            }

    private fun Map<String, List<String>>.values(
        property: String
    ): List<String> =
        this[property]
            .orEmpty()
            .map { value -> value.trim() }
            .filter { value -> value.isNotBlank() }
            .distinct()

    /**
     * Resultado interno do carregamento dos problemas OPL.
     */
    private data class OplLoadResult(
        val results: List<OplDatasetResult>,
        val warnings: List<String>
    )
}