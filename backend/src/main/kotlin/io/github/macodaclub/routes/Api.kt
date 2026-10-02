package io.github.macodaclub.routes

import io.github.macodaclub.config.AppEnvironment
import io.github.macodaclub.plugins.OntologyManager
import io.github.macodaclub.routes.api.*
import io.ktor.server.routing.*
import org.kohsuke.github.GHRepository

fun Routing.apiRoutes(
    ontologyManager: OntologyManager,
    ghRepo: GHRepository? = null
) {
    val appEnvironment = AppEnvironment.current()

    queryRoutes(ontologyManager)
    treeRoutes(ontologyManager)
    entityInfoRoutes(ontologyManager)
    ontologyInfoRoutes(ontologyManager)
    editorRoutes(ontologyManager)
    searchRoutes(ontologyManager)

    datasetRoutes()
    datasetVocabularyRoutes()

    oplDatasetRoutes()
    hybridDatasetRoutes()
    ontologyGraphRoutes(ontologyManager)

    if (appEnvironment == AppEnvironment.PRD) {
        articleSubmissionRoutes(ontologyManager, ghRepo!!)
        curatorRoutes(ontologyManager)
    }
}