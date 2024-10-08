package io.github.macodaclub.routes.api

import io.github.macodaclub.models.api.editor.GetEditorIndividualPropertiesResponse
import io.github.macodaclub.models.api.editor.GetSynonymSuggestionsResponse
import io.github.macodaclub.plugins.EntityFinder
import io.github.macodaclub.plugins.OntologyManager
import io.github.macodaclub.utils.getLabel
import io.github.macodaclub.utils.simpleType
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.semanticweb.owlapi.model.IRI
import org.semanticweb.owlapi.search.EntitySearcher

fun Routing.editorRoutes(
    ontologyManager: OntologyManager,
    entityFinder: EntityFinder,
) {
    route("/api") {
        route("/editor") {
            get("/individualProperties") {
                val classIri = call.request.queryParameters["classIri"]
                val query = call.request.queryParameters["query"]?.lowercase()
                    ?: return@get call.respond(HttpStatusCode.BadRequest)

                val cls = classIri?.let {
                    ontologyManager.mergedOntology.getEntitiesInSignature(IRI.create(classIri)).firstOrNull { it.isOWLClass }
                        ?.asOWLClass()
                }
                val superClasses = cls?.let { ontologyManager.reasoner.getSuperClasses(cls, false) }

                val properties = entityFinder.searchEntities(query, "DataProperty").mapNotNull { entity ->
                    val property = entity.asOWLDataProperty()
                    val domains = EntitySearcher.getDomains(property, ontologyManager.mergedOntology)
                    if (domains.isNotEmpty() && domains.none {
                            it.asOWLClass() == cls || superClasses !== null && superClasses.containsEntity(
                                it.asOWLClass()
                            )
                        }) return@mapNotNull null
                    // TODO: Support complex domains

                    val range =
                        EntitySearcher.getRanges(property, ontologyManager.mergedOntology).map { it.asOWLDatatype() }.firstOrNull()
                            ?: return@mapNotNull null
                    // TODO: Support complex ranges

                    GetEditorIndividualPropertiesResponse.Property(
                        GetEditorIndividualPropertiesResponse.Property.Entity(
                            property.iri.toString(),
                            property.getLabel(ontologyManager.mergedOntology),
                            property.simpleType ?: return@mapNotNull null
                        ),
                        GetEditorIndividualPropertiesResponse.Property.Entity(
                            range.iri.toString(),
                            range.getLabel(ontologyManager.mergedOntology),
                            range.simpleType ?: return@mapNotNull null
                        ),
                    )
                } + entityFinder.searchEntities(query, "ObjectProperty").mapNotNull { entity ->
                    val property = entity.asOWLObjectProperty()
                    val domains = EntitySearcher.getDomains(property, ontologyManager.mergedOntology)
                    if (domains.isNotEmpty() && domains.none {
                            it.asOWLClass() == cls || superClasses != null && superClasses.containsEntity(
                                it.asOWLClass()
                            )
                        }) return@mapNotNull null
                    // TODO: Support complex domains

                    val range = EntitySearcher.getRanges(property, ontologyManager.mergedOntology).map { it.asOWLClass() }.firstOrNull()
                        ?: return@mapNotNull null
                    // TODO: Support complex ranges

                    GetEditorIndividualPropertiesResponse.Property(
                        GetEditorIndividualPropertiesResponse.Property.Entity(
                            property.iri.toString(),
                            property.getLabel(ontologyManager.mergedOntology),
                            property.simpleType ?: return@mapNotNull null
                        ),
                        GetEditorIndividualPropertiesResponse.Property.Entity(
                            range.iri.toString(),
                            range.getLabel(ontologyManager.mergedOntology),
                            range.simpleType ?: return@mapNotNull null
                        ),
                    )
                }

                call.respond(GetEditorIndividualPropertiesResponse(properties))
            }
            get("/synonymSuggestions") {
                val query = call.request.queryParameters["query"]?.lowercase()
                    ?: return@get call.respond(HttpStatusCode.BadRequest)

                val suggestions = entityFinder.findSynonymSuggestions(query)
                call.respond(
                    GetSynonymSuggestionsResponse(
                        suggestions.mapNotNull {
                            GetSynonymSuggestionsResponse.Entity(
                                it.iri.toString(),
                                it.getLabel(ontologyManager.mergedOntology),
                                it.simpleType ?: return@mapNotNull null
                            )
                        }
                    )
                )
            }
        }
    }
}