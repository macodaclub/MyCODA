package io.github.macodaclub.services

/**
 * Normalização e validação dos identificadores externos usados
 * pelos filtros OpenAIRE.
 */
object ResearchIdentifierService {

    private val rorPattern = Regex(
        "^0[0-9a-hj-km-np-tv-z]{6}[0-9]{2}$",
        RegexOption.IGNORE_CASE
    )

    private val rorPrefix = Regex(
        "^(?:https?://)?(?:www\\.)?ror\\.org/",
        RegexOption.IGNORE_CASE
    )

    private val orcidPrefix = Regex(
        "^(?:https?://)?(?:www\\.)?orcid\\.org/",
        RegexOption.IGNORE_CASE
    )

    fun normalizeRor(rawValue: String): String {
        val identifier = rawValue
            .trim()
            .replace(rorPrefix, "")
            .trimEnd('/')
            .lowercase()

        require(rorPattern.matches(identifier)) {
            "Invalid ROR identifier: $rawValue."
        }

        return "https://ror.org/$identifier"
    }

    fun tryNormalizeRor(rawValue: String): String? =
        try {
            normalizeRor(rawValue)
        } catch (_: IllegalArgumentException) {
            null
        }

    fun normalizeOrcid(rawValue: String): String {
        val withoutPrefix = rawValue
            .trim()
            .replace(orcidPrefix, "")
            .replace(" ", "")
            .uppercase()

        val compact = withoutPrefix.replace("-", "")

        require(Regex("^\\d{15}[\\dX]$").matches(compact)) {
            "Invalid ORCID identifier: $rawValue."
        }

        require(hasValidOrcidChecksum(compact)) {
            "Invalid ORCID checksum: $rawValue."
        }

        return buildString {
            append(compact.substring(0, 4))
            append('-')
            append(compact.substring(4, 8))
            append('-')
            append(compact.substring(8, 12))
            append('-')
            append(compact.substring(12, 16))
        }
    }

    /** ISO/IEC 7064 MOD 11-2, usado pelo ORCID. */
    private fun hasValidOrcidChecksum(compact: String): Boolean {
        var total = 0

        compact.substring(0, 15).forEach { character ->
            total = (total + character.digitToInt()) * 2
        }

        val remainder = total % 11
        val result = (12 - remainder) % 11
        val expected = if (result == 10) 'X' else result.digitToChar()

        return compact.last() == expected
    }
}
