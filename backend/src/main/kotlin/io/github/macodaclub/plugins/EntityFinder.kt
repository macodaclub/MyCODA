package io.github.macodaclub.plugins

import io.github.macodaclub.utils.*
import org.semanticweb.owlapi.model.OWLEntity

class EntityFinder(private val ontologyManager: OntologyManager, private val lemmatize: String.() -> String) {

    fun findEntity(text: String) =
        entitiesMappedByLength[text.length]?.get(text.lowercase()) ?: run {
            val lenient = text.lenient(lemmatize)
            entitiesMappedByLength[lenient.length]?.get(lenient)
        }

    fun searchEntities(query: String, type: String?) =
        if (type == null) {
            (ontologyManager.mergedOntology.signature
                .filter { it.getLabel(ontologyManager.mergedOntology).lowercase().startsWith(query) }
                    + ontologyManager.mergedOntology.signature
                .filter { it.getLabel(ontologyManager.mergedOntology).lowercase().contains(query) }
                    ).distinctBy { it.iri }
        } else {
            (entitiesByType.getValue(type)
                .filter { it.getLabel(ontologyManager.mergedOntology).lowercase().startsWith(query) }
                    + entitiesByType.getValue(type)
                .filter { it.getLabel(ontologyManager.mergedOntology).lowercase().contains(query) }
                    ).distinctBy { it.iri }
        }

    fun searchEntities(query: String, types: List<String>?) =
        types?.flatMap { type ->
            (entitiesByType.getValue(type)
                .filter { it.getLabel(ontologyManager.mergedOntology).lowercase().startsWith(query) }
                    + entitiesByType.getValue(type)
                .filter { it.getLabel(ontologyManager.mergedOntology).lowercase().contains(query) }
                    ).distinctBy { it.iri }
        }
            ?: (ontologyManager.mergedOntology.signature
                .filter { it.getLabel(ontologyManager.mergedOntology).lowercase().startsWith(query) }
                    + ontologyManager.mergedOntology.signature
                .filter { it.getLabel(ontologyManager.mergedOntology).lowercase().contains(query) }
                    ).distinctBy { it.iri }

    fun findSynonymSuggestions(query: String): List<OWLEntity> {
        val initials = query.getInitials()
        val suggestionsByInitials =
            ontologyManager.mergedOntology.signature.filter {
                it.getLabel(ontologyManager.mergedOntology).uppercase() == initials
            } + if (initials.length > 1) {
                entityInitials.getOrDefault(query.uppercase(), emptyList())
            } else {
                emptyList()
            }.take(3)
        val suggestionsByHammingDistance = ontologyManager.mergedOntology.signature.associateWith {
            hammingDistance(it.getLabel(ontologyManager.mergedOntology).lowercase(), query.lowercase())
        }.filterValues { it <= query.length / 2 }.toList().sortedBy { it.second }.map { it.first }.take(3)
        return (suggestionsByInitials + suggestionsByHammingDistance).distinctBy { it.iri }
    }

    private val entitiesMappedByLength: MutableMap<Int, MutableMap<String, Result>> =
        mutableMapOf()

    private val entityInitials: MutableMap<String, List<OWLEntity>> = mutableMapOf<String, List<OWLEntity>>().apply {
        ontologyManager.mergedOntology.signature.forEach { entity ->
            val initials = entity.getLabel(ontologyManager.mergedOntology).getInitials()
            if (initials.length <= 1) return@forEach
            computeIfPresent(initials) { _, value -> value + entity }
            computeIfAbsent(initials) { listOf(entity) }
        }
    }

    private val entitiesByType = ontologyManager.mergedOntology.signature.groupBy { entity ->
        entity.type
    }.filter { (key, _) -> key != null }

    data class Result(
        val entity: OWLEntity,
        val comparisonField: ComparisonField
    )

    enum class ComparisonField(val priority: Int) {
        Label(0),
        AltLabel(1),
        PartialIri(2),
        Comment(3),
        LenientLabel(4),
        LenientComment(5),
    }

    init {
        ontologyManager.mergedOntology.signature.forEach { entity ->
            val label = ontologyManager.mergedOntology.getAnnotationAssertionAxioms(entity.iri)
                .find { it.property.isLabel }?.value?.asLiteral()?.orNull()?.literal
                ?.substringBefore("@")
                ?.trim('"')
                ?.lowercase()
            val lenientLabel = label?.lenient(lemmatize)
            // TODO: Support Alt Labels
            val comment = ontologyManager.mergedOntology.getAnnotationAssertionAxioms(entity.iri)
                .find { it.property.isComment }
                ?.value?.asLiteral()?.orNull()?.literal
                ?.lowercase()
            val lenientComment = comment?.lenient(lemmatize)
            val partialIri = (entity.iri.remainder.orNull() ?: entity.iri.shortForm)
                .lowercase()
            mapEntityByLength(entity, label, ComparisonField.Label)
            mapEntityByLength(entity, partialIri, ComparisonField.PartialIri)
            mapEntityByLength(entity, comment, ComparisonField.Comment)
            mapEntityByLength(entity, lenientLabel, ComparisonField.LenientLabel)
            mapEntityByLength(entity, lenientComment, ComparisonField.LenientComment)
        }
    }

    private fun mapEntityByLength(entity: OWLEntity, value: String?, comparisonField: ComparisonField) {
        if (value == null) return
        if (entitiesMappedByLength.containsKey(value.length)) {
            val mappedEntities = entitiesMappedByLength.getValue(value.length)
            if (mappedEntities.containsKey(value)) {
                val (_, existingComparisonField) = mappedEntities.getValue(value)
                if (comparisonField.priority < existingComparisonField.priority) {
                    mappedEntities[value] = Result(entity, comparisonField)
                }
            } else {
                mappedEntities[value] = Result(entity, comparisonField)
            }
        } else {
            entitiesMappedByLength[value.length] = mutableMapOf(value to Result(entity, comparisonField))
        }
    }
}

fun configureEntityFinder(ontologyManager: OntologyManager, lemmatize: String.() -> String): EntityFinder {
    return EntityFinder(ontologyManager, lemmatize)
}