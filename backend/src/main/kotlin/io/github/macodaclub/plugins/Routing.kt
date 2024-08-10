package io.github.macodaclub.plugins

import io.github.macodaclub.routes.apiRoutes
import io.github.macodaclub.routes.ontologyBrowseRedirectionRoutes
import io.github.macodaclub.routes.staticRoutes
import io.ktor.server.application.*
import io.ktor.server.routing.*
import org.kohsuke.github.GHRepository
import org.semanticweb.owlapi.model.OWLOntology
import org.semanticweb.owlapi.reasoner.OWLReasoner
import org.swrlapi.sqwrl.SQWRLQueryEngine


fun Application.configureRouting(
    ontology: OWLOntology,
    mergedOntology: OWLOntology,
    reasoner: OWLReasoner,
    queryEngine: SQWRLQueryEngine,
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
        apiRoutes(ontology, mergedOntology, reasoner, queryEngine, entityFinder, ghRepo)
    }
}