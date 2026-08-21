package io.github.macodaclub.models.api.ontology

import kotlinx.serialization.Serializable

/**
 * Cytoscape-compatible node wrapper.
 */
@Serializable
data class OntologyGraphNode(
    val data: OntologyGraphNodeData
)

/**
 * Cytoscape-compatible edge wrapper.
 */
@Serializable
data class OntologyGraphEdge(
    val data: OntologyGraphEdgeData
)

@Serializable
data class OntologyGraphNodeData(
    val id: String,
    val iri: String? = null,
    val label: String,
    val type: String,
    val selected: Boolean = false
)

@Serializable
data class OntologyGraphEdgeData(
    val id: String,
    val source: String,
    val target: String,
    val label: String
)

@Serializable
data class OntologyGraphResponse(
    val nodes: List<OntologyGraphNode>,
    val edges: List<OntologyGraphEdge>
)

@Serializable
data class OntologyGraphErrorResponse(
    val code: String,
    val message: String
)
