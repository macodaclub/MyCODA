package io.github.macodaclub

import io.github.macodaclub.plugins.*
import io.github.macodaclub.utils.contextReceiver
import io.ktor.server.application.*

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    val ontologyManager = configureOntology()
    val ghRepo = configureGithub()
    configureHTTP()
    configureMonitoring()
    configureSerialization()
    configureDatabase()
    configureCuratorAuthentication()
    configureRouting(ontologyManager, ghRepo)
}
