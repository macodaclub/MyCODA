package io.github.macodaclub.utils

import io.github.macodaclub.models.GetTreeResponse
import org.semanticweb.owlapi.model.*
import org.semanticweb.owlapi.reasoner.OWLReasoner

fun OWLNamedObject.getLabel(ontology: OWLOntology) =
    ontology.getAnnotationAssertionAxioms(iri).find { it.property.isLabel }?.value?.asLiteral()
        ?.orNull()?.literal?.substringBefore("@")?.trim('"') ?: iri.remainder.orNull() ?: iri.shortForm

val OWLEntity.simpleType get() =
    when {
        isOWLClass -> "Class"
        isOWLDataProperty || isOWLObjectProperty -> "Property"
        isOWLNamedIndividual -> "Individual"
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

fun CharSequence.splitIndexed(regex: Regex, limit: Int = 0): List<Pair<String, Int>> {
    val matcher = regex.toPattern().matcher(this)
    if (limit == 1 || !matcher.find()) return listOf(Pair(this.toString(), 0))

    val result = ArrayList<Pair<String, Int>>(if (limit > 0) limit.coerceAtMost(10) else 10)
    var lastStart = 0
    val lastSplit = limit - 1 // negative if there's no limit

    do {
        result.add(Pair(this.substring(lastStart, matcher.start()), lastStart))
        lastStart = matcher.end()
        if (lastSplit >= 0 && result.size == lastSplit) break
    } while (matcher.find())

    result.add(Pair(this.substring(lastStart, this.length), lastStart))

    return result
}