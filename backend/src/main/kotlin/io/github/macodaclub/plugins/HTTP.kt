package io.github.macodaclub.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.compression.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.defaultheaders.*
import io.ktor.server.plugins.httpsredirect.*
import io.ktor.server.plugins.partialcontent.*

fun Application.configureHTTP() {
    install(Compression) {
        gzip {
            priority = 1.0
            if (!this@configureHTTP.developmentMode) {
                breachProtection()
            }
        }
        deflate {
            priority = 10.0
            minimumSize(1024) // condition
            if (!this@configureHTTP.developmentMode) {
                breachProtection()
            }
        }
    }
    install(PartialContent)
    install(DefaultHeaders) {
        header("X-Engine", "Ktor") // will send this header with each response
    }
    install(CORS) {
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Get)
        allowHeader(HttpHeaders.AccessControlAllowOrigin)
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.ContentEncoding)
        if (this@configureHTTP.developmentMode) {
            anyHost()
        }
    }
    if (!this@configureHTTP.developmentMode) {
        val configSslPort =
            this@configureHTTP.environment.config.propertyOrNull("ktor.deployment.sslPort")?.getString()?.toIntOrNull()
        if (configSslPort != null) {
            install(HttpsRedirect) {
                sslPort = configSslPort
                permanentRedirect = true
            }
        }
    }
}

/**
 * Protection against the BREACH attack.
 * https://ktor.io/docs/server-compression.html#security
 * https://en.wikipedia.org/wiki/BREACH
 */
private fun CompressionEncoderBuilder.breachProtection() {
    condition {
        val hostName = application.environment.config.propertyOrNull("ktor.hostName")?.getString()
            ?: error("Ktor configuration error: ktor.hostName is not set.")
        request.headers[HttpHeaders.Referrer]?.startsWith("https://$hostName/") == true
    }
}
