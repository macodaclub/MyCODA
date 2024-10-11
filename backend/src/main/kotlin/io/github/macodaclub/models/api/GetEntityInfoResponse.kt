package io.github.macodaclub.models.api

import kotlinx.serialization.Serializable

@Serializable
data class GetEntityInfoResponse(
    val entity: Entity,
    val comment: String?,
    val annotations: List<Annotation>,
    // val axioms: List<Axiom>,
    val classInfo: ClassInfo? = null,
    val individualInfo: IndividualInfo? = null,
    val propertyInfo: PropertyInfo? = null,
) {
    @Serializable
    data class Annotation(
        val property: Entity,
        val values: List<Entity>?
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
        val properties: List<Property>
        // sameAs TODO
    ) {
        @Serializable
        data class Property(
            val property: Entity,
            val range: Entity,
            val values: List<Entity>?,
            // val inverseOfProperty: Entity?, TODO
        )
    }

    @Serializable
    data class PropertyInfo(
        val domain: Entity?,
        val range: Entity,
        // val inverseOf: Entity?, TODO
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