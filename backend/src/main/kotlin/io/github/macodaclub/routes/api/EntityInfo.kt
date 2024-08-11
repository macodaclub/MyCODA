package io.github.macodaclub.routes.api

import io.github.macodaclub.models.GetEntityInfoResponse
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
            val annotations = annotationAxioms.map {
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
                        )
                    )
                }

                "Property" -> {
                    call.respond(
                        GetEntityInfoResponse(
                            entity.iri.toString(),
                            "Property",
                            label,
                            annotations,
                            // propertyInfo = TODO
                        )
                    )
                }

                "Individual" -> {
                    val individual = entity.asOWLNamedIndividual()
                    val individualTypes = reasoner.getTypes(individual, true)
                    val properties = EntitySearcher.getDataPropertyValues(individual, mergedOntology).asMap()
                        .mapNotNull { (propertyExp, literals) ->
                            val property = propertyExp.asOWLDataProperty()
                            GetEntityInfoResponse.IndividualInfo.Property(
                                property.iri.toString(),
                                property.getLabel(mergedOntology),
                                literals.map { literal ->
                                    GetEntityInfoResponse.Entity(
                                        literal.datatype.iri.toString(),
                                        literal.literal,
                                        "Datatype"
                                    )
                                },
                                null
                            )
                        } + EntitySearcher.getObjectPropertyValues(individual, mergedOntology).asMap()
                        .mapNotNull { (propertyExp, literals) ->
                            val property = propertyExp.asOWLObjectProperty()
                            GetEntityInfoResponse.IndividualInfo.Property(
                                property.iri.toString(),
                                property.getLabel(mergedOntology),
                                literals.map { literal ->
                                    literal.asOWLNamedIndividual().let { individual ->
                                        GetEntityInfoResponse.Entity(
                                            individual.iri.toString(),
                                            individual.getLabel(mergedOntology),
                                            individual.simpleType ?: return@mapNotNull null
                                        )
                                    }
                                },
                                null
                            )
                        }
                    call.respond(
                        GetEntityInfoResponse(
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