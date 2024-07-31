package io.github.macodaclub.models

import kotlinx.serialization.Serializable

@Serializable
data class GetEntityInfoResponse(
    val iri: String,
    val type: String,
    val label: String,
    val annotations: List<Annotation>,
    val classInfo: ClassInfo? = null,
    val individualInfo: IndividualInfo? = null,
    val propertyInfo: PropertyInfo? = null,
) {
    @Serializable
    data class Annotation(
        val propertyLabel: String,
        val propertyIri: String,
        val value: String
    )

    @Serializable
    data class ClassInfo(
        //val equivalentClasses: List<Entity>,
        val subClassOf: List<Entity>,
        val disjointWith: List<Entity>,
        val domainOf: List<Entity>,
        val rangeOf: List<Entity>,
    )

    @Serializable
    data class IndividualInfo(
        val types: List<Entity>,
        // Properties TODO
        // sameAs TODO
    )

    @Serializable
    data class PropertyInfo(
        val domain: Entity,
        val range: Entity,
        val isDataProperty: Boolean,
        val inverseOf: Entity?,
    )

    @Serializable
    data class Entity(
        val iri: String,
        val label: String,
        val type: String,
    )

    @Serializable
    data class Axiom(
        val iri: String,
        val label: String,
        val type: String,
    )
}