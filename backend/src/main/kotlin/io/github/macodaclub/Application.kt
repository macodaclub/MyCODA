package io.github.macodaclub

import io.github.macodaclub.config.AppEnvironment
import io.github.macodaclub.plugins.*
import io.ktor.server.application.*

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    val appEnvironment = AppEnvironment.current()

    val ontologyManager = configureOntology()

    configureHTTP()
    configureMonitoring()
    configureSerialization()

    if (appEnvironment == AppEnvironment.PRD) {
        configureDatabase()
        configureCuratorAuthentication()

        val ghRepo = configureGithub()

        configureRouting(
            ontologyManager = ontologyManager,
            ghRepo = ghRepo
        )
    } else {
        configureRouting(
            ontologyManager = ontologyManager
        )
    }
}