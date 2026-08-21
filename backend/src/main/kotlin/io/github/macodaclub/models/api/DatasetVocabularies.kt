package io.github.macodaclub.routes.api

import io.github.macodaclub.services.DatasetVocabularyService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.ktor.server.routing.route

/**
 * Endpoint genérico para os vocabulários do query builder de datasets.
 */
fun Routing.datasetVocabularyRoutes() {
    route("/api/datasets/vocabularies") {
        get("/{vocabulary}") {
            val vocabulary = call.parameters["vocabulary"]
                ?.trim()
                .orEmpty()

            try {
                val options = DatasetVocabularyService
                    .getVocabulary(vocabulary)

                call.respond(HttpStatusCode.OK, options)
            } catch (exception: IllegalArgumentException) {
                call.respond(
                    HttpStatusCode.NotFound,
                    DatasetErrorResponse(
                        code = "UNKNOWN_DATASET_VOCABULARY",
                        message = exception.message
                            ?: "Unknown dataset vocabulary."
                    )
                )
            } catch (exception: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    DatasetErrorResponse(
                        code = "DATASET_VOCABULARY_ERROR",
                        message = exception.message
                            ?: "Failed to load dataset vocabulary."
                    )
                )
            }
        }
    }
}
