package io.github.macodaclub.routes.api

import io.github.macodaclub.models.api.datasets.OplDatasetSearchRequest
import io.github.macodaclub.services.OplDatasetSearchService
import io.github.macodaclub.services.OplSourceTimeoutException
import io.github.macodaclub.services.OplSourceUnavailableException
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import kotlinx.serialization.SerializationException

/** POST /api/datasets/opl/search */
fun Routing.oplDatasetRoutes() {
    route("/api/datasets/opl") {
        post("/search") {
            try {
                val request = call.receive<OplDatasetSearchRequest>()
                call.respond(HttpStatusCode.OK, OplDatasetSearchService.search(request))
            } catch (exception: IllegalArgumentException) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    DatasetErrorResponse("INVALID_OPL_QUERY", exception.message ?: "The OPL query is invalid.")
                )
            } catch (exception: BadRequestException) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    DatasetErrorResponse("INVALID_REQUEST_BODY", "The OPL search request body is invalid.")
                )
            } catch (exception: SerializationException) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    DatasetErrorResponse("INVALID_REQUEST_BODY", "The OPL search request contains invalid fields or values.")
                )
            } catch (exception: OplSourceTimeoutException) {
                call.respond(
                    HttpStatusCode.GatewayTimeout,
                    DatasetErrorResponse("OPL_TIMEOUT", exception.message ?: "The OPL source timed out.")
                )
            } catch (exception: OplSourceUnavailableException) {
                call.respond(
                    HttpStatusCode.BadGateway,
                    DatasetErrorResponse("OPL_SOURCE_UNAVAILABLE", exception.message ?: "The OPL source is unavailable.")
                )
            } catch (exception: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    DatasetErrorResponse("INTERNAL_OPL_ERROR", exception.message ?: "An unexpected error occurred while searching OPL.")
                )
            }
        }
    }
}
