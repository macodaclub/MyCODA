package io.github.macodaclub.plugins

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.util.*

fun Application.configureCuratorAuthentication() {
    val curatorPassword =
        System.getenv("CURATOR_PASSWORD") ?: error("Please provide environment variable CURATOR_PASSWORD")
    val digestFunction = getDigestFunction("SHA-256") { "mycoda${it.length}" }
    val hashedUserTable = UserHashedTableAuth(
        table = mapOf("curator" to digestFunction(curatorPassword)),
        digester = digestFunction
    )

    install(Authentication) {
        basic("auth-curator") {
            realm = "Access to Knowledge Curator tools"
            validate { credentials ->
                hashedUserTable.authenticate(credentials)
            }
        }
    }
}