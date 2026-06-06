package io.github.macodaclub.routes.api

import io.github.macodaclub.models.api.query.PostSqwrlRequest
import io.github.macodaclub.plugins.OntologyManager
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

@Serializable
data class SqwrlQueryResponse(
    val query: String,
    val count: Int,
    val results: List<String>
)

private fun getEntityDisplayName(
    ontologyManager: OntologyManager,
    iri: org.semanticweb.owlapi.model.IRI
): String {
    val ontology = ontologyManager.mergedOntology

    val label = ontology.getAnnotationAssertionAxioms(iri)
        .firstOrNull { axiom ->
            axiom.property.isLabel &&
                axiom.value is org.semanticweb.owlapi.model.OWLLiteral
        }
        ?.value as? org.semanticweb.owlapi.model.OWLLiteral

    return label?.literal ?: iri.shortForm.substringAfter(":")
}

private fun getEntityDisplayNameByShortName(
    ontologyManager: OntologyManager,
    shortName: String
): String {
    val cleanShortName = shortName.substringAfter(":")
    val iriPrefix = System.getenv("MYCODA_ONTOLOGY_IRI_PREFIX")
        ?: "https://mycoda.ddns.net/ontologies/MYCODA#"

    val iri = org.semanticweb.owlapi.model.IRI.create("$iriPrefix$cleanShortName")

    val label = ontologyManager.mergedOntology
        .getAnnotationAssertionAxioms(iri)
        .firstOrNull { axiom ->
            axiom.property.isLabel &&
                axiom.value is org.semanticweb.owlapi.model.OWLLiteral
        }
        ?.value as? org.semanticweb.owlapi.model.OWLLiteral

    return label?.literal ?: cleanShortName
}

fun Routing.queryRoutes(ontologyManager: OntologyManager) {
    route("/api") {
        post("/sqwrl") {
            try {
                val originalQueryString = call.receive<PostSqwrlRequest>().queryString
                val queryString = originalQueryString

                println("Received SQWRL query:")
                println(originalQueryString)

                println("Prepared SQWRL query:")
                println(queryString)

                val queryName = "q_" + System.currentTimeMillis()

                val results =
                    ontologyManager.sqwrlQueryEngine.runSQWRLQuery(queryName, queryString)

                val resultsColumn = results.getColumn(0)

                val responseResults =
                    resultsColumn.mapNotNull { result ->
                        when {
                            result.isEntity -> {
                                val entityResult = result.asEntityResult()
                                getEntityDisplayNameByShortName(
    ontologyManager,
    entityResult.shortName
)
                            }

                            else -> result.toString()
                        }
                    }.distinct()

                call.respond(
                    SqwrlQueryResponse(
                        query = queryString,
                        count = responseResults.size,
                        results = responseResults
                    )
                )
            } catch (e: Exception) {
                e.printStackTrace()

                call.respondText(
                    text = e.message ?: "Unexpected error while running SQWRL query",
                    status = HttpStatusCode.InternalServerError
                )
            }
        }
    }
}