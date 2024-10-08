package io.github.macodaclub.routes.api

import io.github.macodaclub.plugins.OntologyManager
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Routing.curatorRoutes(ontologyManager: OntologyManager) {
    route("/api") {
        authenticate("auth-curator") {
            route("/curator") {
                post("/reloadOntology") {
                    ontologyManager.reloadOntology()
                    call.respondText("Ontology reloaded successfully!")
                }
            }
        }
    }
}