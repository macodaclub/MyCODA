package io.github.macodaclub.routes.api

import io.github.macodaclub.models.api.tree.GetTreeResponse
import io.github.macodaclub.utils.toTaxonomyTreeEntity
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.semanticweb.owlapi.model.IRI
import org.semanticweb.owlapi.model.OWLOntology
import org.semanticweb.owlapi.reasoner.OWLReasoner

fun Routing.treeRoutes(
    mergedOntology: OWLOntology,
    reasoner: OWLReasoner,
) {
    route("/api") {
        route("/tree") {
            get {
                val type = call.request.queryParameters["type"] ?: "Class"
                when (type) {
                    "Class" -> {
                        call.respond(
                            GetTreeResponse(
                                GetTreeResponse.TaxonomyTree(
                                    mergedOntology.classesInSignature
                                        .filter {
                                            !it.isTopEntity && reasoner.getSuperClasses(it, true)
                                                .firstOrNull()?.isTopNode ?: true
                                        }
                                        .map { cls -> cls.toTaxonomyTreeEntity(mergedOntology, reasoner) }
                                ),
                                type
                            )
                        )
                    }

                    "Property" -> {
                        val dp = mergedOntology.dataPropertiesInSignature
                            .filter {
                                !it.isTopEntity && reasoner.getSuperDataProperties(it, true)
                                    .firstOrNull()?.isTopNode ?: true
                            }
                            .map { dp -> dp.toTaxonomyTreeEntity(mergedOntology, reasoner) }
                        val op = mergedOntology.objectPropertiesInSignature
                            .filter {
                                !it.isTopEntity && reasoner.getSuperObjectProperties(it, true)
                                    .firstOrNull()?.isTopNode ?: true
                            }
                            .map { op -> op.toTaxonomyTreeEntity(mergedOntology, reasoner) }
                        call.respond(
                            GetTreeResponse(GetTreeResponse.TaxonomyTree(dp + op), type)
                        )
                    }

                    "Individual" -> {
                        call.respond(
                            GetTreeResponse(
                                GetTreeResponse.TaxonomyTree(
                                    mergedOntology.individualsInSignature
                                        .map { ind -> ind.toTaxonomyTreeEntity(mergedOntology) }
                                ),
                                type
                            )
                        )
                    }
                }
            }
            get("/select") {
                val iri = call.request.queryParameters["iri"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                val type = call.request.queryParameters["type"] ?: run {
                    val entity = mergedOntology.getEntitiesInSignature(IRI.create(iri)).firstOrNull()
                        ?: return@get call.respond(HttpStatusCode.BadRequest)
                    when {
                        entity.isOWLClass -> "Class"
                        entity.isOWLDataProperty || entity.isOWLObjectProperty -> "Property"
                        entity.isOWLNamedIndividual -> "Individual"
                        else -> return@get call.respond(HttpStatusCode.BadRequest)
                    }
                }
                when (type) {
                    "Class" -> {
                        val cls = mergedOntology.getEntitiesInSignature(IRI.create(iri))
                            .firstOrNull { it.isOWLClass }?.asOWLClass()
                            ?: return@get call.respond(HttpStatusCode.BadRequest)
                        val superClasses = reasoner.getSuperClasses(cls, false)
                        val leaf = cls.toTaxonomyTreeEntity(mergedOntology, reasoner)
                        val root = superClasses.fold(leaf) { acc, node ->
                            val superClass = node.representativeElement
                            if (superClass.isTopEntity) return@fold acc
                            superClass.toTaxonomyTreeEntity(mergedOntology, reasoner) {
                                listOf(acc)
                            }
                        }
                        call.respond(GetTreeResponse(GetTreeResponse.TaxonomyTree(listOf(root)), type))
                    }

                    "Property" -> {
                        val entity = mergedOntology.getEntitiesInSignature(IRI.create(iri))
                            .firstOrNull { it.isOWLDataProperty || it.isOWLObjectProperty }
                            ?: return@get call.respond(HttpStatusCode.BadRequest)
                        when {
                            entity.isOWLDataProperty -> {
                                val property = entity.asOWLDataProperty()
                                val superProperties = reasoner.getSuperDataProperties(property, false)
                                val leaf = property.toTaxonomyTreeEntity(mergedOntology, reasoner)
                                val root = superProperties.fold(leaf) { acc, node ->
                                    val superProperty =
                                        node.entities.firstOrNull { !it.isBottomEntity && !it.isTopEntity && !it.isAnonymous }
                                            ?.asOWLDataProperty() ?: return@fold acc
                                    // TODO: handle inverseOf and equivalents
                                    superProperty.toTaxonomyTreeEntity(mergedOntology, reasoner) {
                                        listOf(acc)
                                    }
                                }
                                call.respond(GetTreeResponse(GetTreeResponse.TaxonomyTree(listOf(root)), type))
                            }

                            entity.isOWLObjectProperty -> {
                                val property = entity.asOWLObjectProperty()
                                val superProperties = reasoner.getSuperObjectProperties(property, false)
                                val leaf = property.toTaxonomyTreeEntity(mergedOntology, reasoner)
                                val root = superProperties.fold(leaf) { acc, node ->
                                    val superProperty =
                                        node.entities.firstOrNull { !it.isBottomEntity && !it.isTopEntity && !it.isAnonymous }
                                            ?.asOWLObjectProperty() ?: return@fold acc
                                    // TODO: handle inverseOf and equivalents
                                    superProperty.toTaxonomyTreeEntity(mergedOntology, reasoner) {
                                        listOf(acc)
                                    }
                                }
                                call.respond(GetTreeResponse(GetTreeResponse.TaxonomyTree(listOf(root)), type))
                            }

                            else -> return@get call.respond(HttpStatusCode.BadRequest)
                        }
                    }

                    "Individual" -> {
                        val individual = mergedOntology.getEntitiesInSignature(IRI.create(iri))
                            .firstOrNull { it.isOWLNamedIndividual }
                            ?.asOWLNamedIndividual()
                            ?: return@get call.respond(HttpStatusCode.BadRequest)
                        val leaf = individual.toTaxonomyTreeEntity(mergedOntology)
                        call.respond(GetTreeResponse(GetTreeResponse.TaxonomyTree(listOf(leaf)), type))
                    }
                }
            }
            get("/expand") {
                val iri = call.request.queryParameters["iri"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                val type =
                    call.request.queryParameters["type"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                when (type) {
                    "Class" -> {
                        val cls =
                            mergedOntology.getEntitiesInSignature(IRI.create(iri))
                                .firstOrNull { it.isOWLClass }
                                ?.asOWLClass()
                                ?: return@get call.respond(HttpStatusCode.BadRequest)
                        val children = reasoner.getSubClasses(cls, true)
                            .filterNot { it.isBottomNode }
                            .map { it.representativeElement.toTaxonomyTreeEntity(mergedOntology, reasoner) }
                        call.respond(GetTreeResponse(GetTreeResponse.TaxonomyTree(children), type))
                    }

                    "Property" -> {
                        val property =
                            mergedOntology.getEntitiesInSignature(IRI.create(iri))
                                .firstOrNull { it.isOWLDataProperty || it.isOWLObjectProperty }
                                ?: return@get call.respond(HttpStatusCode.BadRequest)
                        val children = when {
                            property.isOWLDataProperty -> {
                                reasoner.getSubDataProperties(property.asOWLDataProperty(), true)
                                    .filterNot { it.isBottomNode }
                                    .mapNotNull { node ->
                                        node.entities.firstOrNull {
                                            !it.isBottomEntity && !it.isTopEntity && !it.isAnonymous
                                        }
                                            ?.asOWLDataProperty() // TODO: handle inverseOf and equivalents
                                            ?.toTaxonomyTreeEntity(mergedOntology, reasoner)
                                    }
                            }

                            property.isOWLObjectProperty -> {
                                reasoner.getSubObjectProperties(property.asOWLObjectProperty(), true)
                                    .filterNot { it.isBottomNode }
                                    .mapNotNull { node ->
                                        node.entities.firstOrNull {
                                            !it.isBottomEntity && !it.isTopEntity && !it.isAnonymous
                                        }
                                            ?.asOWLObjectProperty() // TODO: handle inverseOf and equivalents
                                            ?.toTaxonomyTreeEntity(mergedOntology, reasoner)
                                    }
                            }

                            else -> return@get call.respond(HttpStatusCode.BadRequest)
                        }
                        call.respond(GetTreeResponse(GetTreeResponse.TaxonomyTree(children), type))
                    }

                    "Individual" -> {
                        call.respond(GetTreeResponse(GetTreeResponse.TaxonomyTree(emptyList()), type))
                    }
                }
            }
        }
    }
}