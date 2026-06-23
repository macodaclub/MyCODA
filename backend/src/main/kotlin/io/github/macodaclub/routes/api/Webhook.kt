package io.github.macodaclub.routes.api

import io.github.macodaclub.plugins.OntologyManager
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Routing.webhookRoutes(ontologyManager: OntologyManager) {
    route("/api/webhooks/github") {
        post("/reload-ontology") {
            val providedSecret = call.request.headers["X-Reload-Secret"]
            val expectedSecret = System.getenv("RELOAD_ONTOLOGY_SECRET")
            
            if (expectedSecret.isNullOrBlank() || providedSecret != expectedSecret) {
                call.respond(HttpStatusCode.Unauthorized, "Unauthorized")
                return@post
            }

            ontologyManager.reloadOntology()

            call.respondText("Ontology reloaded successfully!")
        }
    }
}