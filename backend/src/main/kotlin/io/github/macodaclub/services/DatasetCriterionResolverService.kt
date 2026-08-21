package io.github.macodaclub.services

import io.github.macodaclub.models.api.datasets.DatasetSearchCriterion
import io.github.macodaclub.models.api.datasets.DatasetSearchRequest
import io.github.macodaclub.models.api.datasets.DatasetSourceType

/**
 * Resolve valores amigáveis do MyCODA para valores aceites pela OpenAIRE.
 * Mantém esta lógica fora do frontend e fora do query-string builder.
 */
object DatasetCriterionResolverService {

    fun resolve(request: DatasetSearchRequest): DatasetSearchRequest =
        request.copy(
            criteria = request.criteria.map(::resolveCriterion)
        )

    private fun resolveCriterion(
        criterion: DatasetSearchCriterion
    ): DatasetSearchCriterion {
        if (criterion.source != DatasetSourceType.OPENAIRE) {
            return criterion
        }

        return when (criterion.field) {
            "authorOrcid" -> criterion.copy(
                value = ResearchIdentifierService.normalizeOrcid(
                    criterion.value
                )
            )

            "rorId" -> criterion.copy(
                value = ResearchIdentifierService.normalizeRor(
                    criterion.value
                )
            )

            "relOrganizationId" ->
                resolveRelatedOrganization(criterion)

            else -> criterion
        }
    }

    private fun resolveRelatedOrganization(
        criterion: DatasetSearchCriterion
    ): DatasetSearchCriterion {
        val rawValue = criterion.value.trim()

        val normalizedRor =
            ResearchIdentifierService.tryNormalizeRor(rawValue)

        if (normalizedRor != null) {
            val openAireId =
                OpenAireOrganizationClient.resolveOpenAireIdByRor(
                    normalizedRor
                )

            return criterion.copy(value = openAireId)
        }

        require(rawValue.contains("::")) {
            "Related organization must be a ROR identifier " +
                "or an OpenAIRE organization id."
        }

        return criterion.copy(value = rawValue)
    }
}
