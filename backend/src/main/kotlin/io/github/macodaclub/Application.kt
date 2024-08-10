package io.github.macodaclub

import io.github.macodaclub.plugins.*
import io.github.macodaclub.utils.contextReceiver
import io.ktor.server.application.*

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    val (ontology, mergedOntology, reasoner) = configureOntology()
    val queryEngine = configureSQWRL(mergedOntology)
    val lemmatize: String.() -> String = /* configureCoreNLP() */ { contextReceiver() }
    val entityFinder = configureEntityFinder(mergedOntology, lemmatize)
    val ghRepo = configureGithub()
    configureHTTP()
    configureMonitoring()
    configureSerialization()
    configureRouting(ontology, mergedOntology, reasoner, queryEngine, entityFinder, ghRepo)
}
