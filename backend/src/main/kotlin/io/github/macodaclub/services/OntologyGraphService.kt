package io.github.macodaclub.services

import io.github.macodaclub.models.api.ontology.OntologyGraphEdge
import io.github.macodaclub.models.api.ontology.OntologyGraphEdgeData
import io.github.macodaclub.models.api.ontology.OntologyGraphNode
import io.github.macodaclub.models.api.ontology.OntologyGraphNodeData
import io.github.macodaclub.models.api.ontology.OntologyGraphResponse
import io.github.macodaclub.plugins.OntologyManager
import io.github.macodaclub.utils.getLabel
import org.semanticweb.owlapi.model.IRI
import org.semanticweb.owlapi.model.OWLEntity
import org.semanticweb.owlapi.model.OWLLiteral
import org.semanticweb.owlapi.search.EntitySearcher
import java.security.MessageDigest
import java.util.ArrayDeque

/**
 * Constrói um subgrafo da ontologia centrado numa entidade.
 *
 * A resposta é compatível com Cytoscape:
 * response.nodes + response.edges.
 */
object OntologyGraphService {

    private data class PendingEntity(
        val iri: String,
        val type: String,
        val remainingDepth: Int
    )

    fun buildGraph(
        ontologyManager: OntologyManager,
        iri: String,
        requestedType: String?,
        depth: Int
    ): OntologyGraphResponse {
        val normalizedDepth = depth.coerceIn(1, 3)

        val entity = findEntity(
            ontologyManager = ontologyManager,
            iri = iri,
            requestedType = requestedType
        ) ?: throw IllegalArgumentException(
            "The requested ontology entity was not found."
        )

        val rootType = entityType(entity)

        val nodes = linkedMapOf<String, OntologyGraphNode>()
        val edges = linkedMapOf<String, OntologyGraphEdge>()
        val expanded = mutableSetOf<String>()
        val queue = ArrayDeque<PendingEntity>()

        addEntityNode(
            ontologyManager = ontologyManager,
            nodes = nodes,
            entity = entity,
            selected = true
        )

        queue.add(
            PendingEntity(
                iri = entity.iri.toString(),
                type = rootType,
                remainingDepth = normalizedDepth
            )
        )

        while (queue.isNotEmpty()) {
            val pending = queue.removeFirst()
            val visitKey = "${pending.type}:${pending.iri}"

            if (!expanded.add(visitKey)) {
                continue
            }

            val current = findEntity(
                ontologyManager = ontologyManager,
                iri = pending.iri,
                requestedType = pending.type
            ) ?: continue

            val neighbours = when (pending.type) {
                "Class" -> expandClass(
                    ontologyManager = ontologyManager,
                    entity = current,
                    nodes = nodes,
                    edges = edges
                )

                "Property" -> expandProperty(
                    ontologyManager = ontologyManager,
                    entity = current,
                    nodes = nodes,
                    edges = edges
                )

                "Individual" -> expandIndividual(
                    ontologyManager = ontologyManager,
                    entity = current,
                    nodes = nodes,
                    edges = edges
                )

                else -> emptyList()
            }

            if (pending.remainingDepth > 1) {
                neighbours.forEach { neighbour ->
                    queue.add(
                        PendingEntity(
                            iri = neighbour.iri.toString(),
                            type = entityType(neighbour),
                            remainingDepth = pending.remainingDepth - 1
                        )
                    )
                }
            }
        }

        return OntologyGraphResponse(
            nodes = nodes.values.toList(),
            edges = edges.values.toList()
        )
    }

