package io.github.macodaclub.routes.api

import io.github.macodaclub.models.api.submission.PostSubmitChangesRequest
import io.github.macodaclub.models.api.submission.PostSubmitChangesResponse
import io.github.macodaclub.models.api.submission.PostSubmitFormResponse
import io.github.macodaclub.models.db.ArticleSubmission
import io.github.macodaclub.plugins.EntityFinder
import io.github.macodaclub.plugins.OntologyManager
import io.github.macodaclub.utils.MdTemplate
import io.github.macodaclub.utils.findEntityReferences
import io.github.macodaclub.utils.prettyJson
import io.github.macodaclub.utils.toHtmlTable
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.exposed.sql.transactions.transaction
import org.kohsuke.github.GHRepository

fun Routing.articleSubmissionRoutes(
    ontologyManager: OntologyManager,
    ghRepo: GHRepository,
) {
    route("/api") {
        route("/submission") {
            post("/submitForm") {
                val formParams = call.receiveParameters()
                val title = formParams["title"].orEmpty()
                val abstract = formParams["abstract"].orEmpty()
                val keywords = formParams["keywords"].orEmpty()
                val authors = formParams["authors"].orEmpty()

                val (referencedEntities, textSegments) =
                    listOf(title, abstract, keywords, authors)
                        .findEntityReferences(ontologyManager.entityFinder, ontologyManager.mergedOntology)
                val (titleTextSegments, abstractTextSegments, keywordsTextSegments, authorsTextSegments) = textSegments

                call.respond(
                    PostSubmitFormResponse(
                        referencedEntities,
                        titleTextSegments,
                        abstractTextSegments,
                        keywordsTextSegments,
                        authorsTextSegments
                    )
                )
            }
            post("/submitChanges") {
                val body = call.receive<PostSubmitChangesRequest>()
                val bodyWithoutFormData = body.withoutFormData
                val changesJson = prettyJson.encodeToString(bodyWithoutFormData)
                val addedEntitiesHtmlTables =
                    (if (body.addedEntities.isNotEmpty()) """
## Added Entities
""" else "") +
                            body.addedEntities
                                .groupBy { it["entity"]?.jsonObject?.get("type")?.jsonPrimitive?.content }
                                .map { (type, entity) ->
                                    val typePlural = when (type) {
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

                val editedEntitiesHtmlTables =
                    (if (body.editedEntities.isNotEmpty()) """
## Edited Entities
""" else "") +
                            body.editedEntities.joinToString("") { editedEntity ->
                                """
### <a href="${editedEntity.entity.iri}">${editedEntity.entity.label}</a> (${editedEntity.entity.type})

${editedEntity.edits.toHtmlTable()}
"""
                            }

                val issue = ghRepo
                    .createIssue("Article Submission: ${body.formData.articleTitle}")
                    .label("article submission")
                    .label("ontology change proposal")
                    .label("generated")
                    .body(
                        MdTemplate.SubmitChangesIssue(changesJson, addedEntitiesHtmlTables, editedEntitiesHtmlTables)
                            .get()
                    )
                    .create()
                val formData = body.formData
                transaction {
                    ArticleSubmission.new {
                        articleTitle = formData.articleTitle
                        articleAbstract = formData.articleAbstract
                        articleKeywords = formData.articleKeywords
                        articleAuthors = formData.articleAuthors
                        emailAddress = formData.emailAddress
                        githubIssueUrl = issue.htmlUrl.toString()
                    }
                }
                call.respond(PostSubmitChangesResponse(issue.htmlUrl.toString()))
            }
        }
    }
}