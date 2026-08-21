package io.github.macodaclub.services

import io.github.macodaclub.models.api.datasets.DatasetAuthor
import io.github.macodaclub.models.api.datasets.DatasetSearchResult
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.jsonObject
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

/**
 * Cliente responsável pela comunicação com a OpenAIRE Graph API
 * e pela conversão dos research products para o modelo do MyCODA.
 */
data class OpenAireSearchPage(
    val results: List<DatasetSearchResult>,
    val totalResults: Long?,
    val page: Int?,
    val pageSize: Int?
)

object OpenAireResearchProductClient {

    private const val BASE_URL = "https://api.openaire.eu/graph/v3/research-products"

    private val httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build()

    private val json = Json {
        ignoreUnknownKeys = true
    }

    /**
     * Executa uma pesquisa OpenAIRE com a query previamente construída.
     */
    fun search(queryString: String): OpenAireSearchPage {
        val request = HttpRequest.newBuilder()
            .uri(URI.create(buildUrl(queryString)))
            .timeout(Duration.ofSeconds(30))
            .header("Accept", "application/json")
            .header("User-Agent", "MyCODA/1.0")
            .GET()
            .build()

        val response = httpClient.send(
            request,
            HttpResponse.BodyHandlers.ofString()
        )

        if (response.statusCode() !in 200..299) {
            throw RuntimeException(
                "OpenAIRE request failed with status " +
                    "${response.statusCode()}: ${response.body()}"
            )
        }

        val root = json
            .parseToJsonElement(response.body())
            .jsonObject

        val header = root["header"]
            ?.asJsonObjectOrNull()

        val totalResults = header
            ?.get("numFound")
            ?.asJsonPrimitiveOrNull()
            ?.longOrNull

        val responsePage = header
            ?.get("page")
            ?.asJsonPrimitiveOrNull()
            ?.intOrNull

        val responsePageSize = header
            ?.get("pageSize")
            ?.asJsonPrimitiveOrNull()
            ?.intOrNull

        val results = root["results"]
            ?.asJsonArrayOrNull()
            ?.mapIndexedNotNull { index, item ->
                val researchProduct = item.asJsonObjectOrNull()
                    ?: return@mapIndexedNotNull null

                mapResearchProduct(index, researchProduct)
            }
            ?: emptyList()

        return OpenAireSearchPage(
            results = results,
            totalResults = totalResults,
            page = responsePage,
            pageSize = responsePageSize
        )
    }

    /**
     * Constrói o URL final a partir da query já codificada.
     */
    private fun buildUrl(queryString: String): String {
        val normalizedQuery = queryString.trim()

        return if (normalizedQuery.isBlank()) {
            BASE_URL
        } else {
            "$BASE_URL?$normalizedQuery"
        }
    }

    /**
     * Converte um research product OpenAIRE para o DTO usado pelo MyCODA.
     */
    private fun mapResearchProduct(
        index: Int,
        obj: JsonObject
    ): DatasetSearchResult {
        val authorDetails = extractAuthorDetails(obj)

        return DatasetSearchResult(
            id = getString(obj, "id")
                ?: index.toString(),

            type = getString(obj, "type")
                .orEmpty(),

            instanceType = extractInstanceType(obj),

            title = getString(obj, "mainTitle")
                ?: getString(obj, "title")
                ?: "",

            abstract = extractDescription(obj),

            subjects = extractSubjects(obj),

            authors = authorDetails
                .joinToString(", ") { it.name },

            authorDetails = authorDetails,

            publicationDate = getString(obj, "publicationDate")
                ?: getString(obj, "dateOfAcceptance")
                ?: "",

            publisher = getString(obj, "publisher"),

            scientificEvent = extractScientificEvent(obj),

            citations = extractCitationCount(obj),

            influenceClass = extractCitationImpactClass(
                obj,
                "influenceClass"
            ),

            popularityClass = extractCitationImpactClass(
                obj,
                "popularityClass"
            ),

            impulseClass = extractCitationImpactClass(
                obj,
                "impulseClass"
            ),

            citationCountClass = extractCitationImpactClass(
                obj,
                "citationClass"
            ),

            relatedMaterials = extractRelatedMaterials(obj),

            countryCode = extractCountryCode(obj),

            sdg = extractSubjectsByScheme(
                obj,
                "SDG"
            ),

            fos = extractSubjectsByScheme(
                obj,
                "FOS"
            )
        )
    }

