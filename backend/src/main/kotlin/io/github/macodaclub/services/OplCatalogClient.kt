package io.github.macodaclub.services

import io.github.macodaclub.models.api.datasets.OplDatasetResult
import org.yaml.snakeyaml.LoaderOptions
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.SafeConstructor
import java.io.IOException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.net.http.HttpTimeoutException
import java.time.Duration

/**
 * Cliente da Optimisation Problem Library (OPL).
 *
 * catálogo problems.yaml
 */
object OplCatalogClient {

    const val DEFAULT_SOURCE_URL = "https://raw.githubusercontent.com/OpenOptimizationOrg/OPL/refs/heads/main/problems.yaml"

    private val sourceUrl = System.getenv("OPL_PROBLEMS_URL")
        ?.trim()
        ?.takeIf { it.isNotBlank() }
        ?: DEFAULT_SOURCE_URL

    private val timeoutSeconds = System.getenv("OPL_TIMEOUT_SECONDS")
        ?.toLongOrNull()
        ?.takeIf { it > 0 }
        ?: 30L

    private val cacheTtlMillis = (System.getenv("OPL_CACHE_TTL_SECONDS")
        ?.toLongOrNull()
        ?.takeIf { it >= 0 }
        ?: 300L) * 1_000L

    private val httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(timeoutSeconds))
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build()

    private val yaml = Yaml(SafeConstructor(LoaderOptions()))

    @Volatile
    private var cachedCatalog: CachedCatalog? = null

    fun load(): OplCatalogSnapshot {
        val now = System.currentTimeMillis()
        val cached = cachedCatalog

        if (cached != null && now - cached.loadedAtMillis <= cacheTtlMillis) {
            return OplCatalogSnapshot(cached.results, emptyList(), sourceUrl)
        }

        return synchronized(this) {
            val refreshedNow = System.currentTimeMillis()
            val currentCache = cachedCatalog

            if (currentCache != null && refreshedNow - currentCache.loadedAtMillis <= cacheTtlMillis) {
                return@synchronized OplCatalogSnapshot(currentCache.results, emptyList(), sourceUrl)
            }

            try {
                val response = fetch(currentCache)

                if (response.statusCode() == 304 && currentCache != null) {
                    val refreshed = currentCache.copy(loadedAtMillis = refreshedNow)
                    cachedCatalog = refreshed
                    return@synchronized OplCatalogSnapshot(refreshed.results, emptyList(), sourceUrl)
                }

                if (response.statusCode() !in 200..299) {
                    throw OplSourceUnavailableException(
                        "The OPL source returned HTTP ${response.statusCode()}."
                    )
                }

                val parsedResults = parseCatalog(response.body())
                val newCache = CachedCatalog(
                    results = parsedResults,
                    loadedAtMillis = refreshedNow,
                    etag = response.headers().firstValue("ETag").orElse(null),
                    lastModified = response.headers().firstValue("Last-Modified").orElse(null)
                )

                cachedCatalog = newCache
                OplCatalogSnapshot(parsedResults, emptyList(), sourceUrl)
            } catch (exception: OplSourceException) {
                staleOrThrow(currentCache, exception)
            } catch (exception: HttpTimeoutException) {
                staleOrThrow(
                    currentCache,
                    OplSourceTimeoutException("The OPL source did not respond within the expected time.", exception)
                )
            } catch (exception: IOException) {
                staleOrThrow(
                    currentCache,
                    OplSourceUnavailableException("It was not possible to communicate with the OPL source.", exception)
                )
            } catch (exception: InterruptedException) {
                Thread.currentThread().interrupt()
                staleOrThrow(
                    currentCache,
                    OplSourceUnavailableException("The OPL source request was interrupted.", exception)
                )
            } catch (exception: Exception) {
                staleOrThrow(
                    currentCache,
                    OplSourceUnavailableException("The OPL catalogue could not be parsed.", exception)
                )
            }
        }
    }

    private fun fetch(cached: CachedCatalog?): HttpResponse<String> {
        val builder = HttpRequest.newBuilder()
            .uri(URI.create(sourceUrl))
            .timeout(Duration.ofSeconds(timeoutSeconds))
            .header("Accept", "application/yaml, text/yaml, text/plain, */*")
            .header("User-Agent", "MyCODA/1.0")
            .GET()

        cached?.etag?.takeIf { it.isNotBlank() }?.let { builder.header("If-None-Match", it) }
        cached?.lastModified?.takeIf { it.isNotBlank() }?.let { builder.header("If-Modified-Since", it) }

        return httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString())
    }

    private fun staleOrThrow(cached: CachedCatalog?, exception: OplSourceException): OplCatalogSnapshot {
        if (cached == null) throw exception

        return OplCatalogSnapshot(
            results = cached.results,
            warnings = listOf(
                "The live OPL source could not be refreshed; the most recently cached catalogue is being used. ${exception.message.orEmpty()}"
            ),
            sourceUrl = sourceUrl
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseCatalog(yamlText: String): List<OplDatasetResult> {
        val loaded = yaml.load<Any?>(yamlText) as? Map<*, *>
            ?: throw OplSourceUnavailableException("The OPL YAML root is not a map.")

        val library = loaded.entries.associate { (key, value) ->
            key.toString() to (value as? Map<*, *> ?: emptyMap<Any?, Any?>())
        }

        val implementations = library.filterValues { entry ->
            entry.string("type").equals("implementation", ignoreCase = true)
        }

        return library.entries
            .filter { (_, entry) ->
                entry.string("type")?.lowercase() in setOf("problem", "suite", "generator")
            }
            .map { (id, entry) -> mapProblemLike(id, entry, library, implementations) }
            .sortedWith(compareBy<OplDatasetResult> { it.name?.lowercase().orEmpty() }.thenBy { it.id.lowercase() })
    }

    private fun mapProblemLike(
        id: String,
        entry: Map<*, *>,
        library: Map<String, Map<*, *>>,
        implementations: Map<String, Map<*, *>>
    ): OplDatasetResult {
        val problemIds = entry.stringValues("problems")
        val referencedProblems = problemIds.mapNotNull { problemId ->
            library[problemId]?.takeIf { it.string("type").equals("problem", ignoreCase = true) }
        }
        val ownVariables = entry.mapList("variables")
        val effectiveVariables = if (ownVariables.isNotEmpty()) ownVariables else referencedProblems.flatMap { it.mapList("variables") }
        val effectiveConstraints = when {
            entry.containsKey("constraints") && entry["constraints"] != null -> entry.mapList("constraints")
            else -> referencedProblems.flatMap { it.mapList("constraints") }
        }
        val ownFidelityLevels = entry.stringValues("fidelity_levels")
        val effectiveFidelityLevels = if (ownFidelityLevels.isNotEmpty()) ownFidelityLevels else referencedProblems.flatMap { it.stringValues("fidelity_levels") }.distinct()
        val implementationIds = entry.stringValues("implementations")
        val resolvedImplementations = implementationIds.mapNotNull { implementationId ->
            implementations[implementationId]?.let { implementationId to it }
        }

        val implementationNames = resolvedImplementations.mapNotNull { (_, implementation) ->
            implementation.string("name")
        }.distinct()

        val implementationLinks = resolvedImplementations.flatMap { (_, implementation) ->
            extractLinkUrls(implementation["links"])
        }.distinct()

        val implementationLanguages = resolvedImplementations.mapNotNull { (_, implementation) ->
            implementation.string("language")
        }.distinct()

        val implementationEvaluationTimes = resolvedImplementations.flatMap { (_, implementation) ->
            implementation.stringValues("evaluation_time")
        }.distinct()

        val references = entry.mapList("references")
        val referenceLinks = references.flatMap { reference -> extractLinkUrls(reference["link"]) }.distinct()
        val codeExampleLinks = extractUrlLikeValues(entry["code_examples"])

        return OplDatasetResult(
            id = id,
            name = entry.string("name"),
            longName = entry.string("long_name"),
            description = entry.string("description"),
            objectives = entry.stringValues("objectives"),
            variableTypes = effectiveVariables.mapNotNull { it.string("type") }.distinct(),
            variableDimensions = effectiveVariables.mapNotNull { formatNumericValue(it["dim"]) }.distinct(),
            constraintTypes = effectiveConstraints.mapNotNull { it.string("type") }.distinct(),
            numberOfConstraints = effectiveConstraints.mapNotNull { formatNumericValue(it["number"]) }.distinct(),
            constraintHardness = effectiveConstraints.mapNotNull { scalarString(it["hard"]) }.distinct(),
            constraintEquality = effectiveConstraints.mapNotNull { scalarString(it["equality"]) }.distinct(),
            modality = entry.stringValues("modality").joinToString(", ").ifBlank { null },
            noiseType = entry.stringValues("noise_type").joinToString(", ").ifBlank { null },
            type = entry.string("type"),
            source = "OPL",
            problemSources = entry.stringValues("source"),
            dynamicTypes = entry.stringValues("dynamic_type"),
            fidelityLevels = effectiveFidelityLevels,
            evaluationTimes = entry.stringValues("evaluation_time").ifEmpty { implementationEvaluationTimes },
            problems = problemIds,
            instances = stringValues(entry["instances"]),
            codeExamples = entry.stringValues("code_examples"),
            implementationIds = implementationIds,
            implementationNames = implementationNames,
            implementationLinks = implementationLinks,
            implementationLanguages = implementationLanguages,
            implementationEvaluationTimes = implementationEvaluationTimes,
            tags = entry.stringValues("tags"),
            allowsPartialEvaluation = scalarString(entry["allows_partial_evaluation"]),
            canEvaluateObjectivesIndependently = scalarString(entry["can_evaluate_objectives_independently"]),
            authors = references.flatMap { it.stringValues("authors") }.distinct(),
            referenceTitles = references.mapNotNull { it.string("title") }.distinct(),
            links = (referenceLinks + implementationLinks + codeExampleLinks).distinct()
        )
    }

    private fun Map<*, *>.string(key: String): String? = scalarString(this[key])

    private fun Map<*, *>.stringValues(key: String): List<String> = stringValues(this[key])

    private fun Map<*, *>.mapList(key: String): List<Map<*, *>> = when (val value = this[key]) {
        is List<*> -> value.mapNotNull { it as? Map<*, *> }
        is Map<*, *> -> listOf(value)
        else -> emptyList()
    }

    private fun stringValues(value: Any?): List<String> = when (value) {
        null -> emptyList()
        is List<*> -> value.flatMap(::stringValues).distinct()
        is Set<*> -> value.flatMap(::stringValues).distinct()
        is Map<*, *> -> listOfNotNull(formatNumericValue(value))
        else -> listOfNotNull(scalarString(value))
    }

    private fun scalarString(value: Any?): String? = when (value) {
        null -> null
        is Boolean -> if (value) "yes" else "no"
        is Number -> value.toString()
        is String -> value.trim().takeIf { it.isNotBlank() }
        else -> value.toString().trim().takeIf { it.isNotBlank() }
    }

    private fun formatNumericValue(value: Any?): String? = when (value) {
        null -> null
        is Map<*, *> -> {
            val min = scalarString(value["min"])
            val max = scalarString(value["max"])
            when {
                min != null && max != null -> "$min..$max"
                min != null -> ">=$min"
                max != null -> "<=$max"
                else -> value.entries.joinToString(", ") { (key, item) -> "$key=${scalarString(item).orEmpty()}" }.ifBlank { null }
            }
        }
        is List<*> -> value.mapNotNull(::formatNumericValue).joinToString(", ").ifBlank { null }
        else -> scalarString(value)
    }

    private fun extractLinkUrls(value: Any?): List<String> = when (value) {
        null -> emptyList()
        is List<*> -> value.flatMap(::extractLinkUrls)
        is Map<*, *> -> {
            val direct = scalarString(value["url"])
            if (direct != null) listOf(direct) else value.values.flatMap(::extractLinkUrls)
        }
        is String -> listOf(value).filter { it.startsWith("http://", true) || it.startsWith("https://", true) }
        else -> emptyList()
    }

    private fun extractUrlLikeValues(value: Any?): List<String> = when (value) {
        null -> emptyList()
        is List<*> -> value.flatMap(::extractUrlLikeValues)
        is Map<*, *> -> value.values.flatMap(::extractUrlLikeValues)
        is String -> listOf(value).filter { it.startsWith("http://", true) || it.startsWith("https://", true) }
        else -> emptyList()
    }

    private data class CachedCatalog(
        val results: List<OplDatasetResult>,
        val loadedAtMillis: Long,
        val etag: String?,
        val lastModified: String?
    )
}

data class OplCatalogSnapshot(
    val results: List<OplDatasetResult>,
    val warnings: List<String>,
    val sourceUrl: String
)

open class OplSourceException(message: String, cause: Throwable? = null) : IOException(message, cause)
class OplSourceTimeoutException(message: String, cause: Throwable? = null) : OplSourceException(message, cause)
class OplSourceUnavailableException(message: String, cause: Throwable? = null) : OplSourceException(message, cause)
