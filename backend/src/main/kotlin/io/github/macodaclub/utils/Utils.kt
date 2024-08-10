package io.github.macodaclub.utils

import io.github.macodaclub.models.tree.GetTreeResponse
import io.github.macodaclub.models.submission.PostSubmitFormResponse
import io.github.macodaclub.plugins.EntityFinder
import org.semanticweb.owlapi.model.*
import org.semanticweb.owlapi.reasoner.OWLReasoner

context(T) fun <T> contextReceiver(): T = this@T

fun OWLNamedObject.getLabel(ontology: OWLOntology) =
    ontology.getAnnotationAssertionAxioms(iri).find { it.property.isLabel }?.value?.asLiteral()
        ?.orNull()?.literal?.substringBefore("@")?.trim('"') ?: iri.remainder.orNull() ?: iri.shortForm

val OWLEntity.simpleType
    get() =
        when {
            isOWLClass -> "Class"
            isOWLDataProperty || isOWLObjectProperty -> "Property"
            isOWLNamedIndividual -> "Individual"
            isOWLDatatype -> "Datatype"
            else -> null
        }

val OWLEntity.type
    get() =
        when {
            isOWLClass -> "Class"
            isOWLDataProperty -> "DataProperty"
            isOWLObjectProperty -> "ObjectProperty"
            isOWLNamedIndividual -> "Individual"
            isOWLDatatype -> "Datatype"
            else -> null
        }

fun OWLClass.toTaxonomyTreeEntity(
    ontology: OWLOntology,
    reasoner: OWLReasoner,
    getExpandedChildren: () -> List<GetTreeResponse.TaxonomyTree.Entity>? = { null },
) =
    GetTreeResponse.TaxonomyTree.Entity(
        getLabel(ontology),
        "Class",
        iri.toString(),
        reasoner.getSubClasses(this, true).filterNot { it.isBottomNode }.count(),
        reasoner.getSubClasses(this, false).filterNot { it.isBottomNode }.count(),
        getExpandedChildren()
    )

fun OWLDataProperty.toTaxonomyTreeEntity(
    ontology: OWLOntology,
    reasoner: OWLReasoner,
    getExpandedChildren: () -> List<GetTreeResponse.TaxonomyTree.Entity>? = { null },
) =
    GetTreeResponse.TaxonomyTree.Entity(
        getLabel(ontology),
        "Property",
        iri.toString(),
        reasoner.getSubDataProperties(this, true).filterNot { it.isBottomNode }.count(),
        reasoner.getSubDataProperties(this, false).filterNot { it.isBottomNode }.count(),
        getExpandedChildren()
    )

fun OWLObjectProperty.toTaxonomyTreeEntity(
    ontology: OWLOntology,
    reasoner: OWLReasoner,
    getExpandedChildren: () -> List<GetTreeResponse.TaxonomyTree.Entity>? = { null },
) =
    GetTreeResponse.TaxonomyTree.Entity(
        getLabel(ontology),
        "Property",
        iri.toString(),
        reasoner.getSubObjectProperties(this, true).filterNot { it.isBottomNode }.count(),
        reasoner.getSubObjectProperties(this, false).filterNot { it.isBottomNode }.count(),
        getExpandedChildren()
    )

fun OWLNamedIndividual.toTaxonomyTreeEntity(
    ontology: OWLOntology,
) =
    GetTreeResponse.TaxonomyTree.Entity(
        getLabel(ontology),
        "Individual",
        iri.toString(),
        0,
        0,
        emptyList()
    )

data class TextSegment(
    val isWord: Boolean,
    val value: String,
    val range: IntRange
)

