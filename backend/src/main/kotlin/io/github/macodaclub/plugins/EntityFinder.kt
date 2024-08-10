package io.github.macodaclub.plugins

import io.github.macodaclub.utils.getLabel
import io.github.macodaclub.utils.lenient
import io.github.macodaclub.utils.type
import org.semanticweb.owlapi.model.OWLEntity
import org.semanticweb.owlapi.model.OWLOntology

class EntityFinder(private val mergedOntology: OWLOntology, private val lemmatize: String.() -> String) {

    fun findEntity(text: String) =
        entitiesMappedByLength[text.length]?.get(text.lowercase()) ?: run {
            val lenient = text.lenient(lemmatize)
            entitiesMappedByLength[lenient.length]?.get(lenient)
        }

    fun searchEntities(query: String, type: String?) =
        if (type == null) {
            mergedOntology.signature.filter { it.getLabel(mergedOntology).lowercase().startsWith(query) }
        } else {
            entitiesByType.getValue(type).filter { it.getLabel(mergedOntology).lowercase().startsWith(query) }
        }

    fun searchEntities(query: String, types: List<String>) =
        types.flatMap { type ->
            entitiesByType.getValue(type).filter { it.getLabel(mergedOntology).lowercase().startsWith(query) }
        }

    private var entitiesMappedByLength: MutableMap<Int, MutableMap<String, Result>> =
        mutableMapOf()

    private val entitiesByType = mergedOntology.signature.groupBy { entity ->
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
        mergedOntology.signature.forEach { entity ->
            val label = mergedOntology.getAnnotationAssertionAxioms(entity.iri)
                .find { it.property.isLabel }?.value?.asLiteral()?.orNull()?.literal
                ?.substringBefore("@")
                ?.trim('"')
                ?.lowercase()
            val lenientLabel = label?.lenient(lemmatize)
            // TODO: Support Alt Labels
            val comment = mergedOntology.getAnnotationAssertionAxioms(entity.iri)
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

fun configureEntityFinder(mergedOntology: OWLOntology, lemmatize: String.() -> String): EntityFinder {
    return EntityFinder(mergedOntology, lemmatize)
}