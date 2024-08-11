package io.github.macodaclub.routes

import io.github.macodaclub.plugins.EntityFinder
import io.github.macodaclub.routes.api.*
import io.ktor.server.routing.*
import org.kohsuke.github.GHRepository
import org.semanticweb.owlapi.model.OWLOntology
import org.semanticweb.owlapi.reasoner.OWLReasoner
import org.swrlapi.sqwrl.SQWRLQueryEngine

fun Routing.apiRoutes(
    ontology: OWLOntology,
    mergedOntology: OWLOntology,
    reasoner: OWLReasoner,
    queryEngine: SQWRLQueryEngine,
    entityFinder: EntityFinder,
    ghRepo: GHRepository,
) {
    queryRoutes(queryEngine)
    treeRoutes(mergedOntology, reasoner)
    entityInfoRoutes(mergedOntology, reasoner)
    ontologyInfoRoutes(ontology, mergedOntology)
    articleSubmissionRoutes(mergedOntology, entityFinder, ghRepo)
    editorRoutes(mergedOntology, reasoner, entityFinder)
    searchRoutes(mergedOntology, reasoner, entityFinder)
}