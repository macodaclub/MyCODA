package io.github.macodaclub.routes.api

import io.github.macodaclub.models.api.query.PostSqwrlRequest
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.swrlapi.sqwrl.SQWRLQueryEngine

fun Routing.queryRoutes(queryEngine: SQWRLQueryEngine) {
    route("/api") {
        /*get("/sqwrl") {
            // TODO: Take care of SQWRL queries with axioms starting with a number (e.g. 2p-NSGA-II) and featuring / (e.g. iMOEA/D)
            val results =
                queryEngine.runSQWRLQuery(
                    "q1",
                    "MetaHeuristic(?_MetaHeuristic_) ^ hasDevelopingYear(?_MetaHeuristic_, ?_hasDevelopingYear_) ^ swrlb:greaterThanOrEqual(?_hasDevelopingYear_, 2000) -> sqwrl:select(?_MetaHeuristic_) ^ sqwrl:select(?_hasDevelopingYear_) ^ sqwrl:orderBy(?_hasDevelopingYear_)"
                )
            val resultsColumn = results.getColumn(0)
            call.respondText(resultsColumn.toString())
        }*/
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