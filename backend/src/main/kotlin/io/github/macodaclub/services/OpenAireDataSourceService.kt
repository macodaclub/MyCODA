package io.github.macodaclub.services

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.time.Duration

object OpenAireDataSourceService {

    private const val BASE_URL =
        "https://api.openaire.eu/graph/v1/dataSources"


    private val openAireTimeoutSeconds: Long =
        System.getenv("OpenAIRETimeout")
            ?.toLongOrNull()
            ?.takeIf { timeout -> timeout > 0 }
            ?: 50L

    private val httpClient: HttpClient =
        HttpClient.newBuilder()
            .connectTimeout(
                Duration.ofSeconds(openAireTimeoutSeconds)
            )
            .build()

    private val json = Json {
        ignoreUnknownKeys = true
    }

    fun resolveJournalId(
        journalName: String
    ): String? {
        val normalizedName =
            journalName.trim()

        if (normalizedName.isBlank()) {
            return null
        }

        val url =
            BASE_URL +
                "?dataSourceTypeName=" +
                encode("Journal") +
                "&officialName=" +
                encode(normalizedName) +
                "&page=1" +
                "&pageSize=10"

        val request =
            HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(
                    Duration.ofSeconds(openAireTimeoutSeconds)
                )
                .header("Accept", "application/json")
                .header("User-Agent", "MyCODA/1.0")
                .GET()
                .build()

        val response =
            httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString()
            )

        if (response.statusCode() !in 200..299) {
            throw RuntimeException(
                "OpenAIRE data source request failed with status " +
                    "${response.statusCode()}: ${response.body()}"
            )
        }

        val root = json
                .parseToJsonElement(response.body())
                .jsonObject

        val results =
            root["results"] as? JsonArray
                ?: return null

        val firstResult =
            results.firstOrNull() as? JsonObject
                ?: return null

        return firstResult["id"]
            ?.asJsonPrimitiveOrNull()
            ?.contentOrNull
    }

    private fun encode(
        value: String
    ): String {
        return URLEncoder.encode(
            value,
            StandardCharsets.UTF_8
        )
    }

    private fun JsonElement.asJsonPrimitiveOrNull(): JsonPrimitive? {
        return this as? JsonPrimitive
    }
}