    /**
     * Obtém a primeira descrição disponível do research product.
     */
    private fun extractDescription(
        obj: JsonObject
    ): String? {
        return obj["descriptions"]
            ?.asJsonArrayOrNull()
            ?.mapNotNull { element ->
                element.asJsonPrimitiveOrNull()
                    ?.contentOrNull
            }
            ?.firstOrNull {
                it.isNotBlank()
            }
    }

    /**
     * Lê um campo JSON primitivo como String.
     */
    private fun getString(
        obj: JsonObject,
        field: String
    ): String? {
        return obj[field]
            ?.asJsonPrimitiveOrNull()
            ?.contentOrNull
    }

    private fun JsonElement.asJsonPrimitiveOrNull(): JsonPrimitive? {
        return this as? JsonPrimitive
    }

    private fun JsonElement.asJsonObjectOrNull(): JsonObject? {
        return this as? JsonObject
    }

    private fun JsonElement.asJsonArrayOrNull(): JsonArray? {
        return this as? JsonArray
    }

    /**
     * Extrai os autores e os respetivos ORCID, quando disponíveis.
     */
    private fun extractAuthorDetails(
        obj: JsonObject
    ): List<DatasetAuthor> {
        val authors = obj["authors"]
            ?.asJsonArrayOrNull()
            ?: return emptyList()

        return authors.mapNotNull { element ->
            val author = element.asJsonObjectOrNull()
                ?: return@mapNotNull null

            val name = getString(author, "fullName")
                ?: buildAuthorName(author)
                ?: return@mapNotNull null

            DatasetAuthor(
                name = name,
                orcid = extractAuthorOrcid(author)
            )
        }
    }

    /**
     * Constrói o nome do autor quando fullName não está disponível.
     */
    private fun buildAuthorName(
        author: JsonObject
    ): String? {
        val firstName = getString(author, "name")
            ?.trim()
            .orEmpty()

        val surname = getString(author, "surname")
            ?.trim()
            .orEmpty()

        return listOf(
            firstName,
            surname
        )
            .filter {
                it.isNotBlank()
            }
            .joinToString(" ")
            .takeIf {
                it.isNotBlank()
            }
    }

    /**
     * Extrai o identificador ORCID do autor.
     */
    private fun extractAuthorOrcid(
        author: JsonObject
    ): String? {
        val pid = author["pid"]
            ?.asJsonObjectOrNull()
            ?: return null

        val identifier = pid["id"]
            ?.asJsonObjectOrNull()
            ?: return null

        val scheme = getString(
            identifier,
            "scheme"
        ) ?: return null

        if (!scheme.equals(
                "orcid",
                ignoreCase = true
            )
        ) {
            return null
        }

        return getString(
            identifier,
            "value"
        )
    }

    /**
     * Obtém o primeiro tipo de instância disponível.
     */
    private fun extractInstanceType(
        obj: JsonObject
    ): String? {
        val instances = obj["instances"]
            ?.asJsonArrayOrNull()
            ?: obj["instance"]
                ?.asJsonArrayOrNull()
            ?: return null

        return instances
            .mapNotNull {
                it.asJsonObjectOrNull()
            }
            .mapNotNull {
                getString(
                    it,
                    "type"
                )
            }
            .firstOrNull {
                it.isNotBlank()
            }
    }

    /**
     * Obtém o nome do evento, revista ou contentor associado.
     */
    private fun extractScientificEvent(
        obj: JsonObject
    ): String? {
        val container = obj["container"]
            ?.asJsonObjectOrNull()
            ?: return null

        return getString(
            container,
            "name"
        )
    }

    /**
     * Obtém uma classe de impacto bibliométrico.
     */
    private fun extractCitationImpactClass(
        obj: JsonObject,
        field: String
    ): String? {
        val indicators = obj["indicators"]
            ?.asJsonObjectOrNull()
            ?: return null

        val citationImpact = indicators["citationImpact"]
            ?.asJsonObjectOrNull()
            ?: return null

        return getString(
            citationImpact,
            field
        )
    }

