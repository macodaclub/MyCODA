package io.github.macodaclub.plugins

import io.github.macodaclub.utils.contextReceiver
import org.semanticweb.owlapi.apibinding.OWLManager
import org.semanticweb.owlapi.model.IRI
import org.semanticweb.owlapi.model.OWLOntology
import org.semanticweb.owlapi.reasoner.OWLReasoner
import org.semanticweb.owlapi.reasoner.structural.StructuralReasonerFactory
import org.semanticweb.owlapi.util.OWLOntologyMerger
import org.swrlapi.factory.SWRLAPIFactory
import org.swrlapi.sqwrl.SQWRLQueryEngine
import java.io.File

class OntologyManager {
    private val mycodaOntologyUrl = System.getenv("MYCODA_ONTOLOGY_URL")
    private val mycodaOntologyFilePath = System.getenv("MYCODA_ONTOLOGY_FILE_PATH")

    private val lemmatize: String.() -> String = {
        contextReceiver()
    }

    var mycodaOntology: OWLOntology
    var mergedOntology: OWLOntology
    var reasoner: OWLReasoner
    var sqwrlQueryEngine: SQWRLQueryEngine
    var entityFinder: EntityFinder

    init {
        val loadedOntology = loadOntology()

        mycodaOntology = loadedOntology.first
        mergedOntology = loadedOntology.second

        reasoner = StructuralReasonerFactory().createReasoner(mergedOntology)
        sqwrlQueryEngine = createSqwrlQueryEngine(mergedOntology)
        entityFinder = EntityFinder(this, lemmatize)
    }

    private fun loadOntology(): Pair<OWLOntology, OWLOntology> {
        val manager = OWLManager.createOWLOntologyManager()

        val ontology = when {
            mycodaOntologyUrl != null -> {
                manager.loadOntology(IRI.create(mycodaOntologyUrl))
            }

            mycodaOntologyFilePath != null -> {
                manager.loadOntologyFromOntologyDocument(File(mycodaOntologyFilePath))
            }

            else -> {
                error("Please provide environment variable MYCODA_ONTOLOGY_URL or MYCODA_ONTOLOGY_FILE_PATH")
            }
        }

        val merged = OWLOntologyMerger(manager).createMergedOntology(
            manager,
            IRI.create("https://www.merged-ontology.com")
        )

        return Pair(ontology, merged)
    }

    private fun createSqwrlQueryEngine(ontology: OWLOntology): SQWRLQueryEngine {
        val iriResolver = SWRLAPIFactory.createIRIResolver(
            "https://mycoda.ddns.net/ontologies/MYCODA#"
        )

        iriResolver.setPrefix(
            "MYCODA",
            "https://mycoda.ddns.net/ontologies/MYCODA#"
        )

        iriResolver.setPrefix(
            "MYCODA:",
            "https://mycoda.ddns.net/ontologies/MYCODA#"
        )

        iriResolver.setPrefix(
            "",
            "https://mycoda.ddns.net/ontologies/MYCODA#"
        )

        return SWRLAPIFactory.createSQWRLQueryEngine(
            ontology,
            iriResolver
        )
    }

    fun reloadOntology() {
        val loadedOntology = loadOntology()

        mycodaOntology = loadedOntology.first
        mergedOntology = loadedOntology.second

        reasoner = StructuralReasonerFactory().createReasoner(mergedOntology)
        sqwrlQueryEngine = createSqwrlQueryEngine(mergedOntology)
        entityFinder = EntityFinder(this, lemmatize)
    }
}

fun configureOntology() = OntologyManager()