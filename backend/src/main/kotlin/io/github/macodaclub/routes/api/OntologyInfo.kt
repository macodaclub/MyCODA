package io.github.macodaclub.routes.api
import io.github.macodaclub.utils.getLabel
import io.github.macodaclub.models.api.GetOntologyInfoResponse
import io.github.macodaclub.plugins.OntologyManager
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.semanticweb.owlapi.model.OWLOntology
import io.github.macodaclub.models.api.GetOntologyEntitiesResponse
import io.ktor.http.HttpStatusCode
import org.semanticweb.owlapi.model.OWLEntity
import org.semanticweb.owlapi.model.OWLLiteral


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
    try {
        val requestedType = call.request.queryParameters["type"]

        fun getEntityLabel(
            ontology: org.semanticweb.owlapi.model.OWLOntology,
            entity: org.semanticweb.owlapi.model.OWLEntity
        ): String {
            val label = ontology.getAnnotationAssertionAxioms(entity.iri)
                .firstOrNull { axiom ->
                    axiom.property.isLabel &&
                        axiom.value is org.semanticweb.owlapi.model.OWLLiteral
                }
                ?.value as? org.semanticweb.owlapi.model.OWLLiteral

            return label?.literal ?: entity.iri.shortForm
        }

        fun getEntityComment(
            ontology: org.semanticweb.owlapi.model.OWLOntology,
            entity: org.semanticweb.owlapi.model.OWLEntity
        ): String? {
            val comment = ontology.getAnnotationAssertionAxioms(entity.iri)
                .firstOrNull { axiom ->
                    axiom.property.isComment &&
                        axiom.value is org.semanticweb.owlapi.model.OWLLiteral
                }
                ?.value as? org.semanticweb.owlapi.model.OWLLiteral

            return comment?.literal
        }

        fun buildEntity(
            type: String,
            entity: org.semanticweb.owlapi.model.OWLEntity
        ): GetOntologyEntitiesResponse.Entity {
            val iri = entity.iri

            return GetOntologyEntitiesResponse.Entity(
                id = "${type}_${iri}",
                type = type,
                name = getEntityLabel(ontologyManager.mergedOntology, entity),
                queryName = iri.shortForm,
                iri = iri.toString(),
                details = iri.toString(),
                comment = getEntityComment(ontologyManager.mergedOntology, entity)
            )
        }

        val classes =
            if (requestedType == null || requestedType == "Class") {
                ontologyManager.mergedOntology.classesInSignature
                    .filterNot { it.isTopEntity }
                    .map { buildEntity("Class", it) }
            } else {
                emptyList()
            }

        val properties =
            if (requestedType == null || requestedType == "Property") {
                val objectProperties = ontologyManager.mergedOntology.objectPropertiesInSignature
                    .filterNot { it.isTopEntity }
                    .map { buildEntity("ObjectProperty", it) }

                val dataProperties = ontologyManager.mergedOntology.dataPropertiesInSignature
                    .filterNot { it.isTopEntity }
                    .map { buildEntity("DatatypeProperty", it) }

                objectProperties + dataProperties
            } else {
                emptyList()
            }

        val individuals =
            if (requestedType == null || requestedType == "Individual") {
                ontologyManager.mergedOntology.individualsInSignature
                    .map { buildEntity("Individual", it) }
            } else {
                emptyList()
            }

        val entities = (classes + properties + individuals)
            .sortedBy { it.name.lowercase() }

        call.respond(GetOntologyEntitiesResponse(entities))
    } catch (e: Exception) {
        e.printStackTrace()

        call.respondText(
            text = e.message ?: "Error loading ontology entities",
            status = HttpStatusCode.InternalServerError
        )
    }
}
    }
}