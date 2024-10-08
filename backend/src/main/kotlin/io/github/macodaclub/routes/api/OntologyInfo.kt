package io.github.macodaclub.routes.api

import io.github.macodaclub.models.api.GetOntologyInfoResponse
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
    }
}