package io.github.macodaclub.routes.api

import io.github.macodaclub.models.GetEntityInfoResponse
import io.github.macodaclub.models.editor.GetEditorIndividualPropertiesResponse
import io.github.macodaclub.utils.getLabel
import io.github.macodaclub.utils.simpleType
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.semanticweb.owlapi.model.IRI
import org.semanticweb.owlapi.model.OWLOntology
import org.semanticweb.owlapi.reasoner.OWLReasoner
import org.semanticweb.owlapi.search.EntitySearcher

fun Routing.entityInfoRoutes(
    mergedOntology: OWLOntology,
    reasoner: OWLReasoner,
) {
    route("/api") {
        get("entityInfo") {
            val iri = call.request.queryParameters["iri"] ?: return@get call.respond(HttpStatusCode.BadRequest)
            val type = call.request.queryParameters["type"] ?: run {
                val entity = mergedOntology.getEntitiesInSignature(IRI.create(iri)).firstOrNull()
                    ?: return@get call.respond(HttpStatusCode.BadRequest)
                when {
                    entity.isOWLClass -> "Class"
                    entity.isOWLDataProperty || entity.isOWLObjectProperty -> "Property"
                    entity.isOWLNamedIndividual -> "Individual"
                    else -> return@get call.respond(HttpStatusCode.BadRequest)
                }
            }
            val entity = mergedOntology.getEntitiesInSignature(IRI.create(iri)).firstOrNull() // TODO: Ensure type
                ?: return@get call.respond(HttpStatusCode.BadRequest)
            val label = entity.getLabel(mergedOntology)
            val annotationAxioms = mergedOntology.getAnnotationAssertionAxioms(entity.iri)
            val comment =
                annotationAxioms.firstOrNull { it.property.isComment }?.value?.asLiteral()?.orNull()?.literal
            val annotations = annotationAxioms
                .filterNot { it.property.isComment }
                .filterNot { it.property.isLabel }
                .map {
                    GetEntityInfoResponse.Annotation(
                        it.property.getLabel(mergedOntology),
                        it.property.iri.toString(),
                        it.value.asLiteral().orNull()?.literal.toString(),
                    )
                }
            when (type) {
                "Class" -> {
                    //val equivalentClasses = mergedOntology.getEquivalentClassesAxioms(entity.asOWLClass())
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
                            val domain = EntitySearcher.getDomains(property, mergedOntology).firstOrNull()?.asOWLClass()
                            val range = EntitySearcher.getRanges(property, mergedOntology).firstOrNull()?.asOWLDatatype()
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
                                                domain.getLabel(mergedOntology),
                                                domain.simpleType ?: return@get call.respond(HttpStatusCode.BadRequest)
                                            )
                                        },
                                        GetEntityInfoResponse.Entity(
                                            range.iri.toString(),
                                            range.getLabel(mergedOntology),
                                            range.simpleType ?: return@get call.respond(HttpStatusCode.BadRequest)
                                        )
                                    )
                                )
                            )
                        }

                        entity.isOWLObjectProperty -> {
                            val property = entity.asOWLObjectProperty()
                            val domain = EntitySearcher.getDomains(property, mergedOntology).firstOrNull()?.asOWLClass()
                            val range = EntitySearcher.getRanges(property, mergedOntology).firstOrNull()?.asOWLClass()
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
                                                domain.getLabel(mergedOntology),
                                                domain.simpleType ?: return@get call.respond(HttpStatusCode.BadRequest)
                                            )
                                        },
                                        GetEntityInfoResponse.Entity(
                                            range.iri.toString(),
                                            range.getLabel(mergedOntology),
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
                    val individualTypes = reasoner.getTypes(individual, true)
                    val properties = EntitySearcher.getDataPropertyValues(individual, mergedOntology).asMap()
                        .mapNotNull { (propertyExp, literals) ->
                            val property = propertyExp.asOWLDataProperty()
                            val range =
                                EntitySearcher.getRanges(property, mergedOntology).map { it.asOWLDatatype() }
                                    .firstOrNull()
                                    ?: return@mapNotNull null
                            // TODO: Support complex ranges

                            GetEntityInfoResponse.IndividualInfo.Property(
                                GetEntityInfoResponse.Entity(
                                    property.iri.toString(),
                                    property.getLabel(mergedOntology),
                                    "Property"
                                ),
                                GetEntityInfoResponse.Entity(
                                    range.iri.toString(),
                                    range.getLabel(mergedOntology),
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
                        } + EntitySearcher.getObjectPropertyValues(individual, mergedOntology).asMap()
                        .mapNotNull { (propertyExp, literals) ->
                            val property = propertyExp.asOWLObjectProperty()
                            val range =
                                EntitySearcher.getRanges(property, mergedOntology).map { it.asOWLClass() }
                                    .firstOrNull()
                                    ?: return@mapNotNull null
                            // TODO: Support complex ranges

                            GetEntityInfoResponse.IndividualInfo.Property(
                                GetEntityInfoResponse.Entity(
                                    property.iri.toString(),
                                    property.getLabel(mergedOntology),
                                    "Property"
                                ),
                                GetEntityInfoResponse.Entity(
                                    range.iri.toString(),
                                    range.getLabel(mergedOntology),
                                    range.simpleType ?: return@mapNotNull null
                                ),
                                literals.map { literal ->
                                    literal.asOWLNamedIndividual().let { individual ->
                                        GetEntityInfoResponse.Entity(
                                            individual.iri.toString(),
                                            individual.getLabel(mergedOntology),
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
                                        individualType.getLabel(mergedOntology),
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