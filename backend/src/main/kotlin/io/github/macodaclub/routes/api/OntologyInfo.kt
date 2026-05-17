package io.github.macodaclub.routes.api

import io.github.macodaclub.models.api.GetOntologyInfoResponse
import io.github.macodaclub.models.api.GetOntologyEntitiesResponse
import io.github.macodaclub.plugins.OntologyManager
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.semanticweb.owlapi.model.OWLOntology

fun Routing.ontologyInfoRoutes(
    ontologyManager: OntologyManager
) {
    route("/api") {
        get("/ontologyInfo") {
            call.respond(
                GetOntologyInfoResponse(
                    listOf(
                        GetOntologyInfoResponse.Annotation(
                            "Ontology IRI",
                            ontologyManager.mycodaOntology.ontologyID.ontologyIRI.orNull().toString()
                        ),
                        /*GetOntologyInfoResponse.Annotation(
                            "Version IRI",
                            ontology.ontologyID.versionIRI.orNull().toString()
                        ),*/
                    ),
                    GetOntologyInfoResponse.Counts(
                        ontologyManager.mergedOntology.classesInSignature.size,
                        ontologyManager.mergedOntology.dataPropertiesInSignature.size + ontologyManager.mergedOntology.objectPropertiesInSignature.size,
                        ontologyManager.mergedOntology.individualsInSignature.size
                    )
                )
            )
        }
        get("/ontology/entities") {
            val classes = ontologyManager.mergedOntology.classesInSignature.map { owlClass ->
                val iri = owlClass.iri.toString()
                val name = owlClass.iri.shortForm

                GetOntologyEntitiesResponse.Entity(
                    id = "Class_$iri",
                    type = "Class",
                    name = name,
                    details = iri,
                    comment = null
                )
            }

            val objectProperties = ontologyManager.mergedOntology.objectPropertiesInSignature.map { property ->
                val iri = property.iri.toString()
                val name = property.iri.shortForm

                GetOntologyEntitiesResponse.Entity(
                    id = "ObjectProperty_$iri",
                    type = "ObjectProperty",
                    name = name,
                    details = iri,
                    comment = null
                )
            }

            val datatypeProperties = ontologyManager.mergedOntology.dataPropertiesInSignature.map { property ->
                val iri = property.iri.toString()
                val name = property.iri.shortForm

                GetOntologyEntitiesResponse.Entity(
                    id = "DatatypeProperty_$iri",
                    type = "DatatypeProperty",
                    name = name,
                    details = iri,
                    comment = null
                )
            }

            val individuals = ontologyManager.mergedOntology.individualsInSignature.map { individual ->
                val iri = individual.iri.toString()
                val name = individual.iri.shortForm

                GetOntologyEntitiesResponse.Entity(
                    id = "Individual_$iri",
                    type = "Individual",
                    name = name,
                    details = iri,
                    comment = null
                )
            }

            val entities = classes + objectProperties + datatypeProperties + individuals

            call.respond(
                GetOntologyEntitiesResponse(
                    entities = entities.sortedBy { it.name.lowercase() }
                )
            )
        }
    }
}