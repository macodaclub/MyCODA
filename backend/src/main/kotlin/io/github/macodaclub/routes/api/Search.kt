package io.github.macodaclub.routes.api

import io.github.macodaclub.models.api.Pagination
import io.github.macodaclub.models.api.search.GetSearchResponse
import io.github.macodaclub.plugins.EntityFinder
import io.github.macodaclub.plugins.OntologyManager
import io.github.macodaclub.utils.getLabel
import io.github.macodaclub.utils.simpleType
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json
import org.semanticweb.owlapi.model.IRI

fun Routing.searchRoutes(
    ontologyManager: OntologyManager,
) {
    route("/api") {
        route("/search") {
            get {
                val entityFinder = ontologyManager.entityFinder
                val query = call.request.queryParameters["query"]?.lowercase()
                    ?: return@get call.respond(HttpStatusCode.BadRequest)
                val types = call.request.queryParameters["types"]?.let {
                    Json.decodeFromString<List<String>>(it)
                }
                val parentClassIri = call.request.queryParameters["subClassOf"]
                val offset = call.request.queryParameters["offset"]?.toIntOrNull() ?: 0
                val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 5

                val entities = entityFinder.searchEntities(query, types)
                val filteredEntities = if (parentClassIri == null) entities else {
                    val parentClass =
                        ontologyManager.mergedOntology.getEntitiesInSignature(IRI.create(parentClassIri)).firstOrNull { it.isOWLClass }
                            ?.asOWLClass() ?: return@get call.respond(HttpStatusCode.BadRequest)
                    entities.filter { entity ->
                        when {
                            entity.isOWLNamedIndividual -> ontologyManager.reasoner.getTypes(entity.asOWLNamedIndividual(), false)
                                .containsEntity(parentClass)

                            else -> true
                        }
                    }
                }
                val searchResponseEntities = filteredEntities.mapNotNull {
                    GetSearchResponse.Entity(
                        it.iri.toString(),
                        it.getLabel(ontologyManager.mergedOntology),
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
}