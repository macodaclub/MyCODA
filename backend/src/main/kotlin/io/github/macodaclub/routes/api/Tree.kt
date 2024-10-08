package io.github.macodaclub.routes.api

import io.github.macodaclub.models.api.tree.GetTreeResponse
import io.github.macodaclub.plugins.OntologyManager
import io.github.macodaclub.utils.toTaxonomyTreeEntity
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.semanticweb.owlapi.model.IRI

fun Routing.treeRoutes(
    ontologyManager: OntologyManager
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
                                    ontologyManager.mergedOntology.classesInSignature
                                        .filter {
                                            !it.isTopEntity && ontologyManager.reasoner.getSuperClasses(it, true)
                                                .firstOrNull()?.isTopNode ?: true
                                        }
                                        .map { cls -> cls.toTaxonomyTreeEntity(ontologyManager.mergedOntology, ontologyManager.reasoner) }
                                ),
                                type
                            )
                        )
                    }

                    "Property" -> {
                        val dp = ontologyManager.mergedOntology.dataPropertiesInSignature
                            .filter {
                                !it.isTopEntity && ontologyManager.reasoner.getSuperDataProperties(it, true)
                                    .firstOrNull()?.isTopNode ?: true
                            }
                            .map { dp -> dp.toTaxonomyTreeEntity(ontologyManager.mergedOntology, ontologyManager.reasoner) }
                        val op = ontologyManager.mergedOntology.objectPropertiesInSignature
                            .filter {
                                !it.isTopEntity && ontologyManager.reasoner.getSuperObjectProperties(it, true)
                                    .firstOrNull()?.isTopNode ?: true
                            }
                            .map { op -> op.toTaxonomyTreeEntity(ontologyManager.mergedOntology, ontologyManager.reasoner) }
                        call.respond(
                            GetTreeResponse(GetTreeResponse.TaxonomyTree(dp + op), type)
                        )
                    }

                    "Individual" -> {
                        call.respond(
                            GetTreeResponse(
                                GetTreeResponse.TaxonomyTree(
                                    ontologyManager.mergedOntology.individualsInSignature
                                        .map { ind -> ind.toTaxonomyTreeEntity(ontologyManager.mergedOntology) }
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
                    val entity = ontologyManager.mergedOntology.getEntitiesInSignature(IRI.create(iri)).firstOrNull()
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
                        val cls = ontologyManager.mergedOntology.getEntitiesInSignature(IRI.create(iri))
                            .firstOrNull { it.isOWLClass }?.asOWLClass()
                            ?: return@get call.respond(HttpStatusCode.BadRequest)
                        val superClasses = ontologyManager.reasoner.getSuperClasses(cls, false)
                        val leaf = cls.toTaxonomyTreeEntity(ontologyManager.mergedOntology, ontologyManager.reasoner)
                        val root = superClasses.fold(leaf) { acc, node ->
                            val superClass = node.representativeElement
                            if (superClass.isTopEntity) return@fold acc
                            superClass.toTaxonomyTreeEntity(ontologyManager.mergedOntology, ontologyManager.reasoner) {
                                listOf(acc)
                            }
                        }
                        call.respond(GetTreeResponse(GetTreeResponse.TaxonomyTree(listOf(root)), type))
                    }

                    "Property" -> {
                        val entity = ontologyManager.mergedOntology.getEntitiesInSignature(IRI.create(iri))
                            .firstOrNull { it.isOWLDataProperty || it.isOWLObjectProperty }
                            ?: return@get call.respond(HttpStatusCode.BadRequest)
                        when {
                            entity.isOWLDataProperty -> {
                                val property = entity.asOWLDataProperty()
                                val superProperties = ontologyManager.reasoner.getSuperDataProperties(property, false)
                                val leaf = property.toTaxonomyTreeEntity(ontologyManager.mergedOntology, ontologyManager.reasoner)
                                val root = superProperties.fold(leaf) { acc, node ->
                                    val superProperty =
                                        node.entities.firstOrNull { !it.isBottomEntity && !it.isTopEntity && !it.isAnonymous }
                                            ?.asOWLDataProperty() ?: return@fold acc
                                    // TODO: handle inverseOf and equivalents
                                    superProperty.toTaxonomyTreeEntity(ontologyManager.mergedOntology, ontologyManager.reasoner) {
                                        listOf(acc)
                                    }
                                }
                                call.respond(GetTreeResponse(GetTreeResponse.TaxonomyTree(listOf(root)), type))
                            }

                            entity.isOWLObjectProperty -> {
                                val property = entity.asOWLObjectProperty()
                                val superProperties = ontologyManager.reasoner.getSuperObjectProperties(property, false)
                                val leaf = property.toTaxonomyTreeEntity(ontologyManager.mergedOntology, ontologyManager.reasoner)
                                val root = superProperties.fold(leaf) { acc, node ->
                                    val superProperty =
                                        node.entities.firstOrNull { !it.isBottomEntity && !it.isTopEntity && !it.isAnonymous }
                                            ?.asOWLObjectProperty() ?: return@fold acc
                                    // TODO: handle inverseOf and equivalents
                                    superProperty.toTaxonomyTreeEntity(ontologyManager.mergedOntology, ontologyManager.reasoner) {
                                        listOf(acc)
                                    }
                                }
                                call.respond(GetTreeResponse(GetTreeResponse.TaxonomyTree(listOf(root)), type))
                            }

                            else -> return@get call.respond(HttpStatusCode.BadRequest)
                        }
                    }

                    "Individual" -> {
                        val individual = ontologyManager.mergedOntology.getEntitiesInSignature(IRI.create(iri))
                            .firstOrNull { it.isOWLNamedIndividual }
                            ?.asOWLNamedIndividual()
                            ?: return@get call.respond(HttpStatusCode.BadRequest)
                        val leaf = individual.toTaxonomyTreeEntity(ontologyManager.mergedOntology)
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
                            ontologyManager.mergedOntology.getEntitiesInSignature(IRI.create(iri))
                                .firstOrNull { it.isOWLClass }
                                ?.asOWLClass()
                                ?: return@get call.respond(HttpStatusCode.BadRequest)
                        val children = ontologyManager.reasoner.getSubClasses(cls, true)
                            .filterNot { it.isBottomNode }
                            .map { it.representativeElement.toTaxonomyTreeEntity(ontologyManager.mergedOntology, ontologyManager.reasoner) }
                        call.respond(GetTreeResponse(GetTreeResponse.TaxonomyTree(children), type))
                    }

                    "Property" -> {
                        val property =
                            ontologyManager.mergedOntology.getEntitiesInSignature(IRI.create(iri))
                                .firstOrNull { it.isOWLDataProperty || it.isOWLObjectProperty }
                                ?: return@get call.respond(HttpStatusCode.BadRequest)
                        val children = when {
                            property.isOWLDataProperty -> {
                                ontologyManager.reasoner.getSubDataProperties(property.asOWLDataProperty(), true)
                                    .filterNot { it.isBottomNode }
                                    .mapNotNull { node ->
                                        node.entities.firstOrNull {
                                            !it.isBottomEntity && !it.isTopEntity && !it.isAnonymous
                                        }
                                            ?.asOWLDataProperty() // TODO: handle inverseOf and equivalents
                                            ?.toTaxonomyTreeEntity(ontologyManager.mergedOntology, ontologyManager.reasoner)
                                    }
                            }

                            property.isOWLObjectProperty -> {
                                ontologyManager.reasoner.getSubObjectProperties(property.asOWLObjectProperty(), true)
                                    .filterNot { it.isBottomNode }
                                    .mapNotNull { node ->
                                        node.entities.firstOrNull {
                                            !it.isBottomEntity && !it.isTopEntity && !it.isAnonymous
                                        }
                                            ?.asOWLObjectProperty() // TODO: handle inverseOf and equivalents
                                            ?.toTaxonomyTreeEntity(ontologyManager.mergedOntology, ontologyManager.reasoner)
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