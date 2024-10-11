package io.github.macodaclub.routes

import io.github.macodaclub.plugins.EntityFinder
import io.github.macodaclub.plugins.OntologyManager
import io.github.macodaclub.routes.api.*
import io.ktor.server.routing.*
import org.kohsuke.github.GHRepository

fun Routing.apiRoutes(
    ontologyManager: OntologyManager,
    ghRepo: GHRepository,
) {
    queryRoutes(ontologyManager)
    treeRoutes(ontologyManager)
    entityInfoRoutes(ontologyManager)
    ontologyInfoRoutes(ontologyManager)
    articleSubmissionRoutes(ontologyManager, ghRepo)
    editorRoutes(ontologyManager)
    searchRoutes(ontologyManager)
    curatorRoutes(ontologyManager)
}