    /**
     * Obtém o número de citações.
     *
     * A OpenAIRE pode devolver citationCount como número decimal,
     * por exemplo 0.0 ou 5.0.
     */
    private fun extractCitationCount(
        obj: JsonObject
    ): Int? {
        val indicators = obj["indicators"]
            ?.asJsonObjectOrNull()
            ?: return null

        val citationImpact = indicators["citationImpact"]
            ?.asJsonObjectOrNull()
            ?: return null

        return citationImpact["citationCount"]
            ?.asJsonPrimitiveOrNull()
            ?.contentOrNull
            ?.toDoubleOrNull()
            ?.toInt()
    }

    /**
     * Agrega PIDs, URLs e outros materiais relacionados.
     */
    private fun extractRelatedMaterials(
        obj: JsonObject
    ): String? {
        val values =
            mutableListOf<String>()

        values.addAll(
            extractPids(obj)
        )

        values.addAll(
            extractStringArray(
                obj,
                "documentationUrls"
            )
        )

        getString(
            obj,
            "codeRepositoryUrl"
        )
            ?.let(
                values::add
            )

        values.addAll(
            extractInstanceUrls(obj)
        )

        values.addAll(
            extractInstancePids(obj)
        )

        values.addAll(
            extractInstanceAlternateIdentifiers(obj)
        )

        values.addAll(
            extractDirectRelatedMaterials(obj)
        )

        return values
            .map {
                it.trim()
            }
            .filter {
                it.isNotBlank()
            }
            .distinct()
            .joinToString(", ")
            .ifBlank {
                null
            }
    }

    private fun extractDirectRelatedMaterials(
        obj: JsonObject
    ): List<String> {
        val relatedMaterials =
            obj["relatedMaterials"]
                ?: return emptyList()

        return when (relatedMaterials) {
            is JsonArray ->
                relatedMaterials.mapNotNull { item ->
                    when (item) {
                        is JsonObject ->
                            extractRelatedMaterialValue(
                                item
                            )

                        else ->
                            item
                                .asJsonPrimitiveOrNull()
                                ?.contentOrNull
                    }
                }

            is JsonObject ->
                listOfNotNull(
                    extractRelatedMaterialValue(
                        relatedMaterials
                    )
                )

            else ->
                listOfNotNull(
                    relatedMaterials
                        .asJsonPrimitiveOrNull()
                        ?.contentOrNull
                )
        }
    }

    private fun extractPids(
        obj: JsonObject
    ): List<String> {
        val pids = obj["pids"]
            ?.asJsonArrayOrNull()
            ?: return emptyList()

        return extractPidArray(
            pids
        )
    }

    private fun extractPidArray(
        pids: JsonArray
    ): List<String> {
        return pids.mapNotNull { item ->
            val pid = item
                .asJsonObjectOrNull()
                ?: return@mapNotNull null

            val scheme = getString(
                pid,
                "scheme"
            )
                ?.lowercase()

            val value = getString(
                pid,
                "value"
            )
                ?: return@mapNotNull null

            when (scheme) {
                "doi" ->
                    toDoiUrl(
                        value
                    )

                else ->
                    value
            }
        }
    }

    private fun extractStringArray(
        obj: JsonObject,
        field: String
    ): List<String> {
        val array = obj[field]
            ?.asJsonArrayOrNull()
            ?: return emptyList()

        return array.mapNotNull { item ->
            item
                .asJsonPrimitiveOrNull()
                ?.contentOrNull
        }
    }

    private fun extractInstanceUrls(
        obj: JsonObject
    ): List<String> {
        val instances = obj["instances"]
            ?.asJsonArrayOrNull()
            ?: return emptyList()

        return instances.flatMap { item ->
            val instance = item
                .asJsonObjectOrNull()
                ?: return@flatMap emptyList()

            extractStringArray(
                instance,
                "urls"
            )
        }
    }

    private fun extractInstancePids(
        obj: JsonObject
    ): List<String> {
        val instances = obj["instances"]
            ?.asJsonArrayOrNull()
            ?: return emptyList()

        return instances.flatMap { item ->
            val instance = item
                .asJsonObjectOrNull()
                ?: return@flatMap emptyList()

            val pids =
                instance["pids"]
                    ?.asJsonArrayOrNull()
                    ?: return@flatMap emptyList()

            extractPidArray(
                pids
            )
        }
    }

