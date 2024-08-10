package io.github.macodaclub.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Routing.ontologyBrowseRedirectionRoutes() {
    get("/ontologies/{pathIri}") {
        val fullIri = URLBuilder().apply {
            protocol = URLProtocol.createOrDefault(call.request.local.scheme)
            host = call.request.host()
            port = call.request.port()
            path(call.request.path())
        }.buildString()
        call.respondRedirect {
            protocol = URLProtocol.createOrDefault(call.request.local.scheme)
            host = call.request.host()
            port =
                call.application.environment.config.propertyOrNull("ktor.frontendPort")?.getString()?.toIntOrNull()
                    ?: call.request.port()
            path("/browse")
            parameters.append("iri", fullIri)
        }
    }
}