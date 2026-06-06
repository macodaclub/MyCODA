package io.github.macodaclub.plugins

import io.github.macodaclub.routes.apiRoutes
import io.github.macodaclub.routes.ontologyBrowseRedirectionRoutes
import io.github.macodaclub.routes.staticRoutes
import io.ktor.server.application.*
import io.ktor.server.routing.*
import org.kohsuke.github.GHRepository

fun Application.configureRouting(
    ontologyManager: OntologyManager,
    ghRepo: GHRepository,
) {
    routing {
        staticRoutes()
        ontologyBrowseRedirectionRoutes()
        apiRoutes(ontologyManager, ghRepo)
    }
}