package io.github.macodaclub.routes.api

import io.github.macodaclub.models.submission.PostSubmitFormResponse
import io.github.macodaclub.plugins.EntityFinder
import io.github.macodaclub.utils.findEntityReferences
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.kohsuke.github.GHRepository
import org.semanticweb.owlapi.model.OWLOntology

fun Route.articleSubmissionRoutes(
    mergedOntology: OWLOntology,
    entityFinder: EntityFinder,
    ghRepo: GHRepository,
) {
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
            val issue = ghRepo
                .createIssue("Test – Submission Form: Ontology Changes")
                .label("article submission form")
                .label("generated")
                .label("ontology changes")
                .label("ontology change proposal")
                .body("<Changes Listed Here>")
                .create()
            call.respondText { issue.url.toString() }
        }
    }
}