    private fun extractInstanceAlternateIdentifiers(
        obj: JsonObject
    ): List<String> {
        val instances = obj["instances"]
            ?.asJsonArrayOrNull()
            ?: return emptyList()

        return instances.flatMap { item ->
            val instance = item
                .asJsonObjectOrNull()
                ?: return@flatMap emptyList()

            val alternateIdentifiers =
                instance["alternateIdentifiers"]
                    ?.asJsonArrayOrNull()
                    ?: return@flatMap emptyList()

            extractPidArray(
                alternateIdentifiers
            )
        }
    }

    private fun extractRelatedMaterialValue(
        material: JsonObject
    ): String? {
        return getString(
            material,
            "url"
        )
            ?: getString(
                material,
                "doi"
            )?.let(
                ::toDoiUrl
            )
            ?: getString(
                material,
                "pid"
            )
            ?: getString(
                material,
                "id"
            )
            ?: getString(
                material,
                "value"
            )
    }

    /**
     * Normaliza um DOI para URL sem alterar URLs já completas.
     */
    private fun toDoiUrl(
        value: String
    ): String {
        val trimmed =
            value.trim()

        return when {
            trimmed.startsWith(
                "http://",
                ignoreCase = true
            ) ||
                trimmed.startsWith(
                    "https://",
                    ignoreCase = true
                ) ->
                trimmed

            trimmed.startsWith(
                "doi:",
                ignoreCase = true
            ) -> {
                val doi =
                    trimmed.substringAfter(
                        ":",
                        missingDelimiterValue = trimmed
                    )

                "https://doi.org/$doi"
            }

            else ->
                "https://doi.org/$trimmed"
        }
    }

    /**
     * Extrai os valores textuais associados aos subjects.
     */
    private fun extractSubjects(
        obj: JsonObject
    ): List<String> {
        val subjects = obj["subjects"]
            ?.asJsonArrayOrNull()
            ?: return emptyList()

        return subjects
            .mapNotNull { element ->
                val wrapper =
                    element.asJsonObjectOrNull()
                        ?: return@mapNotNull null

                val subject =
                    wrapper["subject"]
                        ?.asJsonObjectOrNull()
                        ?: return@mapNotNull null

                getString(
                    subject,
                    "value"
                )
            }
            .filter {
                it.isNotBlank()
            }
            .distinct()
    }

    /**
     * Filtra os subjects pelo respetivo scheme,
     * por exemplo SDG ou FOS.
     */
    private fun extractSubjectsByScheme(
        obj: JsonObject,
        expectedScheme: String
    ): List<String> {
        val subjects = obj["subjects"]
            ?.asJsonArrayOrNull()
            ?: return emptyList()

        return subjects
            .mapNotNull { element ->
                val wrapper =
                    element.asJsonObjectOrNull()
                        ?: return@mapNotNull null

                val subject =
                    wrapper["subject"]
                        ?.asJsonObjectOrNull()
                        ?: return@mapNotNull null

                val scheme =
                    getString(
                        subject,
                        "scheme"
                    )
                        ?: return@mapNotNull null

                if (!scheme.equals(
                        expectedScheme,
                        ignoreCase = true
                    )
                ) {
                    return@mapNotNull null
                }

                getString(
                    subject,
                    "value"
                )
            }
            .filter {
                it.isNotBlank()
            }
            .distinct()
    }

    /**
     * Extrai o código de país suportando as formas conhecidas da resposta.
     */
    private fun extractCountryCode(
        obj: JsonObject
    ): String? {
        getString(
            obj,
            "countryCode"
        )
            ?.takeIf {
                it.isNotBlank()
            }
            ?.let {
                return it
            }

        val countries =
            obj["country"]
                ?.asJsonArrayOrNull()
                ?: obj["countries"]
                    ?.asJsonArrayOrNull()
                ?: return null

        return countries
            .mapNotNull {
                it.asJsonObjectOrNull()
            }
            .mapNotNull { country ->
                getString(
                    country,
                    "code"
                )
                    ?: getString(
                        country,
                        "countryCode"
                    )
            }
            .firstOrNull {
                it.isNotBlank()
            }
    }
}