val defaultWordDelimiterRegex = """[^\w#+]+""".toRegex()
fun String.splitInTextSegments(regex: Regex = defaultWordDelimiterRegex): List<TextSegment> {
    val matcher = regex.toPattern().matcher(this)

    val result = mutableListOf<TextSegment>()
    var lastStart = 0

    if (isEmpty()) return emptyList()
    if (!matcher.find()) return listOf(TextSegment(true, this, indices))

    do {
        if (matcher.start() > lastStart) {
            val substr = substring(lastStart, matcher.start())
            result.add(
                TextSegment(substr.contains("""\w""".toRegex()), substr, lastStart..matcher.start())
            )
        }
        result.add(
            TextSegment(false, substring(matcher.start(), matcher.end()), matcher.start()..matcher.end())
        )
        lastStart = matcher.end()
    } while (matcher.find())

    if (lastStart + 1 < length) {
        result.add(
            TextSegment(true, substring(lastStart, length), lastStart..length)
        )
    }
    return result
}

fun String.lenient(lemmatize: String.() -> String) =
    lemmatize()
        .replace("""'.|\W""".toRegex(), "")
        .lowercase()

data class FindEntityReferencesResult(
    val referencedEntities: List<PostSubmitFormResponse.Entity>,
    val textSegments: List<List<PostSubmitFormResponse.TextSegment>>,
)

fun List<String>.findEntityReferences(
    entityFinder: EntityFinder,
    mergedOntology: OWLOntology
): FindEntityReferencesResult {
    class EntityReference(
        val entity: PostSubmitFormResponse.Entity,
        val comparisonField: EntityFinder.ComparisonField,
        val rawTextSegmentIndexes: IntRange,
    )

    val rawTextSegments = map { it.splitInTextSegments() }
    val entityReferences = rawTextSegments.map { textSegments ->
        val words = textSegments.withIndex().filter { (_, it) -> it.isWord }
        (1..words.size).flatMap { windowSize ->
            words.windowed(windowSize).mapNotNull { windowedWords ->
                val text = windowedWords.joinToString(" ") { it.value.value }
                val result = entityFinder.findEntity(text)
                result?.let { (entity, comparisonField) ->
                    EntityReference(
                        PostSubmitFormResponse.Entity(
                            entity.getLabel(mergedOntology),
                            entity.iri.toString(),
                            entity.simpleType ?: return@mapNotNull null
                        ),
                        comparisonField,
                        windowedWords.first().index..windowedWords.last().index,
                    )
                }
            }
        }
            .fold(mutableListOf<EntityReference>()) { acc, entityReference ->
                val intersectingEntity = acc.find {
                    it.rawTextSegmentIndexes.intersect(entityReference.rawTextSegmentIndexes).isNotEmpty()
                }
                if (intersectingEntity == null) {
                    acc.add(entityReference)
                } else {
                    if (entityReference.comparisonField > intersectingEntity.comparisonField || entityReference.rawTextSegmentIndexes.count() > intersectingEntity.rawTextSegmentIndexes.count()) {
                        acc[acc.indexOf(intersectingEntity)] = entityReference
                    }
                }
                acc
            }
            .sortedBy { it.rawTextSegmentIndexes.first }
    }
    val referencedEntities = entityReferences.flatMap { refs -> refs.map { it.entity } }.distinct()
    val textSegments =
        rawTextSegments.zip(entityReferences).map { (segments, refs) ->
            segments.foldIndexed(mutableListOf<PostSubmitFormResponse.TextSegment>()) { index, acc, textSegment ->
                val entityReference = refs.find { it.rawTextSegmentIndexes.contains(index) }
                if (entityReference == null) {
                    acc.add(PostSubmitFormResponse.TextSegment(textSegment.value, null))
                } else {
                    if (acc.lastOrNull()?.entityReferenceIndex == null) {
                        acc.add(
                            PostSubmitFormResponse.TextSegment(
                                segments.subList(
                                    entityReference.rawTextSegmentIndexes.first,
                                    entityReference.rawTextSegmentIndexes.last + 1
                                ).joinToString("") { it.value },
                                referencedEntities.indexOf(entityReference.entity)
                            )
                        )
                    }
                }
                acc
            }
        }
    return FindEntityReferencesResult(
        referencedEntities,
        textSegments
    )
}