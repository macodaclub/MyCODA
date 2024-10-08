package io.github.macodaclub.routes

import io.ktor.server.http.content.*
import io.ktor.server.routing.*

fun Routing.staticRoutes() {
    staticResources("/", "website") {
        preCompressed()
        default("index.html")
    }
}