    private fun expandClass(
        ontologyManager: OntologyManager,
        entity: OWLEntity,
        nodes: MutableMap<String, OntologyGraphNode>,
        edges: MutableMap<String, OntologyGraphEdge>
    ): List<OWLEntity> {
        val cls = entity.asOWLClass()
        val neighbours = linkedSetOf<OWLEntity>()

        ontologyManager.reasoner
            .getSuperClasses(cls, true)
            .filterNot { it.isTopNode || it.isBottomNode }
            .map { it.representativeElement }
            .forEach { parent ->
                addEntityNode(ontologyManager, nodes, parent)
                addEdge(
                    edges = edges,
                    source = cls.iri.toString(),
                    target = parent.iri.toString(),
                    label = "subClassOf"
                )
                neighbours.add(parent)
            }

        ontologyManager.reasoner
            .getSubClasses(cls, true)
            .filterNot { it.isTopNode || it.isBottomNode }
            .map { it.representativeElement }
            .forEach { child ->
                addEntityNode(ontologyManager, nodes, child)
                addEdge(
                    edges = edges,
                    source = child.iri.toString(),
                    target = cls.iri.toString(),
                    label = "subClassOf"
                )
                neighbours.add(child)
            }

        ontologyManager.reasoner
            .getEquivalentClasses(cls)
            .entities
            .filter {
                it.iri != cls.iri &&
                    !it.isTopEntity &&
                    !it.isBottomEntity
            }
            .forEach { equivalent ->
                addEntityNode(ontologyManager, nodes, equivalent)
                addEdge(
                    edges = edges,
                    source = cls.iri.toString(),
                    target = equivalent.iri.toString(),
                    label = "equivalentClass"
                )
                neighbours.add(equivalent)
            }

        ontologyManager.reasoner
            .getInstances(cls, true)
            .filterNot { it.isBottomNode }
            .map { it.representativeElement }
            .forEach { individual ->
                addEntityNode(ontologyManager, nodes, individual)
                addEdge(
                    edges = edges,
                    source = individual.iri.toString(),
                    target = cls.iri.toString(),
                    label = "rdf:type"
                )
                neighbours.add(individual)
            }

        ontologyManager.mergedOntology.objectPropertiesInSignature
            .forEach { property ->
                val domains = EntitySearcher.getDomains(
                    property,
                    ontologyManager.mergedOntology
                )

                if (
                    domains.any {
                        !it.isAnonymous &&
                            it.asOWLClass().iri == cls.iri
                    }
                ) {
                    addEntityNode(ontologyManager, nodes, property)
                    addEdge(
                        edges = edges,
                        source = property.iri.toString(),
                        target = cls.iri.toString(),
                        label = "domain"
                    )
                    neighbours.add(property)
                }

                val ranges = EntitySearcher.getRanges(
                    property,
                    ontologyManager.mergedOntology
                )

                if (
                    ranges.any {
                        !it.isAnonymous &&
                            it.asOWLClass().iri == cls.iri
                    }
                ) {
                    addEntityNode(ontologyManager, nodes, property)
                    addEdge(
                        edges = edges,
                        source = property.iri.toString(),
                        target = cls.iri.toString(),
                        label = "range"
                    )
                    neighbours.add(property)
                }
            }

        ontologyManager.mergedOntology.dataPropertiesInSignature
            .forEach { property ->
                val domains = EntitySearcher.getDomains(
                    property,
                    ontologyManager.mergedOntology
                )

                if (
                    domains.any {
                        !it.isAnonymous &&
                            it.asOWLClass().iri == cls.iri
                    }
                ) {
                    addEntityNode(ontologyManager, nodes, property)
                    addEdge(
                        edges = edges,
                        source = property.iri.toString(),
                        target = cls.iri.toString(),
                        label = "domain"
                    )
                    neighbours.add(property)
                }
            }

        return neighbours.toList()
    }

