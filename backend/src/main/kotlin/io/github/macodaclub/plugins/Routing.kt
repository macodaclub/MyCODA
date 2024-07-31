package io.github.macodaclub.plugins

import io.github.macodaclub.models.*
import io.github.macodaclub.utils.getLabel
import io.github.macodaclub.utils.simpleType
import io.github.macodaclub.utils.splitIndexed
import io.github.macodaclub.utils.toTaxonomyTreeEntity
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.semanticweb.owlapi.model.IRI
import org.semanticweb.owlapi.model.OWLOntology
import org.semanticweb.owlapi.model.parameters.Imports
import org.semanticweb.owlapi.reasoner.OWLReasoner
import org.swrlapi.sqwrl.SQWRLQueryEngine
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min


fun Application.configureRouting(
    ontology: OWLOntology,
    mergedOntology: OWLOntology,
    reasoner: OWLReasoner,
    queryEngine: SQWRLQueryEngine
) {
    /*install(StatusPages) {
        exception<Throwable> { call, cause ->
            call.respondText(text = "500: $cause", status = HttpStatusCode.InternalServerError)
        }
    }*/
    routing {
        staticResources("/", "website") {
            default("index.html")
        }
        staticResources("/static", "static")
        get("/ontologies/{pathIri}") {
            val fullIri = URLBuilder().apply {
                protocol = URLProtocol.createOrDefault(call.request.local.scheme)
                host = call.request.host()
                port = call.request.port()
                path(call.request.path())
            }.buildString()
            call.respondRedirect {
                protocol = URLProtocol.createOrDefault(call.request.local.scheme)
                host = call.request.host()
                port =
                    call.application.environment.config.propertyOrNull("ktor.frontendPort")?.getString()?.toIntOrNull()
                        ?: call.request.port()
                path("/browse")
                parameters.append("iri", fullIri)
            }
        }
        route("/api") {
            get("/sqwrl") {
                // TODO: Take care of SQWRL queries with axioms starting with a number (e.g. 2p-NSGA-II) and featuring / (e.g. iMOEA/D)
                val results =
                    queryEngine.runSQWRLQuery(
                        "q1",
                        "MetaHeuristic(?_MetaHeuristic_) ^ hasDevelopingYear(?_MetaHeuristic_, ?_hasDevelopingYear_) ^ swrlb:greaterThanOrEqual(?_hasDevelopingYear_, 2000) -> sqwrl:select(?_MetaHeuristic_) ^ sqwrl:select(?_hasDevelopingYear_) ^ sqwrl:orderBy(?_hasDevelopingYear_)"
                    )
                val resultsColumn = results.getColumn(0)
                call.respondText(resultsColumn.toString())
            }
            post("/sqwrl") {
                val queryString = call.receive<PostSqwrlRequest>().queryString
                // TODO: Take care of SQWRL queries with axioms starting with a number (e.g. 2p-NSGA-II) and featuring / (e.g. iMOEA/D)
                val results =
                    queryEngine.runSQWRLQuery("q1", queryString)
                val resultsColumn = results.getColumn(0)
                call.respond<List<String>>(
                    resultsColumn.mapNotNull { result ->
                        when {
                            result.isEntity -> {
                                val entityResult = result.asEntityResult()
                                entityResult.shortName.substringAfter(":")
                            }
                            // TODO: Support non-entity results
                            else -> null
                        }
                    }.distinct()
                )
            }
            route("/tree") {
                get {
                    val type = call.request.queryParameters["type"] ?: "Class"
                    when (type) {
                        "Class" -> {
                            call.respond(
                                GetTreeResponse(
                                    GetTreeResponse.TaxonomyTree(
                                        mergedOntology.classesInSignature
                                            .filter {
                                                !it.isTopEntity && reasoner.getSuperClasses(it, true)
                                                    .firstOrNull()?.isTopNode ?: true
                                            }
                                            .map { cls -> cls.toTaxonomyTreeEntity(mergedOntology, reasoner) }
                                    ),
                                    type
                                )
                            )
                        }

                        "Property" -> {
                            val dp = mergedOntology.dataPropertiesInSignature
                                .filter {
                                    !it.isTopEntity && reasoner.getSuperDataProperties(it, true)
                                        .firstOrNull()?.isTopNode ?: true
                                }
                                .map { dp -> dp.toTaxonomyTreeEntity(mergedOntology, reasoner) }
                            val op = mergedOntology.objectPropertiesInSignature
                                .filter {
                                    !it.isTopEntity && reasoner.getSuperObjectProperties(it, true)
                                        .firstOrNull()?.isTopNode ?: true
                                }
                                .map { op -> op.toTaxonomyTreeEntity(mergedOntology, reasoner) }
                            call.respond(
                                GetTreeResponse(GetTreeResponse.TaxonomyTree(dp + op), type)
                            )
                        }

                        "Individual" -> {
                            call.respond(
                                GetTreeResponse(
                                    GetTreeResponse.TaxonomyTree(
                                        mergedOntology.individualsInSignature
                                            .map { ind -> ind.toTaxonomyTreeEntity(mergedOntology) }
                                    ),
                                    type
                                )
                            )
                        }
                    }
                }
                get("/select") {
                    val iri = call.request.queryParameters["iri"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                    val type = call.request.queryParameters["type"] ?: run {
                        val entity = mergedOntology.getEntitiesInSignature(IRI.create(iri)).firstOrNull()
                            ?: return@get call.respond(HttpStatusCode.NotFound)
                        when {
                            entity.isOWLClass -> "Class"
                            entity.isOWLDataProperty || entity.isOWLObjectProperty -> "Property"
                            entity.isOWLNamedIndividual -> "Individual"
                            else -> return@get call.respond(HttpStatusCode.BadRequest)
                        }
                    }
                    when (type) {
                        "Class" -> {
                            val cls = mergedOntology.getEntitiesInSignature(IRI.create(iri))
                                .firstOrNull { it.isOWLClass }?.asOWLClass()
                                ?: return@get call.respond(HttpStatusCode.NotFound)
                            val superClasses = reasoner.getSuperClasses(cls, false)
                            val leaf = cls.toTaxonomyTreeEntity(mergedOntology, reasoner)
                            val root = superClasses.fold(leaf) { acc, node ->
                                val superClass = node.representativeElement
                                if (superClass.isTopEntity) return@fold acc
                                superClass.toTaxonomyTreeEntity(mergedOntology, reasoner) {
                                    listOf(acc)
                                }
                            }
                            call.respond(GetTreeResponse(GetTreeResponse.TaxonomyTree(listOf(root)), type))
                        }

                        "Property" -> {
                            val entity = mergedOntology.getEntitiesInSignature(IRI.create(iri))
                                .firstOrNull { it.isOWLDataProperty || it.isOWLObjectProperty }
                                ?: return@get call.respond(HttpStatusCode.NotFound)
                            when {
                                entity.isOWLDataProperty -> {
                                    val property = entity.asOWLDataProperty()
                                    val superProperties = reasoner.getSuperDataProperties(property, false)
                                    val leaf = property.toTaxonomyTreeEntity(mergedOntology, reasoner)
                                    val root = superProperties.fold(leaf) { acc, node ->
                                        val superProperty =
                                            node.entities.firstOrNull { !it.isBottomEntity && !it.isTopEntity && !it.isAnonymous }
                                                ?.asOWLDataProperty() ?: return@fold acc
                                        // TODO: handle inverseOf and equivalents
                                        superProperty.toTaxonomyTreeEntity(mergedOntology, reasoner) {
                                            listOf(acc)
                                        }
                                    }
                                    call.respond(GetTreeResponse(GetTreeResponse.TaxonomyTree(listOf(root)), type))
                                }

                                entity.isOWLObjectProperty -> {
                                    val property = entity.asOWLObjectProperty()
                                    val superProperties = reasoner.getSuperObjectProperties(property, false)
                                    val leaf = property.toTaxonomyTreeEntity(mergedOntology, reasoner)
                                    val root = superProperties.fold(leaf) { acc, node ->
                                        val superProperty =
                                            node.entities.firstOrNull { !it.isBottomEntity && !it.isTopEntity && !it.isAnonymous }
                                                ?.asOWLObjectProperty() ?: return@fold acc
                                        // TODO: handle inverseOf and equivalents
                                        superProperty.toTaxonomyTreeEntity(mergedOntology, reasoner) {
                                            listOf(acc)
                                        }
                                    }
                                    call.respond(GetTreeResponse(GetTreeResponse.TaxonomyTree(listOf(root)), type))
                                }

                                else -> return@get call.respond(HttpStatusCode.BadRequest)
                            }
                        }

                        "Individual" -> {
                            val individual = mergedOntology.getEntitiesInSignature(IRI.create(iri))
                                .firstOrNull { it.isOWLNamedIndividual }
                                ?.asOWLNamedIndividual()
                                ?: return@get call.respond(HttpStatusCode.NotFound)
                            val leaf = individual.toTaxonomyTreeEntity(mergedOntology)
                            call.respond(GetTreeResponse(GetTreeResponse.TaxonomyTree(listOf(leaf)), type))
                        }
                    }
                }
                get("/expand") {
                    val iri = call.request.queryParameters["iri"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                    val type =
                        call.request.queryParameters["type"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                    when (type) {
                        "Class" -> {
                            val cls =
                                mergedOntology.getEntitiesInSignature(IRI.create(iri))
                                    .firstOrNull { it.isOWLClass }
                                    ?.asOWLClass()
                                    ?: return@get call.respond(HttpStatusCode.BadRequest)
                            val children = reasoner.getSubClasses(cls, true)
                                .filterNot { it.isBottomNode }
                                .map { it.representativeElement.toTaxonomyTreeEntity(mergedOntology, reasoner) }
                            call.respond(GetTreeResponse(GetTreeResponse.TaxonomyTree(children), type))
                        }

                        "Property" -> {
                            val property =
                                mergedOntology.getEntitiesInSignature(IRI.create(iri))
                                    .firstOrNull { it.isOWLDataProperty || it.isOWLObjectProperty }
                                    ?: return@get call.respond(HttpStatusCode.BadRequest)
                            val children = when {
                                property.isOWLDataProperty -> {
                                    reasoner.getSubDataProperties(property.asOWLDataProperty(), true)
                                        .filterNot { it.isBottomNode }
                                        .mapNotNull { node ->
                                            node.entities.firstOrNull {
                                                !it.isBottomEntity && !it.isTopEntity && !it.isAnonymous
                                            }
                                                ?.asOWLDataProperty() // TODO: handle inverseOf and equivalents
                                                ?.toTaxonomyTreeEntity(mergedOntology, reasoner)
                                        }
                                }

                                property.isOWLObjectProperty -> {
                                    reasoner.getSubObjectProperties(property.asOWLObjectProperty(), true)
                                        .filterNot { it.isBottomNode }
                                        .mapNotNull { node ->
                                            node.entities.firstOrNull {
                                                !it.isBottomEntity && !it.isTopEntity && !it.isAnonymous
                                            }
                                                ?.asOWLObjectProperty() // TODO: handle inverseOf and equivalents
                                                ?.toTaxonomyTreeEntity(mergedOntology, reasoner)
                                        }
                                }

                                else -> return@get call.respond(HttpStatusCode.BadRequest)
                            }
                            call.respond(GetTreeResponse(GetTreeResponse.TaxonomyTree(children), type))
                        }

                        "Individual" -> {
                            call.respond(GetTreeResponse(GetTreeResponse.TaxonomyTree(emptyList()), type))
                        }
                    }
                }
            }
            get("entityInfo") {
                val iri = call.request.queryParameters["iri"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                val type = call.request.queryParameters["type"] ?: run {
                    val entity = mergedOntology.getEntitiesInSignature(IRI.create(iri)).firstOrNull()
                        ?: return@get call.respond(HttpStatusCode.NotFound)
                    when {
                        entity.isOWLClass -> "Class"
                        entity.isOWLDataProperty || entity.isOWLObjectProperty -> "Property"
                        entity.isOWLNamedIndividual -> "Individual"
                        else -> return@get call.respond(HttpStatusCode.BadRequest)
                    }
                }
                val entity = mergedOntology.getEntitiesInSignature(IRI.create(iri)).firstOrNull() // TODO: Ensure type
                    ?: return@get call.respond(HttpStatusCode.NotFound)
                val label = entity.getLabel(mergedOntology)
                val annotationAxioms = mergedOntology.getAnnotationAssertionAxioms(entity.iri)
                val annotations = annotationAxioms.map {
                    GetEntityInfoResponse.Annotation(
                        it.property.getLabel(mergedOntology),
                        it.property.iri.toString(),
                        it.value.asLiteral().orNull()?.literal.toString(),
                    )
                }
                when (type) {
                    "Class" -> {
                        val equivalentClasses = ontology.getEquivalentClassesAxioms(entity.asOWLClass())
                        call.respond(GetEntityInfoResponse(
                            entity.iri.toString(),
                            "Class",
                            label,
                            annotations,
                            classInfo = GetEntityInfoResponse.ClassInfo(
                                /*equivalentClasses.flatMap { equivalentClassesAxiom ->
                                    equivalentClassesAxiom.namedClasses.filter { it.iri != entity.iri }.map {
                                        GetEntityInfoResponse.Entity(
                                            it.iri.toString(),
                                            it.getLabel(mergedOntology),
                                            "Class"
                                        )
                                    }
                                },*/
                                reasoner.getSuperClasses(entity.asOWLClass(), true).map {
                                    val parentClass = it.representativeElement
                                    GetEntityInfoResponse.Entity(
                                        parentClass.iri.toString(),
                                        parentClass.getLabel(mergedOntology),
                                        "Class"
                                    )
                                },
                                emptyList(), // TODO
                                emptyList(), // TODO
                                emptyList(), // TODO
                            )
                        ))
                    }

                    "Property" -> {
                        call.respond(GetEntityInfoResponse(
                            entity.iri.toString(),
                            "Property",
                            label,
                            annotations,
                            // propertyInfo = TODO
                        ))
                    }

                    "Individual" -> {
                        val individual = entity.asOWLNamedIndividual()
                        val individualTypes = reasoner.getTypes(individual, true)
                        call.respond(GetEntityInfoResponse(
                            entity.iri.toString(),
                            "Individual",
                            label,
                            annotations,
                            individualInfo = GetEntityInfoResponse.IndividualInfo(
                                individualTypes.map {
                                    val individualType = it.representativeElement
                                    GetEntityInfoResponse.Entity(
                                        individualType.iri.toString(),
                                        individualType.getLabel(mergedOntology),
                                        "Class"
                                    )
                                }
                            )
                        ))
                    }
                }
            }
            get("/ontologyInfo") {
                call.respond(
                    GetOntologyInfoResponse(
                        listOf(
                            GetOntologyInfoResponse.Annotation(
                                "Ontology IRI",
                                ontology.ontologyID.ontologyIRI.orNull().toString()
                            ),
                            GetOntologyInfoResponse.Annotation(
                                "Version IRI",
                                ontology.ontologyID.versionIRI.orNull().toString()
                            ),
                        ),
                        GetOntologyInfoResponse.Counts(
                            mergedOntology.classesInSignature.size,
                            mergedOntology.dataPropertiesInSignature.size + mergedOntology.objectPropertiesInSignature.size,
                            mergedOntology.individualsInSignature.size
                        )
                    )
                )
            }
            route("submission") {
                post("submitForm") {
                    val formParams = call.receiveParameters()
                    val title = formParams["title"].orEmpty()
                    val abstract = formParams["abstract"].orEmpty()
                    val keywords = formParams["keywords"].orEmpty()

                    class EntityReference(
                        val entity: PostSubmitFormResponse.Entity,
                        val textStartIndex: Int,
                        val textEndIndex: Int,
                        val hammingDistance: Int,
                    )

                    val titleEntityReferences = mutableListOf<EntityReference>()
                    val abstractEntityReferences = mutableListOf<EntityReference>()
                    val keywordEntityReferences = mutableListOf<EntityReference>()
                    mergedOntology.signature.forEach { entity ->
                        val label = entity.getLabel(mergedOntology)
                        val nWordsInEntity = label.split("\\s+".toRegex()).size
                        fun findEntityReferencesInText(text: String, entityReferences: MutableList<EntityReference>) {
                            val wordsInTextIndexed = text.splitIndexed("\\s+".toRegex()).map { (word, textStartIndex) ->
                                word.replace("""^[,.?]|[,.?]$""".toRegex(), "") to textStartIndex
                            }
                            wordsInTextIndexed
                                .windowed(nWordsInEntity)
                                .forEach { wordsWindowIndexed ->
                                    val (wordsWindow, startIndexesWindow) = wordsWindowIndexed.unzip()
                                    val startIndex = startIndexesWindow.first()
                                    val endIndex = startIndexesWindow.last() + wordsWindow.last().length - 1
                                    val hammingDistance = label
                                        .zip(wordsWindow.joinToString(" "))
                                        .count { it.first.lowercaseChar() != it.second.lowercaseChar() } +
                                            abs(wordsWindow.joinToString(" ").length - label.length)
                                    if (hammingDistance <= wordsWindow.joinToString(" ").length / 5) {
                                        val entityReference = EntityReference(
                                            PostSubmitFormResponse.Entity(
                                                entity.getLabel(mergedOntology),
                                                entity.iri.toString(),
                                                entity.simpleType!!
                                            ),
                                            startIndex,
                                            endIndex,
                                            hammingDistance
                                        )
                                        val overlappingReferencedEntityIndex = entityReferences.indexOfFirst {
                                            max(startIndex, it.textStartIndex) <= min(endIndex, it.textEndIndex)
                                        }
                                        val overlappingReferencedEntity =
                                            entityReferences.getOrNull(overlappingReferencedEntityIndex)
                                        if (overlappingReferencedEntity == null) {
                                            entityReferences.add(entityReference)
                                        } else if (
                                            label.length > overlappingReferencedEntity.entity.label.length ||
                                            hammingDistance < overlappingReferencedEntity.hammingDistance
                                        ) {
                                            entityReferences[overlappingReferencedEntityIndex] = entityReference
                                        }
                                    }
                                }
                        }
                        findEntityReferencesInText(title, titleEntityReferences)
                        findEntityReferencesInText(abstract, abstractEntityReferences)
                        findEntityReferencesInText(keywords, keywordEntityReferences)
                    }
                    val referencedEntities =
                        (titleEntityReferences + abstractEntityReferences + keywordEntityReferences)
                            .distinctBy { it.entity.iri }
                            .map { it.entity }
                    titleEntityReferences.sortBy { it.textStartIndex }
                    abstractEntityReferences.sortBy { it.textStartIndex }
                    keywordEntityReferences.sortBy { it.textStartIndex }
                    fun getTextSegments(
                        text: String,
                        entityReferences: List<EntityReference>
                    ): List<PostSubmitFormResponse.TextSegment> {
                        val textSegments = mutableListOf<PostSubmitFormResponse.TextSegment>()
                        var nextIndex = 0
                        entityReferences.forEachIndexed { index, entityReference ->
                            if (entityReference.textStartIndex > nextIndex) {
                                textSegments.add(
                                    PostSubmitFormResponse.TextSegment(
                                        text.substring(
                                            nextIndex,
                                            entityReference.textStartIndex
                                        ), null
                                    )
                                )
                            }
                            textSegments.add(
                                PostSubmitFormResponse.TextSegment(
                                    text.substring(
                                        entityReference.textStartIndex,
                                        entityReference.textEndIndex + 1
                                    ), referencedEntities.indexOfFirst { it.iri == entityReference.entity.iri }
                                )
                            )
                            nextIndex = entityReference.textEndIndex + 1
                        }
                        if (text.isNotEmpty() && nextIndex < text.length) {
                            textSegments.add(
                                PostSubmitFormResponse.TextSegment(
                                    text.substring(
                                        nextIndex,
                                        text.length
                                    ), null
                                )
                            )
                        }
                        return textSegments
                    }

                    val titleSegments = getTextSegments(title, titleEntityReferences)
                    val abstractSegments = getTextSegments(abstract, abstractEntityReferences)
                    val keywordSegments = getTextSegments(keywords, keywordEntityReferences)
                    call.respond(
                        PostSubmitFormResponse(
                            referencedEntities,
                            titleSegments,
                            abstractSegments,
                            keywordSegments,
                        )
                    )
                }
            }
        }
    }
}