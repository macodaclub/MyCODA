package io.github.macodaclub.services

import io.github.macodaclub.models.api.datasets.DatasetVocabularyOption
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

/**
 * Vocabulários usados pelos controlos OpenAIRE no frontend.
 * Todos são servidos pelo backend para manter o browser desacoplado
 * das APIs externas e para garantir que os valores enviados são válidos.
 */
object DatasetVocabularyService {

    private val json = Json {
        ignoreUnknownKeys = true
    }

    fun getVocabulary(name: String): List<DatasetVocabularyOption> =
        when (name) {
            "countries" -> loadSimpleVocabulary(
                "/datasets/vocabularies/countries.json"
            )

            "sdgs" -> loadSimpleVocabulary(
                "/datasets/vocabularies/sdgs.json"
            )

            "fields-of-science" -> loadFieldsOfScience()
            "instance-types" -> loadInstanceTypes()

            else -> throw IllegalArgumentException(
                "Unknown dataset vocabulary: $name."
            )
        }

    /** Compatibilidade com código existente. */
    fun getCountries(): List<DatasetVocabularyOption> =
        getVocabulary("countries")

    private fun loadSimpleVocabulary(
        resourcePath: String
    ): List<DatasetVocabularyOption> =
        json.decodeFromString<List<DatasetVocabularyOption>>(
            readResource(resourcePath)
        )

    /**
     * A OpenAIRE V3 aceita os labels OECD de nível 1 e 2.
     * O ficheiro fonte também contém granularidade inferior que não
     * expomos no query builder.
     */
    private fun loadFieldsOfScience(): List<DatasetVocabularyOption> {
        val document = json.decodeFromString<FosVocabularyDocument>(
            readResource("/datasets/vocabularies/fos.json")
        )

        return flattenFos(document.fos)
            .filter { it.level in 1..2 }
            .map { node ->
                DatasetVocabularyOption(
                    code = node.label.trim().lowercase(),
                    title = "${node.code} — ${node.label}"
                )
            }
            .distinctBy { it.code }
            .sortedBy { it.title.lowercase() }
    }

    /**
     * O filtro V3 instanceType recebe o nome do termo (ex.: Article),
     * e não o código numérico do vocabulário.
     */
    private fun loadInstanceTypes(): List<DatasetVocabularyOption> {
        val document = json.decodeFromString<OpenAireVocabularyDocument>(
            readResource("/datasets/vocabularies/instance-types.json")
        )

        return document.terms
            .mapNotNull { term ->
                val value = term.englishName.trim()

                if (value.isBlank()) {
                    null
                } else {
                    DatasetVocabularyOption(
                        code = value,
                        title = value
                    )
                }
            }
            .distinctBy { it.code }
            .sortedBy { it.title.lowercase() }
    }

    private fun flattenFos(
        nodes: List<FosVocabularyNode>
    ): List<FosVocabularyNode> = buildList {
        nodes.forEach { node ->
            add(node)
            addAll(flattenFos(node.children))
        }
    }

    private fun readResource(resourcePath: String): String {
        val inputStream = DatasetVocabularyService::class.java
            .getResourceAsStream(resourcePath)
            ?: throw IllegalStateException(
                "Dataset vocabulary was not found at $resourcePath."
            )

        return inputStream.bufferedReader().use { it.readText() }
    }

    @Serializable
    private data class OpenAireVocabularyDocument(
        val terms: List<OpenAireVocabularyTerm> = emptyList()
    )

    @Serializable
    private data class OpenAireVocabularyTerm(
        val englishName: String = "",
        val code: String = ""
    )

    @Serializable
    private data class FosVocabularyDocument(
        val fos: List<FosVocabularyNode> = emptyList()
    )

    @Serializable
    private data class FosVocabularyNode(
        val code: String = "",
        val id: String = "",
        val label: String = "",
        val level: Int = 0,
        val children: List<FosVocabularyNode> = emptyList()
    )
}
