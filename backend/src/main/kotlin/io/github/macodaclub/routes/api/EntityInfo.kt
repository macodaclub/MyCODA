package io.github.macodaclub.routes.api

import io.github.macodaclub.models.api.GetEntityInfoResponse
import io.github.macodaclub.plugins.OntologyManager
import io.github.macodaclub.utils.getLabel
import io.github.macodaclub.utils.simpleType
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.semanticweb.owlapi.model.IRI
import org.semanticweb.owlapi.search.EntitySearcher

fun Routing.entityInfoRoutes(
    ontologyManager: OntologyManager
) {
    route("/api") {
        get("/entityInfo") {
            val iri = call.request.queryParameters["iri"] ?: return@get call.respond(HttpStatusCode.BadRequest)
            val type = call.request.queryParameters["type"] ?: run {
                val entity = ontologyManager.mergedOntology.getEntitiesInSignature(IRI.create(iri)).firstOrNull()
                    ?: return@get call.respond(HttpStatusCode.BadRequest)
                when {
                    entity.isOWLClass -> "Class"
                    entity.isOWLDataProperty || entity.isOWLObjectProperty -> "Property"
                    entity.isOWLNamedIndividual -> "Individual"
                    else -> return@get call.respond(HttpStatusCode.BadRequest)
                }
            }
            val entity = ontologyManager.mergedOntology.getEntitiesInSignature(IRI.create(iri))
                .firstOrNull() // TODO: Ensure type
                ?: return@get call.respond(HttpStatusCode.BadRequest)
            val label = entity.getLabel(ontologyManager.mergedOntology)
            val annotationAxioms = ontologyManager.mergedOntology.getAnnotationAssertionAxioms(entity.iri)
            val comment =
                annotationAxioms.firstOrNull { it.property.isComment }?.value?.asLiteral()?.orNull()?.literal
            val annotations = EntitySearcher.getAnnotations(entity, ontologyManager.mergedOntology)
                .filterNot { it.property.isComment }
                .filterNot { it.property.isLabel }
                .groupBy { it.property }
                .map { (property, annotations) ->
                    GetEntityInfoResponse.Annotation(
                        GetEntityInfoResponse.Entity(
                            property.iri.toString(),
                            property.getLabel(ontologyManager.mergedOntology),
                            "Annotation"
                        ),
                        annotations.mapNotNull {
                            when {
                                it.value.isLiteral -> {
                                    val literal = it.value.asLiteral().get()
                                    GetEntityInfoResponse.Entity(
                                        literal.datatype.iri.toString(),
                                        it.literalValue().get().literal,
                                        "Datatype",
                                    )
                                }

                                else -> {
                                    // TODO: Support non-literal annotations
                                    return@mapNotNull null
                                }
                            }
                        }
                    )
                }
            when (type) {
                "Class" -> {
                    //val equivalentClasses = ontologyManager.mergedOntology.getEquivalentClassesAxioms(entity.asOWLClass())
                    call.respond(
                        GetEntityInfoResponse(
                            GetEntityInfoResponse.Entity(
                                entity.iri.toString(),
                                label,
                                "Class",
                            ),
                            comment,
                            annotations,
                            classInfo = GetEntityInfoResponse.ClassInfo(
                                /*equivalentClasses.flatMap { equivalentClassesAxiom ->
                                    equivalentClassesAxiom.namedClasses.filter { it.iri != entity.iri }.map {
                                        GetEntityInfoResponse.Entity(
                                            it.iri.toString(),
                                            it.getLabel(ontologyManager.mergedOntology),
                                            "Class"
                                        )
                                    }
                                },*/
                                ontologyManager.reasoner.getSuperClasses(entity.asOWLClass(), true).map {
                                    val parentClass = it.representativeElement
                                    GetEntityInfoResponse.Entity(
                                        parentClass.iri.toString(),
                                        parentClass.getLabel(ontologyManager.mergedOntology),
                                        "Class",
                                    )
                                },
                                emptyList(), // TODO
                                emptyList(), // TODO
                                emptyList(), // TODO
                            )
                        )
                    )
                }

                "Property" -> {
                    when {
                        entity.isOWLDataProperty -> {
                            val property = entity.asOWLDataProperty()
                            val domain =
                                EntitySearcher.getDomains(property, ontologyManager.mergedOntology).firstOrNull()
                                    ?.asOWLClass()
                            val range = EntitySearcher.getRanges(property, ontologyManager.mergedOntology).firstOrNull()
                                ?.asOWLDatatype()
                                ?: return@get call.respond(HttpStatusCode.BadRequest)
                            // TODO: Support complex domains/ranges

                            call.respond(
                                GetEntityInfoResponse(
                                    GetEntityInfoResponse.Entity(
                                        entity.iri.toString(),
                                        label,
                                        "Property",
                                    ),
                                    comment,
                                    annotations,
                                    propertyInfo = GetEntityInfoResponse.PropertyInfo(
                                        domain?.let {
                                            GetEntityInfoResponse.Entity(
                                                domain.iri.toString(),
                                                domain.getLabel(ontologyManager.mergedOntology),
                                                domain.simpleType ?: return@get call.respond(HttpStatusCode.BadRequest)
                                            )
                                        },
                                        GetEntityInfoResponse.Entity(
                                            range.iri.toString(),
                                            range.getLabel(ontologyManager.mergedOntology),
                                            range.simpleType ?: return@get call.respond(HttpStatusCode.BadRequest)
                                        )
                                    )
                                )
                            )
                        }

                        entity.isOWLObjectProperty -> {
                            val property = entity.asOWLObjectProperty()
                            val domain =
                                EntitySearcher.getDomains(property, ontologyManager.mergedOntology).firstOrNull()
                                    ?.asOWLClass()
                            val range = EntitySearcher.getRanges(property, ontologyManager.mergedOntology).firstOrNull()
                                ?.asOWLClass()
                                ?: return@get call.respond(HttpStatusCode.BadRequest)
                            // TODO: Support complex domains/ranges

                            call.respond(
                                GetEntityInfoResponse(
                                    GetEntityInfoResponse.Entity(
                                        entity.iri.toString(),
                                        label,
                                        "Property",
                                    ),
                                    comment,
                                    annotations,
                                    propertyInfo = GetEntityInfoResponse.PropertyInfo(
                                        domain?.let {
                                            GetEntityInfoResponse.Entity(
                                                domain.iri.toString(),
                                                domain.getLabel(ontologyManager.mergedOntology),
                                                domain.simpleType ?: return@get call.respond(HttpStatusCode.BadRequest)
                                            )
                                        },
                                        GetEntityInfoResponse.Entity(
                                            range.iri.toString(),
                                            range.getLabel(ontologyManager.mergedOntology),
                                            range.simpleType ?: return@get call.respond(HttpStatusCode.BadRequest)
                                        )
                                    )
                                )
                            )
                        }

                        else -> return@get call.respond(HttpStatusCode.BadRequest)
                    }
                }

                "Individual" -> {
                    val individual = entity.asOWLNamedIndividual()
                    val individualTypes = ontologyManager.reasoner.getTypes(individual, true)
                    val properties =
                        EntitySearcher.getDataPropertyValues(individual, ontologyManager.mergedOntology).asMap()
                            .mapNotNull { (propertyExp, literals) ->
                                val property = propertyExp.asOWLDataProperty()
                                val range =
                                    EntitySearcher.getRanges(property, ontologyManager.mergedOntology)
                                        .map { it.asOWLDatatype() }
                                        .firstOrNull()
                                        ?: return@mapNotNull null
                                // TODO: Support complex ranges

                                GetEntityInfoResponse.IndividualInfo.Property(
                                    GetEntityInfoResponse.Entity(
                                        property.iri.toString(),
                                        property.getLabel(ontologyManager.mergedOntology),
                                        "Property"
                                    ),
                                    GetEntityInfoResponse.Entity(
                                        range.iri.toString(),
                                        range.getLabel(ontologyManager.mergedOntology),
                                        range.simpleType ?: return@mapNotNull null
                                    ),
                                    literals.map { literal ->
                                        GetEntityInfoResponse.Entity(
                                            literal.datatype.iri.toString(),
                                            literal.literal,
                                            "Datatype"
                                        )
                                    },
                                )
                            } + EntitySearcher.getObjectPropertyValues(individual, ontologyManager.mergedOntology)
                            .asMap()
                            .mapNotNull { (propertyExp, literals) ->
                                val property = propertyExp.asOWLObjectProperty()
                                val range =
                                    EntitySearcher.getRanges(property, ontologyManager.mergedOntology)
                                        .map { it.asOWLClass() }
                                        .firstOrNull()
                                        ?: return@mapNotNull null
                                // TODO: Support complex ranges

                                GetEntityInfoResponse.IndividualInfo.Property(
                                    GetEntityInfoResponse.Entity(
                                        property.iri.toString(),
                                        property.getLabel(ontologyManager.mergedOntology),
                                        "Property"
                                    ),
                                    GetEntityInfoResponse.Entity(
                                        range.iri.toString(),
                                        range.getLabel(ontologyManager.mergedOntology),
                                        range.simpleType ?: return@mapNotNull null
                                    ),
                                    literals.map { literal ->
                                        literal.asOWLNamedIndividual().let { individual ->
                                            GetEntityInfoResponse.Entity(
                                                individual.iri.toString(),
                                                individual.getLabel(ontologyManager.mergedOntology),
                                                individual.simpleType ?: return@mapNotNull null
                                            )
                                        }
                                    },
                                )
                            }
                    call.respond(
                        GetEntityInfoResponse(
                            GetEntityInfoResponse.Entity(
                                entity.iri.toString(),
                                label,
                                "Individual",
                            ),
                            comment,
                            annotations,
                            individualInfo = GetEntityInfoResponse.IndividualInfo(
                                individualTypes.map {
                                    val individualType = it.representativeElement
                                    GetEntityInfoResponse.Entity(
                                        individualType.iri.toString(),
                                        individualType.getLabel(ontologyManager.mergedOntology),
                                        "Class"
                                    )
                                },
                                properties
                            )
                        )
                    )
                }
            }
        }
    }
}