package io.github.macodaclub.routes.api

import io.github.macodaclub.models.api.datasets.DatasetSearchRequest
import io.github.macodaclub.services.DatasetSearchService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializable
import java.io.IOException
import java.net.http.HttpTimeoutException

/**
 * Modelo utilizado para tratar erros da API de datasets
 * num formato JSON para o frontend.
 */
@Serializable
data class DatasetErrorResponse(
    val code: String,
    val message: String
)

/**
 * Regista os endpoints relacionados com a pesquisa de datasets.
 *
 * O endpoint POST /api/datasets/search:
 * - recebe os critérios enviados pelo frontend;
 * - valida e processa o pedido através do DatasetSearchService;
 * - devolve os resultados normalizados;
 * - converte erros de validação e integração em respostas HTTP adequadas.
 */
fun Routing.datasetRoutes() {
    route("/api") {
        route("/datasets") {
            post("/search") {
                try {
                    val request =
                        call.receive<DatasetSearchRequest>()

                    val response =
                        DatasetSearchService.search(request)

                    call.respond(
                        HttpStatusCode.OK,
                        response
                    )
                } catch (exception: IllegalArgumentException) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        DatasetErrorResponse(
                            code = "INVALID_DATASET_QUERY",
                            message = exception.message
                                ?: "The dataset query is invalid."
                        )
                    )
                } catch (exception: BadRequestException) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        DatasetErrorResponse(
                            code = "INVALID_REQUEST_BODY",
                            message =
                                "The dataset search request body is invalid."
                        )
                    )
                } catch (exception: SerializationException) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        DatasetErrorResponse(
                            code = "INVALID_REQUEST_BODY",
                            message =
                                "The dataset search request contains invalid fields or values."
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
                            code = "DATASET_SEARCH_INTERRUPTED",
                            message =
                                "The dataset search was interrupted."
                        )
                    )
                } catch (exception: RuntimeException) {
                    call.respond(
                        HttpStatusCode.BadGateway,
                        DatasetErrorResponse(
                            code = "OPENAIRE_REQUEST_ERROR",
                            message = exception.message
                                ?: "OpenAIRE could not complete the request."
                        )
                    )
                } catch (exception: Exception) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        DatasetErrorResponse(
                            code = "INTERNAL_DATASET_ERROR",
                            message =
                                "An unexpected error occurred while searching datasets."
                        )
                    )
                }
            }
        }
    }
}