    private fun expandProperty(
        ontologyManager: OntologyManager,
        entity: OWLEntity,
        nodes: MutableMap<String, OntologyGraphNode>,
        edges: MutableMap<String, OntologyGraphEdge>
    ): List<OWLEntity> {
        val neighbours = linkedSetOf<OWLEntity>()

        if (entity.isOWLObjectProperty) {
            val property = entity.asOWLObjectProperty()

            ontologyManager.reasoner
                .getSuperObjectProperties(property, true)
                .mapNotNull { node ->
                    node.entities
                        .firstOrNull {
                            !it.isAnonymous &&
                                !it.isTopEntity &&
                                !it.isBottomEntity
                        }
                        ?.asOWLObjectProperty()
                }
                .forEach { parent ->
                    addEntityNode(ontologyManager, nodes, parent)
                    addEdge(
                        edges = edges,
                        source = property.iri.toString(),
                        target = parent.iri.toString(),
                        label = "subPropertyOf"
                    )
                    neighbours.add(parent)
                }

            ontologyManager.reasoner
                .getSubObjectProperties(property, true)
                .mapNotNull { node ->
                    node.entities
                        .firstOrNull {
                            !it.isAnonymous &&
                                !it.isTopEntity &&
                                !it.isBottomEntity
                        }
                        ?.asOWLObjectProperty()
                }
                .forEach { child ->
                    addEntityNode(ontologyManager, nodes, child)
                    addEdge(
                        edges = edges,
                        source = child.iri.toString(),
                        target = property.iri.toString(),
                        label = "subPropertyOf"
                    )
                    neighbours.add(child)
                }

            EntitySearcher.getDomains(
                property,
                ontologyManager.mergedOntology
            )
                .filter { !it.isAnonymous }
                .map { it.asOWLClass() }
                .forEach { domain ->
                    addEntityNode(ontologyManager, nodes, domain)
                    addEdge(
                        edges = edges,
                        source = property.iri.toString(),
                        target = domain.iri.toString(),
                        label = "domain"
                    )
                    neighbours.add(domain)
                }

            EntitySearcher.getRanges(
                property,
                ontologyManager.mergedOntology
            )
                .filter { !it.isAnonymous }
                .map { it.asOWLClass() }
                .forEach { range ->
                    addEntityNode(ontologyManager, nodes, range)
                    addEdge(
                        edges = edges,
                        source = property.iri.toString(),
                        target = range.iri.toString(),
                        label = "range"
                    )
                    neighbours.add(range)
                }
        } else if (entity.isOWLDataProperty) {
            val property = entity.asOWLDataProperty()

            ontologyManager.reasoner
                .getSuperDataProperties(property, true)
                .mapNotNull { node ->
                    node.entities
                        .firstOrNull {
                            !it.isTopEntity &&
                                !it.isBottomEntity
                        }
                }
                .forEach { parent ->
                    addEntityNode(ontologyManager, nodes, parent)
                    addEdge(
                        edges = edges,
                        source = property.iri.toString(),
                        target = parent.iri.toString(),
                        label = "subPropertyOf"
                    )
                    neighbours.add(parent)
                }

            ontologyManager.reasoner
                .getSubDataProperties(property, true)
                .mapNotNull { node ->
                    node.entities
                        .firstOrNull {
                            !it.isTopEntity &&
                                !it.isBottomEntity
                        }
                }
                .forEach { child ->
                    addEntityNode(ontologyManager, nodes, child)
                    addEdge(
                        edges = edges,
                        source = child.iri.toString(),
                        target = property.iri.toString(),
                        label = "subPropertyOf"
                    )
                    neighbours.add(child)
                }

            EntitySearcher.getDomains(
                property,
                ontologyManager.mergedOntology
            )
                .filter { !it.isAnonymous }
                .map { it.asOWLClass() }
                .forEach { domain ->
                    addEntityNode(ontologyManager, nodes, domain)
                    addEdge(
                        edges = edges,
                        source = property.iri.toString(),
                        target = domain.iri.toString(),
                        label = "domain"
                    )
                    neighbours.add(domain)
                }

            EntitySearcher.getRanges(
                property,
                ontologyManager.mergedOntology
            )
                .mapNotNull { range ->
                    if (range.isDatatype) {
                        range.asOWLDatatype()
                    } else {
                        null
                    }
                }
                .forEach { range ->
                    addEntityNode(ontologyManager, nodes, range)
                    addEdge(
                        edges = edges,
                        source = property.iri.toString(),
                        target = range.iri.toString(),
                        label = "range"
                    )
                    neighbours.add(range)
                }
        }

        return neighbours.toList()
    }

