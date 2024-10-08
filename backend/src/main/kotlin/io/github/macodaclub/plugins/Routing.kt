package io.github.macodaclub.plugins

import io.github.macodaclub.routes.apiRoutes
import io.github.macodaclub.routes.ontologyBrowseRedirectionRoutes
import io.github.macodaclub.routes.staticRoutes
import io.ktor.server.application.*
import io.ktor.server.routing.*
import org.kohsuke.github.GHRepository


fun Application.configureRouting(
    ontologyManager: OntologyManager,
    entityFinder: EntityFinder,
    ghRepo: GHRepository,
) {
    /*install(StatusPages) {
        exception<Throwable> { call, cause ->
            call.respondText(text = "500: $cause", status = HttpStatusCode.InternalServerError)
        }
    }*/
    routing {
        staticRoutes()
        ontologyBrowseRedirectionRoutes()
        apiRoutes(ontologyManager, entityFinder, ghRepo)
    }
}