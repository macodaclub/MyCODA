package io.github.macodaclub.routes.api

import io.github.macodaclub.models.api.ontology.OntologyGraphErrorResponse
import io.github.macodaclub.plugins.OntologyManager
import io.github.macodaclub.services.OntologyGraphService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.ktor.server.routing.route

/**
 * GET /api/ontology/graph?iri=<IRI>&type=Class|Property|Individual&depth=1
 */
fun Routing.ontologyGraphRoutes(
    ontologyManager: OntologyManager
) {
    route("/api") {
        route("/ontology") {
            get("/graph") {
                val iri = call.request.queryParameters["iri"]

                if (iri.isNullOrBlank()) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        OntologyGraphErrorResponse(
                            code = "MISSING_ONTOLOGY_GRAPH_IRI",
                            message = "The iri query parameter is required."
                        )
                    )
                    return@get
                }

                val type = call.request.queryParameters["type"]

                val depthValue =
                    call.request.queryParameters["depth"]
                        ?.toIntOrNull()
                        ?: 1

                if (depthValue !in 1..3) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        OntologyGraphErrorResponse(
                            code = "INVALID_ONTOLOGY_GRAPH_DEPTH",
                            message = "Depth must be between 1 and 3."
                        )
                    )
                    return@get
                }

                try {
                    val response =
                        OntologyGraphService.buildGraph(
                            ontologyManager = ontologyManager,
                            iri = iri,
                            requestedType = type,
                            depth = depthValue
                        )

                    call.respond(
                        HttpStatusCode.OK,
                        response
                    )
                } catch (exception: IllegalArgumentException) {
                    call.respond(
                        HttpStatusCode.NotFound,
                        OntologyGraphErrorResponse(
                            code = "ONTOLOGY_GRAPH_ENTITY_NOT_FOUND",
                            message = exception.message
                                ?: "The ontology entity was not found."
                        )
                    )
                } catch (exception: Exception) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        OntologyGraphErrorResponse(
                            code = "ONTOLOGY_GRAPH_ERROR",
                            message = exception.message
                                ?: "The ontology graph could not be generated."
                        )
                    )
                }
            }
        }
    }
}
