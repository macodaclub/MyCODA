package io.github.macodaclub.plugins

import io.ktor.server.application.*
import org.semanticweb.owlapi.apibinding.OWLManager
import org.semanticweb.owlapi.model.OWLOntology
import org.swrlapi.factory.SWRLAPIFactory
import org.swrlapi.sqwrl.SQWRLQueryEngine

fun Application.configureOntology(): OWLOntology {
    val manager = OWLManager.createOWLOntologyManager()
    val ontology =
        manager.loadOntologyFromOntologyDocument(this.javaClass.classLoader!!.getResourceAsStream("static/MaCODA.owl")!!)
    return ontology
}

fun configureSQWRL(ontology: OWLOntology): SQWRLQueryEngine {
    return SWRLAPIFactory.createSQWRLQueryEngine(ontology)
}