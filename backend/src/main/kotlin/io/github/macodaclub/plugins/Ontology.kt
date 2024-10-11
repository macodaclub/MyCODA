package io.github.macodaclub.plugins

import io.github.macodaclub.utils.contextReceiver
import org.semanticweb.owlapi.apibinding.OWLManager
import org.semanticweb.owlapi.model.IRI
import org.semanticweb.owlapi.model.OWLOntology
import org.semanticweb.owlapi.model.OWLOntologyManager
import org.semanticweb.owlapi.reasoner.structural.StructuralReasonerFactory
import org.semanticweb.owlapi.util.OWLOntologyMerger
import org.swrlapi.factory.SWRLAPIFactory

class OntologyManager {
    private val mycodaOntologyUrl =
        System.getenv("MYCODA_ONTOLOGY_URL") ?: error("Please provide environment variable MYCODA_ONTOLOGY_URL")
    private var manager: OWLOntologyManager = OWLManager.createOWLOntologyManager()
    var mycodaOntology = manager.loadOntology(IRI.create(mycodaOntologyUrl))
    var mergedOntology: OWLOntology = OWLOntologyMerger(manager).createMergedOntology(manager, IRI.create("https://www.merged-ontology.com"))
    var reasoner = StructuralReasonerFactory().createReasoner(mergedOntology)
    var sqwrlQueryEngine = SWRLAPIFactory.createSQWRLQueryEngine(mergedOntology)
    private val lemmatize: String.() -> String = /* configureCoreNLP() */ { contextReceiver() }
    var entityFinder = EntityFinder(this, lemmatize)

    fun reloadOntology() {
        val manager = OWLManager.createOWLOntologyManager()
        mycodaOntology = manager.loadOntology(IRI.create(mycodaOntologyUrl))
        mergedOntology =
            OWLOntologyMerger(manager).createMergedOntology(manager, IRI.create("https://www.merged-ontology.com"))
        reasoner = StructuralReasonerFactory().createReasoner(mergedOntology)
        sqwrlQueryEngine = SWRLAPIFactory.createSQWRLQueryEngine(mergedOntology)
        entityFinder = EntityFinder(this, lemmatize)
    }
}

fun configureOntology() = OntologyManager()