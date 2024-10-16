package io.github.macodaclub.routes.api

import io.github.macodaclub.models.api.submission.*
import io.github.macodaclub.models.db.ArticleSubmission
import io.github.macodaclub.models.db.ArticleSubmissions
import io.github.macodaclub.plugins.OntologyManager
import io.github.macodaclub.utils.MdTemplate
import io.github.macodaclub.utils.findEntityReferences
import io.github.macodaclub.utils.prettyJson
import io.github.macodaclub.utils.toHtmlTable
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
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
                val submission = transaction {
                    ArticleSubmission.new {
                        articleTitle = formData.articleTitle
                        articleAbstract = formData.articleAbstract
                        articleKeywords = formData.articleKeywords
                        articleAuthors = formData.articleAuthors
                        emailAddress = formData.emailAddress
                        githubIssueUrl = issue.htmlUrl.toString()
                    }
                }
                call.respond(PostSubmitChangesResponse(submission.id.value, issue.htmlUrl.toString()))
            }
            post("/submitSus") {
                val body = call.receive<PostSubmitSusRequest>()
                val submissionId = body.submissionId
                val answers = body.answers
                val submission = transaction {
                    ArticleSubmission.findByIdAndUpdate(submissionId) {
                        it.susAnswer1 = answers[0]
                        it.susAnswer2 = answers[1]
                        it.susAnswer3 = answers[2]
                        it.susAnswer4 = answers[3]
                        it.susAnswer5 = answers[4]
                        it.susAnswer6 = answers[5]
                        it.susAnswer7 = answers[6]
                        it.susAnswer8 = answers[7]
                        it.susAnswer9 = answers[8]
                        it.susAnswer10 = answers[9]
                    }
                }
                if(submission == null) return@post call.respond(HttpStatusCode.BadRequest)
                call.respond(HttpStatusCode.OK, submission.id.value)
            }
            post("/submitFeedback") {
                val body = call.receive<PostSubmitFeedbackRequest>()
                val submissionId = body.submissionId
                val submission = transaction {
                    ArticleSubmission.findByIdAndUpdate(submissionId) {
                        it.feedback = body.feedback
                    }
                }
                if(submission == null) return@post call.respond(HttpStatusCode.BadRequest)
                call.respond(HttpStatusCode.OK, submission.id.value)
            }
        }
    }
}