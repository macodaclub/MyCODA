package io.github.macodaclub.plugins

import io.github.macodaclub.utils.contextReceiver
import org.semanticweb.owlapi.apibinding.OWLManager
import org.semanticweb.owlapi.model.IRI
import org.semanticweb.owlapi.model.OWLOntology
import org.semanticweb.owlapi.model.OWLOntologyManager
import org.semanticweb.owlapi.reasoner.OWLReasoner
import org.semanticweb.owlapi.reasoner.structural.StructuralReasonerFactory
import org.semanticweb.owlapi.util.OWLOntologyMerger
import org.swrlapi.factory.SWRLAPIFactory
import org.swrlapi.sqwrl.SQWRLQueryEngine
import java.io.File

class OntologyManager {
    private val mycodaOntologyUrl = System.getenv("MYCODA_ONTOLOGY_URL")
    private val mycodaOntologyFilePath = System.getenv("MYCODA_ONTOLOGY_FILE_PATH")
    private val lemmatize: String.() -> String = /* configureCoreNLP() */ { contextReceiver() }
    var mycodaOntology: OWLOntology
    var mergedOntology: OWLOntology
    var reasoner: OWLReasoner
    var sqwrlQueryEngine: SQWRLQueryEngine
    var entityFinder: EntityFinder

    init {
        val manager = OWLManager.createOWLOntologyManager()
        mycodaOntology = if(mycodaOntologyUrl != null) {
            manager.loadOntology(IRI.create(mycodaOntologyUrl))
        } else {
            if(mycodaOntologyFilePath != null) {
                manager.loadOntologyFromOntologyDocument(File(mycodaOntologyFilePath))
            } else {
                error("Please provide environment variable MYCODA_ONTOLOGY_URL or MYCODA_ONTOLOGY_FILE_PATH")
            }
        }
        mergedOntology =
            OWLOntologyMerger(manager).createMergedOntology(manager, IRI.create("https://www.merged-ontology.com"))
        reasoner = StructuralReasonerFactory().createReasoner(mergedOntology)
        sqwrlQueryEngine = SWRLAPIFactory.createSQWRLQueryEngine(mergedOntology)
        entityFinder = EntityFinder(this, lemmatize)
    }

    fun reloadOntology() {
        val manager = OWLManager.createOWLOntologyManager()
        mycodaOntology = if(mycodaOntologyUrl != null) {
            manager.loadOntology(IRI.create(mycodaOntologyUrl))
        } else {
            if(mycodaOntologyFilePath != null) {
                manager.loadOntologyFromOntologyDocument(File(mycodaOntologyFilePath))
            } else {
                error("Please provide environment variable MYCODA_ONTOLOGY_URL or MYCODA_ONTOLOGY_FILE_PATH")
            }
        }
        mergedOntology =
            OWLOntologyMerger(manager).createMergedOntology(manager, IRI.create("https://www.merged-ontology.com"))
        reasoner = StructuralReasonerFactory().createReasoner(mergedOntology)
        sqwrlQueryEngine = SWRLAPIFactory.createSQWRLQueryEngine(mergedOntology)
        entityFinder = EntityFinder(this, lemmatize)
    }
}

fun configureOntology() = OntologyManager()