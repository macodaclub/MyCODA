package io.github.macodaclub.routes.api

import io.github.macodaclub.models.GetEntityInfoResponse
import io.github.macodaclub.models.GetOntologyInfoResponse
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

fun Route.ontologyInfoRoutes(
    ontology: OWLOntology,
    mergedOntology: OWLOntology,
) {
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
}