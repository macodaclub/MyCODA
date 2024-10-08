package io.github.macodaclub.utils

import io.github.macodaclub.models.api.tree.GetTreeResponse
import io.github.macodaclub.models.api.submission.PostSubmitFormResponse
import io.github.macodaclub.plugins.EntityFinder
import kotlinx.serialization.json.*
import org.semanticweb.owlapi.model.*
import org.semanticweb.owlapi.reasoner.OWLReasoner

val prettyJson = Json { prettyPrint = true }

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
        iri.toString(),
        getLabel(ontology),
        "Class",
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
        iri.toString(),
        getLabel(ontology),
        "Property",
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
        iri.toString(),
        getLabel(ontology),
        "Property",
        reasoner.getSubObjectProperties(this, true).filterNot { it.isBottomNode }.count(),
        reasoner.getSubObjectProperties(this, false).filterNot { it.isBottomNode }.count(),
        getExpandedChildren()
    )

fun OWLNamedIndividual.toTaxonomyTreeEntity(
    ontology: OWLOntology,
) =
    GetTreeResponse.TaxonomyTree.Entity(
        iri.toString(),
        getLabel(ontology),
        "Individual",
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
                            entity.iri.toString(),
                            entity.getLabel(mergedOntology),
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

fun List<JsonObject>.toHtmlTable(): String {
    if (this.isEmpty()) return "<table></table>"
    val headers = this.first().keys
    val tableBuilder = StringBuilder()
    tableBuilder.append("<table border='1'>")
    tableBuilder.append("<tr>")
    headers.forEach { header ->
        val formattedHeader = header
            .replace(Regex("([a-z])([A-Z])"), "$1 $2")
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        tableBuilder.append("<th>$formattedHeader</th>")
    }
    tableBuilder.append("</tr>")
    this.forEach { jsonObject ->
        tableBuilder.append("<tr>")
        headers.forEach { header ->
            val cellValue = jsonObject[header]?.let { value ->
                when (value) {
                    is JsonPrimitive -> value.toHtml()
                    is JsonObject -> {
                        val jsonObj = value.jsonObject
                        if (jsonObj.keys.size == 2 && jsonObj.keys.containsAll(listOf("literal", "individual"))) {
                            if (jsonObj["literal"] is JsonNull) {
                                jsonObj["individual"]?.jsonObject?.entityToHtml().toString()
                            } else {
                                jsonObj["literal"]?.jsonPrimitive?.toHtml().toString()
                            }
                        } else if (jsonObj.keys.size == 3 && jsonObj.keys.containsAll(listOf("iri", "label", "type"))) {
                            jsonObj.entityToHtml()
                        } else {
                            jsonObj.toHtmlTable()
                        }
                    }

                    is JsonArray -> {
                        val jsonArray = value.jsonArray
                        if (jsonArray.isEmpty()) {
                            ""
                        } else {
                            if (jsonArray.first() is JsonObject) {
                                Json.decodeFromJsonElement<List<JsonObject>>(jsonArray).toHtmlTable()
                            } else {
                                jsonArray.toString()
                            }
                        }
                    }

                    else -> value.toString()
                }
            } ?: ""
            tableBuilder.append("<td>$cellValue</td>")
        }
        tableBuilder.append("</tr>")
    }
    tableBuilder.append("</table>")
    return tableBuilder.toString()
}

fun JsonObject.toHtmlTable(): String {
    val stringBuilder = StringBuilder()
    stringBuilder.append("<table border=\"1\" cellspacing=\"0\" cellpadding=\"5\">\n")
    for ((key, value) in this) {
        if (value is JsonNull) continue
        val formattedHeader = key
            .replace(Regex("([a-z])([A-Z])"), "$1 $2")
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        stringBuilder.append("<tr>")
        stringBuilder.append("<td><strong>").append(formattedHeader).append("</strong></td>")
        when (value) {
            is JsonPrimitive -> stringBuilder.append("<td>").append(value.toHtml()).append("</td>")
            is JsonObject -> {
                val jsonObj = value.jsonObject
                if (jsonObj.keys.size == 3 && jsonObj.keys.containsAll(listOf("iri", "label", "type"))) {
                    jsonObj.entityToHtml()
                }
                stringBuilder.append("<td>").append(value.toHtmlTable()).append("</td>")
            }

            else -> stringBuilder.append("<td>").append(value.toString()).append("</td>")
        }
        stringBuilder.append("</tr>\n")
    }
    stringBuilder.append("</table>")
    return stringBuilder.toString()
}

fun JsonObject.entityToHtml() = """
<details>
<summary>${this["label"]?.jsonPrimitive?.content}</summary>

<br />
${this.toHtmlTable()}
</details>
"""

fun JsonPrimitive.toHtml() = if(content.startsWith("http://") || content.startsWith("https://")) {
    """<a href="$content" target="_blank">$content</a>"""
} else {
    content
}

fun String.getInitials() =
    filterIndexed { i, c -> i == 0 || !this[i - 1].isLetterOrDigit() }.uppercase()

fun hammingDistance(str1: String, str2: String): Int {
    val minLength = minOf(str1.length, str2.length)
    val maxLength = maxOf(str1.length, str2.length)
    val baseDistance = str1.zip(str2).count { (char1, char2) -> char1.lowercaseChar() != char2.lowercaseChar() }
    return baseDistance + (maxLength - minLength)
}