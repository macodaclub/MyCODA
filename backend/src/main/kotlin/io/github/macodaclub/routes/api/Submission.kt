package io.github.macodaclub.routes.api

import io.github.macodaclub.models.submission.PostSubmitChangesRequest
import io.github.macodaclub.models.submission.PostSubmitChangesResponse
import io.github.macodaclub.models.submission.PostSubmitFormResponse
import io.github.macodaclub.plugins.EntityFinder
import io.github.macodaclub.utils.MdTemplate
import io.github.macodaclub.utils.findEntityReferences
import io.github.macodaclub.utils.toHtmlTable
import io.github.macodaclub.utils.prettyJson
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.kohsuke.github.GHRepository
import org.semanticweb.owlapi.model.OWLOntology

fun Routing.articleSubmissionRoutes(
    mergedOntology: OWLOntology,
    entityFinder: EntityFinder,
    ghRepo: GHRepository,
) {
    route("/api") {
        route("submission") {
            post("submitForm") {
                val formParams = call.receiveParameters()
                val title = formParams["title"].orEmpty()
                val abstract = formParams["abstract"].orEmpty()
                val keywords = formParams["keywords"].orEmpty()

                val (referencedEntities, textSegments) =
                    listOf(title, abstract, keywords)
                        .findEntityReferences(entityFinder, mergedOntology)
                val (titleTextSegments, abstractTextSegments, keywordsTextSegments) = textSegments

                call.respond(
                    PostSubmitFormResponse(
                        referencedEntities,
                        titleTextSegments,
                        abstractTextSegments,
                        keywordsTextSegments,
                    )
                )
            }
            post("submitChanges") {
                val changes = call.receive<PostSubmitChangesRequest>()
                val changesJson = prettyJson.encodeToString(changes)
                val addedEntitiesHtmlTable = changes.addedEntities
                    .groupBy { it["entity"]?.jsonObject?.get("type")?.jsonPrimitive?.content }
                    .map { (type, entity) ->
                        val typePlural = when(type) {
                            "Class" -> "Classes"
                            "Individual" -> "Individuals"
                            "Property" -> "Properties"
                            else -> type
                        }
                        """
### $typePlural
${entity.toHtmlTable()}
"""
                    }.joinToString("\n")

                val issue = ghRepo
                    .createIssue("Article Submission")
                    .label("article submission")
                    .label("ontology change proposal")
                    .label("generated")
                    .body(MdTemplate.SubmitChangesIssue(changesJson, addedEntitiesHtmlTable).get())
                    .create()
                call.respond(PostSubmitChangesResponse(issue.htmlUrl.toString()))
            }
        }
    }
}