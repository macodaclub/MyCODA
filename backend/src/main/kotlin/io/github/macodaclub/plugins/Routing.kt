package io.github.macodaclub.plugins

import io.github.macodaclub.models.PostSqwrlRequest
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.semanticweb.owlapi.model.OWLOntology
import org.swrlapi.sqwrl.SQWRLQueryEngine


fun Application.configureRouting(ontology: OWLOntology, queryEngine: SQWRLQueryEngine) {
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            call.respondText(text = "500: $cause", status = HttpStatusCode.InternalServerError)
        }
        status(HttpStatusCode.NotFound) { call, status ->
            if(call.request.uri.substringAfter("/").startsWith("frontend")) {
                call.respondRedirect("/frontend")
            } else {
                call.respondRedirect("/")
            }
        }
    }
    // install(Resources)
    routing {
        staticResources("/", "website")
        staticResources("/frontend", "frontend")
        staticResources("/static", "static")
        get("/sqwrl") {
            // TODO: Take care of SQWRL queries with axioms starting with a number (e.g. 2p-NSGA-II) and featuring / (e.g. iMOEA/D)
            val results =
                queryEngine.runSQWRLQuery(
                    "q1",
                    "MetaHeuristic(?_MetaHeuristic_) ^ hasDevelopingYear(?_MetaHeuristic_, ?_hasDevelopingYear_) ^ swrlb:greaterThanOrEqual(?_hasDevelopingYear_, 2000) -> sqwrl:select(?_MetaHeuristic_) ^ sqwrl:select(?_hasDevelopingYear_) ^ sqwrl:orderBy(?_hasDevelopingYear_)"
                )
            val resultsColumn = results.getColumn(0)
            call.respondText(resultsColumn.toString())
        }
        post("/sqwrl") {
            val queryString = call.receive<PostSqwrlRequest>().queryString
            // TODO: Take care of SQWRL queries with axioms starting with a number (e.g. 2p-NSGA-II) and featuring / (e.g. iMOEA/D)
            val results =
                queryEngine.runSQWRLQuery("q1", queryString)
            val resultsColumn = results.getColumn(0)
            call.respond<List<String>>(
                resultsColumn.mapNotNull { result ->
                    when {
                        result.isEntity -> {
                            val entityResult = result.asEntityResult()
                            entityResult.shortName.substringAfter(":")
                        }
                        // TODO: Support non-entity results
                        else -> null
                    }
                }.distinct()
            )
        }
    }
}