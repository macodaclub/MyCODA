package io.github.macodaclub.routes.api

import io.github.macodaclub.models.Pagination
import io.github.macodaclub.models.search.GetSearchResponse
import io.github.macodaclub.plugins.EntityFinder
import io.github.macodaclub.utils.getLabel
import io.github.macodaclub.utils.simpleType
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.semanticweb.owlapi.model.IRI
import org.semanticweb.owlapi.model.OWLOntology
import org.semanticweb.owlapi.reasoner.OWLReasoner

fun Route.searchRoutes(
    mergedOntology: OWLOntology,
    reasoner: OWLReasoner,
    entityFinder: EntityFinder,
) {
    route("search") {
        get {
            val query = call.request.queryParameters["query"]?.lowercase()
                ?: return@get call.respond(HttpStatusCode.BadRequest)
            val type = call.request.queryParameters["type"]
            val parentClassIri = call.request.queryParameters["subClassOf"]
            val offset = call.request.queryParameters["offset"]?.toIntOrNull() ?: 0
            val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 5

            val entities = entityFinder.searchEntities(query, type)
            val filteredEntities = if (parentClassIri == null) entities else {
                val parentClass =
                    mergedOntology.getEntitiesInSignature(IRI.create(parentClassIri)).firstOrNull { it.isOWLClass }
                        ?.asOWLClass() ?: return@get call.respond(HttpStatusCode.BadRequest)
                when (type) {
                    "Class" -> TODO()
                    "Individual" -> {
                        entities.filter {
                            reasoner.getTypes(it.asOWLNamedIndividual(), false).containsEntity(parentClass)
                        }
                    }

                    else -> entities
                }
            }
            val searchResponseEntities = filteredEntities.mapNotNull {
                GetSearchResponse.Entity(
                    it.getLabel(mergedOntology),
                    it.iri.toString(),
                    it.simpleType ?: return@mapNotNull null
                )
            }
            val paginatedEntities = searchResponseEntities.drop(offset).take(limit)

            call.respond(
                GetSearchResponse(
                    paginatedEntities,
                    Pagination(
                        entities.size,
                        offset,
                        limit,
                        (offset + limit).takeIf { it < entities.size },
                        (offset - limit).takeIf { it >= 0 }
                    )
                )
            )
        }
    }
}