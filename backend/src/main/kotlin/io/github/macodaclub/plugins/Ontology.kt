package io.github.macodaclub.plugins

import org.semanticweb.owlapi.apibinding.OWLManager
import org.semanticweb.owlapi.model.IRI
import org.semanticweb.owlapi.model.OWLOntology
import org.semanticweb.owlapi.reasoner.OWLReasoner
import org.semanticweb.owlapi.reasoner.structural.StructuralReasonerFactory
import org.semanticweb.owlapi.util.OWLOntologyMerger
import org.swrlapi.factory.SWRLAPIFactory
import org.swrlapi.sqwrl.SQWRLQueryEngine

fun configureOntology(): Triple<OWLOntology, OWLOntology, OWLReasoner> {
    val manager = OWLManager.createOWLOntologyManager()
    val mycodaOntology = manager.loadOntologyFromOntologyDocument(object {}.javaClass.classLoader!!.getResourceAsStream("static/MaCODA.owl")!!)
    //val moodyOntology = manager.loadOntology(IRI.create("http://www.e-lico.eu/ontologies/dmo/DMOP/DMOP.owl"))
    //val optionOntology = manager.loadOntology(IRI.create("https://raw.githubusercontent.com/KostovskaAna/OPTION-Ontology/main/OntoOpt.owl"))
    val mergedOntology = OWLOntologyMerger(manager).createMergedOntology(manager, IRI.create("https://www.merged-ontology.com"))
    val reasoner = StructuralReasonerFactory().createReasoner(mergedOntology)
    return Triple(mycodaOntology, mergedOntology, reasoner)
}

fun configureSQWRL(ontology: OWLOntology): SQWRLQueryEngine {
    return SWRLAPIFactory.createSQWRLQueryEngine(ontology)
}