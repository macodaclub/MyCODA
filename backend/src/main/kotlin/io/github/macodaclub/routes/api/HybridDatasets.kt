package io.github.macodaclub.routes.api

import io.github.macodaclub.models.api.datasets.HybridDatasetSearchRequest
import io.github.macodaclub.services.HybridDatasetSearchService
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
import java.io.IOException
import java.net.http.HttpTimeoutException

/**
 * Regista o endpoint de pesquisa híbrida OpenAIRE + OPL.
 *
 * POST /api/datasets/hybrid/search
 */
fun Routing.hybridDatasetRoutes() {
    route("/api") {
        route("/datasets") {
            route("/hybrid") {
                post("/search") {
                    try {
                        val request =
                            call.receive<HybridDatasetSearchRequest>()

                        val response =
                            HybridDatasetSearchService.search(
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
                                code = "INVALID_HYBRID_DATASET_QUERY",
                                message = exception.message
                                    ?: "The hybrid dataset query is invalid."
                            )
                        )
                    } catch (exception: BadRequestException) {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            DatasetErrorResponse(
                                code = "INVALID_REQUEST_BODY",
                                message =
                                    "The hybrid dataset search request body is invalid."
                            )
                        )
                    } catch (exception: SerializationException) {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            DatasetErrorResponse(
                                code = "INVALID_REQUEST_BODY",
                                message =
                                    "The hybrid dataset search contains invalid fields or values."
                            )
                        )
                    } catch (exception: OplSourceTimeoutException) {
                        call.respond(
                            HttpStatusCode.GatewayTimeout,
                            DatasetErrorResponse(
                                code = "OPL_TIMEOUT",
                                message = exception.message ?: "The OPL source did not respond within the expected time."
                            )
                        )
                    } catch (exception: OplSourceUnavailableException) {
                        call.respond(
                            HttpStatusCode.BadGateway,
                            DatasetErrorResponse(
                                code = "OPL_SOURCE_UNAVAILABLE",
                                message = exception.message ?: "The OPL source is unavailable."
                            )
                        )
                    } catch (exception: HttpTimeoutException) {
                        call.respond(
                            HttpStatusCode.GatewayTimeout,
                            DatasetErrorResponse(
                                code = "OPENAIRE_TIMEOUT",
                                message =
                                    "OpenAIRE did not respond within the expected time."
                            )
                        )
                    } catch (exception: IOException) {
                        call.respond(
                            HttpStatusCode.BadGateway,
                            DatasetErrorResponse(
                                code = "OPENAIRE_CONNECTION_ERROR",
                                message =
                                    "It was not possible to communicate with OpenAIRE."
                            )
                        )
                    } catch (exception: InterruptedException) {
                        Thread.currentThread().interrupt()

                        call.respond(
                            HttpStatusCode.ServiceUnavailable,
                            DatasetErrorResponse(
                                code = "HYBRID_SEARCH_INTERRUPTED",
                                message =
                                    "The hybrid dataset search was interrupted."
                            )
                        )
                    } catch (exception: RuntimeException) {
                        call.respond(
                            HttpStatusCode.BadGateway,
                            DatasetErrorResponse(
                                code = "HYBRID_SEARCH_ERROR",
                                message = exception.message
                                    ?: "The hybrid dataset search could not be completed."
                            )
                        )
                    } catch (exception: Exception) {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            DatasetErrorResponse(
                                code = "INTERNAL_HYBRID_DATASET_ERROR",
                                message =
                                    "An unexpected error occurred during the hybrid dataset search."
                            )
                        )
                    }
                }
            }
        }
    }
}