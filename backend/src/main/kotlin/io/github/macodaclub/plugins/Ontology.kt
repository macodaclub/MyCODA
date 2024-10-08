package io.github.macodaclub.plugins

import org.semanticweb.owlapi.apibinding.OWLManager
import org.semanticweb.owlapi.model.IRI
import org.semanticweb.owlapi.reasoner.structural.StructuralReasonerFactory
import org.semanticweb.owlapi.util.OWLOntologyMerger
import org.swrlapi.factory.SWRLAPIFactory

class OntologyManager {
    private val mycodaOntologyUrl =
        System.getenv("MYCODA_ONTOLOGY_URL") ?: error("Please provide environment variable MYCODA_ONTOLOGY_URL")
    var manager = OWLManager.createOWLOntologyManager()
    var mycodaOntology = manager.loadOntology(IRI.create(mycodaOntologyUrl))
    var mergedOntology = OWLOntologyMerger(manager).createMergedOntology(manager, IRI.create("https://www.merged-ontology.com"))
    var reasoner = StructuralReasonerFactory().createReasoner(mergedOntology)
    var sqwrlQueryEngine = SWRLAPIFactory.createSQWRLQueryEngine(mergedOntology)

    fun reloadOntology() {
        val manager = OWLManager.createOWLOntologyManager()
        mycodaOntology = manager.loadOntology(IRI.create(mycodaOntologyUrl))
        mergedOntology =
            OWLOntologyMerger(manager).createMergedOntology(manager, IRI.create("https://www.merged-ontology.com"))
        reasoner = StructuralReasonerFactory().createReasoner(mergedOntology)
        sqwrlQueryEngine = SWRLAPIFactory.createSQWRLQueryEngine(mergedOntology)
    }
}

fun configureOntology() = OntologyManager()