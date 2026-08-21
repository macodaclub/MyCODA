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


object OpenAireOrganizationClient {

    private const val BASE_URL =
        "https://api.openaire.eu/graph/v3/organizations"


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

    fun resolveOpenAireIdByRor(rawRor: String): String {
        val normalizedRor =
            ResearchIdentifierService.normalizeRor(rawRor)

        val encodedRor =
            URLEncoder.encode(
                normalizedRor,
                StandardCharsets.UTF_8
            )

        val url =
            "$BASE_URL?pid=$encodedRor&page=1&pageSize=10"

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
                "OpenAIRE organization resolution failed with status " +
                    "${response.statusCode()}: ${response.body()}"
            )
        }

        val root =
            json
                .parseToJsonElement(response.body())
                .jsonObject

        val results =
            root["results"]
                ?.asJsonArrayOrNull()
                ?: emptyList()

        val matchingOrganization =
            results
                .mapNotNull { element ->
                    element.asJsonObjectOrNull()
                }
                .firstOrNull { organization ->
                    organizationContainsRor(
                        organization = organization,
                        expectedRor = normalizedRor
                    )
                }

        requireNotNull(matchingOrganization) {
            "No OpenAIRE organization was found for ROR $normalizedRor."
        }

        return getString(matchingOrganization, "id")
            ?: throw IllegalStateException(
                "OpenAIRE returned an organization without an id."
            )
    }

    private fun organizationContainsRor(
        organization: JsonObject,
        expectedRor: String
    ): Boolean {
        val pids =
            organization["pids"]
                ?.asJsonArrayOrNull()
                ?: return false

        return pids.any { element ->
            val pid =
                element.asJsonObjectOrNull()
                    ?: return@any false

            val scheme =
                getString(pid, "scheme")

            if (!scheme.equals("ROR", ignoreCase = true)) {
                return@any false
            }

            val rawValue =
                getString(pid, "value")
                    ?: return@any false

            val normalized =
                ResearchIdentifierService
                    .tryNormalizeRor(rawValue)
                    ?: return@any false

            normalized == expectedRor
        }
    }

    private fun getString(
        obj: JsonObject,
        field: String
    ): String? =
        obj[field]
            ?.asJsonPrimitiveOrNull()
            ?.contentOrNull

    private fun JsonElement.asJsonPrimitiveOrNull(): JsonPrimitive? =
        this as? JsonPrimitive

    private fun JsonElement.asJsonObjectOrNull(): JsonObject? =
        this as? JsonObject

    private fun JsonElement.asJsonArrayOrNull(): JsonArray? =
        this as? JsonArray
}