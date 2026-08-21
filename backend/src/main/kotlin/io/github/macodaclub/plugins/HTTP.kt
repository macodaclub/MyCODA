package io.github.macodaclub.plugins

import io.github.macodaclub.config.AppEnvironment
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.plugins.cachingheaders.*
import io.ktor.server.plugins.compression.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.defaultheaders.*
import io.ktor.server.plugins.httpsredirect.*
import io.ktor.server.plugins.partialcontent.*
import io.ktor.server.request.*

fun Application.configureHTTP() {

    val appEnvironment = AppEnvironment.current()

    install(Compression) {
        gzip {
            priority = 1.0

            if (appEnvironment == AppEnvironment.PRD) {
                breachProtection()
            }
        }

        deflate {
            priority = 10.0
            minimumSize(1024)

            if (appEnvironment == AppEnvironment.PRD) {
                breachProtection()
            }
        }
    }

    install(PartialContent)

    install(DefaultHeaders) {
        header("X-Engine", "Ktor")
    }

    install(CachingHeaders) {
        options { call, content ->
            when (call.request.httpMethod) {
                HttpMethod.Get -> {
                    when (content.contentType?.withoutParameters()) {
                        ContentType.Application.Json ->
                            CachingOptions(
                                CacheControl.MaxAge(maxAgeSeconds = 300)
                            )

                        else ->
                            CachingOptions(
                                CacheControl.MaxAge(maxAgeSeconds = 3600)
                            )
                    }
                }

                else -> null
            }
        }
    }

    install(CORS) {
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Get)

        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.ContentEncoding)

        if (appEnvironment == AppEnvironment.DEV) {
            allowHost(
                "localhost:3000",
                schemes = listOf("http")
            )

            allowHost(
                "127.0.0.1:3000",
                schemes = listOf("http")
            )
        }

        if (appEnvironment == AppEnvironment.PRD) {
            allowHost(
                "mycoda.iscte-iul.pt",
                schemes = listOf("https")
            )
        }
    }

    if (appEnvironment == AppEnvironment.PRD) {
        val configSslPort =
            this@configureHTTP.environment.config
                .propertyOrNull("ktor.deployment.sslPort")
                ?.getString()
                ?.toIntOrNull()

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
        val hostName =
            application.environment.config
                .propertyOrNull("ktor.hostName")
                ?.getString()
                ?: error(
                    "Ktor configuration error: ktor.hostName is not set."
                )

        request.headers[HttpHeaders.Referrer]
            ?.startsWith("https://$hostName/") == true
    }
}