    private fun expandIndividual(
        ontologyManager: OntologyManager,
        entity: OWLEntity,
        nodes: MutableMap<String, OntologyGraphNode>,
        edges: MutableMap<String, OntologyGraphEdge>
    ): List<OWLEntity> {
        val individual = entity.asOWLNamedIndividual()
        val neighbours = linkedSetOf<OWLEntity>()

        ontologyManager.reasoner
            .getTypes(individual, true)
            .filterNot { it.isTopNode || it.isBottomNode }
            .map { it.representativeElement }
            .forEach { type ->
                addEntityNode(ontologyManager, nodes, type)
                addEdge(
                    edges = edges,
                    source = individual.iri.toString(),
                    target = type.iri.toString(),
                    label = "rdf:type"
                )
                neighbours.add(type)
            }

        EntitySearcher.getObjectPropertyValues(
            individual,
            ontologyManager.mergedOntology
        ).asMap().forEach { (propertyExpression, values) ->
            if (propertyExpression.isAnonymous) {
                return@forEach
            }

            val property = propertyExpression.asOWLObjectProperty()

            addEntityNode(ontologyManager, nodes, property)
            neighbours.add(property)

            values
                .filter { it.isNamed }
                .map { it.asOWLNamedIndividual() }
                .forEach { value ->
                    addEntityNode(ontologyManager, nodes, value)
                    addEdge(
                        edges = edges,
                        source = individual.iri.toString(),
                        target = value.iri.toString(),
                        label = property.getLabel(
                            ontologyManager.mergedOntology
                        )
                    )
                    neighbours.add(value)
                }
        }

        EntitySearcher.getDataPropertyValues(
            individual,
            ontologyManager.mergedOntology
        ).asMap().forEach { (propertyExpression, values) ->
            val property = propertyExpression.asOWLDataProperty()

            addEntityNode(ontologyManager, nodes, property)
            neighbours.add(property)

            values.forEach { literal ->
                addLiteralNode(
                    nodes = nodes,
                    edges = edges,
                    individualIri = individual.iri.toString(),
                    propertyLabel = property.getLabel(
                        ontologyManager.mergedOntology
                    ),
                    literal = literal
                )
            }
        }

        return neighbours.toList()
    }

    private fun findEntity(
        ontologyManager: OntologyManager,
        iri: String,
        requestedType: String?
    ): OWLEntity? {
        val entities = ontologyManager.mergedOntology
            .getEntitiesInSignature(IRI.create(iri))

        return when (requestedType) {
            "Class" ->
                entities.firstOrNull { it.isOWLClass }

            "Property" ->
                entities.firstOrNull {
                    it.isOWLDataProperty ||
                        it.isOWLObjectProperty
                }

            "Individual" ->
                entities.firstOrNull {
                    it.isOWLNamedIndividual
                }

            else ->
                entities.firstOrNull()
        }
    }

    private fun addEntityNode(
        ontologyManager: OntologyManager,
        nodes: MutableMap<String, OntologyGraphNode>,
        entity: OWLEntity,
        selected: Boolean = false
    ) {
        val id = entity.iri.toString()

        nodes[id] = OntologyGraphNode(
            OntologyGraphNodeData(
                id = id,
                iri = id,
                label = entity.getLabel(
                    ontologyManager.mergedOntology
                ),
                type = entityType(entity),
                selected =
                    selected ||
                        nodes[id]?.data?.selected == true
            )
        )
    }

    private fun addLiteralNode(
        nodes: MutableMap<String, OntologyGraphNode>,
        edges: MutableMap<String, OntologyGraphEdge>,
        individualIri: String,
        propertyLabel: String,
        literal: OWLLiteral
    ) {
        val literalId =
            "literal:${sha256(
                "$individualIri|$propertyLabel|${literal.literal}"
            )}"

        nodes[literalId] = OntologyGraphNode(
            OntologyGraphNodeData(
                id = literalId,
                iri = null,
                label = literal.literal,
                type = "Datatype"
            )
        )

        addEdge(
            edges = edges,
            source = individualIri,
            target = literalId,
            label = propertyLabel
        )
    }

    private fun addEdge(
        edges: MutableMap<String, OntologyGraphEdge>,
        source: String,
        target: String,
        label: String
    ) {
        val id =
            "edge:${sha256("$source|$label|$target")}"

        edges[id] = OntologyGraphEdge(
            OntologyGraphEdgeData(
                id = id,
                source = source,
                target = target,
                label = label
            )
        )
    }

    private fun entityType(entity: OWLEntity): String =
        when {
            entity.isOWLClass ->
                "Class"

            entity.isOWLNamedIndividual ->
                "Individual"

            entity.isOWLDataProperty ||
                entity.isOWLObjectProperty ->
                "Property"

            entity.isOWLDatatype ->
                "Datatype"

            entity.isOWLAnnotationProperty ->
                "Annotation"

            else ->
                "Entity"
        }

    private fun sha256(value: String): String =
        MessageDigest
            .getInstance("SHA-256")
            .digest(value.toByteArray())
            .joinToString("") { byte ->
                "%02x".format(byte)
            }
}