package io.github.macodaclub.routes.api

import io.github.macodaclub.models.GetOntologyInfoResponse
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.semanticweb.owlapi.model.OWLOntology

fun Routing.ontologyInfoRoutes(
    ontology: OWLOntology,
    mergedOntology: OWLOntology,
) {
    route("/api") {
        get("/ontologyInfo") {
            call.respond(
                GetOntologyInfoResponse(
                    listOf(
                        GetOntologyInfoResponse.Annotation(
                            "Ontology IRI",
                            ontology.ontologyID.ontologyIRI.orNull().toString()
                        ),
                        /*GetOntologyInfoResponse.Annotation(
                            "Version IRI",
                            ontology.ontologyID.versionIRI.orNull().toString()
                        ),*/
                    ),
                    GetOntologyInfoResponse.Counts(
                        mergedOntology.classesInSignature.size,
                        mergedOntology.dataPropertiesInSignature.size + mergedOntology.objectPropertiesInSignature.size,
                        mergedOntology.individualsInSignature.size
                    )
                )
            )
        }
    }
}