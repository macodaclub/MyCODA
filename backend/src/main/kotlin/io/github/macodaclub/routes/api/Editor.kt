package io.github.macodaclub.routes.api

import io.github.macodaclub.models.editor.GetAutoCompleteIndividualPropertiesResponse
import io.github.macodaclub.plugins.EntityFinder
import io.github.macodaclub.utils.getLabel
import io.github.macodaclub.utils.simpleType
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.semanticweb.owlapi.model.IRI
import org.semanticweb.owlapi.model.OWLOntology
import org.semanticweb.owlapi.reasoner.OWLReasoner
import org.semanticweb.owlapi.search.EntitySearcher

fun Route.editorRoutes(
    mergedOntology: OWLOntology,
    reasoner: OWLReasoner,
    entityFinder: EntityFinder,
) {
    route("editor") {
        get("individualProperties") {
            val classIri = call.request.queryParameters["classIri"]
                ?: return@get call.respond(HttpStatusCode.BadRequest)
            val query = call.request.queryParameters["query"]?.lowercase()
                ?: return@get call.respond(HttpStatusCode.BadRequest)

            val cls =
                mergedOntology.getEntitiesInSignature(IRI.create(classIri)).firstOrNull { it.isOWLClass }?.asOWLClass()
                    ?: return@get call.respond(HttpStatusCode.BadRequest)
            val superClasses = reasoner.getSuperClasses(cls, false)

            val properties = entityFinder.searchEntities(query, "DataProperty").mapNotNull { entity ->
                val property = entity.asOWLDataProperty()
                val domains = EntitySearcher.getDomains(property, mergedOntology)
                if (domains.isNotEmpty() && domains.none { it.asOWLClass() == cls || superClasses.containsEntity(it.asOWLClass()) }) return@mapNotNull null
                // TODO: Support complex domains

                val range = EntitySearcher.getRanges(property, mergedOntology).map { it.asOWLDatatype() }.firstOrNull()
                    ?: return@mapNotNull null
                // TODO: Support complex ranges

                GetAutoCompleteIndividualPropertiesResponse.Property(
                    GetAutoCompleteIndividualPropertiesResponse.Property.Entity(
                        property.iri.toString(),
                        property.getLabel(mergedOntology),
                        property.simpleType ?: return@mapNotNull null
                    ),
                    GetAutoCompleteIndividualPropertiesResponse.Property.Entity(
                        range.iri.toString(),
                        range.getLabel(mergedOntology),
                        range.simpleType ?: return@mapNotNull null
                    ),
                    true
                )
            } + entityFinder.searchEntities(query, "ObjectProperty").mapNotNull { entity ->
                val property = entity.asOWLObjectProperty()
                val domains = EntitySearcher.getDomains(property, mergedOntology)
                if (domains.isNotEmpty() && domains.none { it.asOWLClass() == cls || superClasses.containsEntity(it.asOWLClass()) }) return@mapNotNull null
                // TODO: Support complex domains

                val range = EntitySearcher.getRanges(property, mergedOntology).map { it.asOWLClass() }.firstOrNull()
                    ?: return@mapNotNull null
                // TODO: Support complex ranges

                GetAutoCompleteIndividualPropertiesResponse.Property(
                    GetAutoCompleteIndividualPropertiesResponse.Property.Entity(
                        property.iri.toString(),
                        property.getLabel(mergedOntology),
                        property.simpleType ?: return@mapNotNull null
                    ),
                    GetAutoCompleteIndividualPropertiesResponse.Property.Entity(
                        range.iri.toString(),
                        range.getLabel(mergedOntology),
                        range.simpleType ?: return@mapNotNull null
                    ),
                    false
                )
            }

            call.respond(GetAutoCompleteIndividualPropertiesResponse(properties))
        }
    }
}