package io.github.macodaclub.routes.api

import io.github.macodaclub.models.api.datasets.OplDatasetSearchRequest
import io.github.macodaclub.plugins.OntologyManager
import io.github.macodaclub.services.OplDatasetSearchService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import kotlinx.serialization.SerializationException

/**
 * Endpoints para pesquisa de problemas
 * de otimização existentes na OPL.
 */
fun Routing.oplDatasetRoutes(
    ontologyManager: OntologyManager
) {
    route("/api/datasets/opl") {

        post("/search") {
            try {
                val request =
                    call.receive<OplDatasetSearchRequest>()

                val response =
                    OplDatasetSearchService.search(
                        ontologyManager = ontologyManager,
                        request = request
                    )

                call.respond(
                    HttpStatusCode.OK,
                    response
                )
            } catch (exception: IllegalArgumentException) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    DatasetErrorResponse(
                        code = "INVALID_OPL_QUERY",
                        message = exception.message
                            ?: "The OPL query is invalid."
                    )
                )
            } catch (exception: BadRequestException) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    DatasetErrorResponse(
                        code = "INVALID_REQUEST_BODY",
                        message =
                            "The OPL search request body is invalid."
                    )
                )
            } catch (exception: SerializationException) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    DatasetErrorResponse(
                        code = "INVALID_REQUEST_BODY",
                        message =
                            "The OPL search request contains invalid fields or values."
                    )
                )
            } catch (exception: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    DatasetErrorResponse(
                        code = "INTERNAL_OPL_ERROR",
                        message = exception.message
                            ?: "An unexpected error occurred while searching OPL."
                    )
                )
            }
        }
    }
}