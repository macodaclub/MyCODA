package io.github.macodaclub

import io.github.macodaclub.plugins.*
import io.ktor.server.application.*

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    val ontology = configureOntology()
    val queryEngine = configureSQWRL(ontology)
    configureSession()
    configureHTTP()
    configureMonitoring()
    configureSerialization()
    configureDatabases()
    configureRouting(ontology, queryEngine)
}
