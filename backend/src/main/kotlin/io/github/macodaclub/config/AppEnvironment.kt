package io.github.macodaclub.config

enum class AppEnvironment {
    DEV,
    PRD;

    companion object {

        fun current(): AppEnvironment {
            val value =
                System.getenv("MYCODA_ENV")
                    ?.trim()
                    ?.uppercase()
                    ?: "DEV"

            return entries.firstOrNull {
                it.name == value
            } ?: throw IllegalStateException(
                "Invalid MYCODA_ENV value: $value. Expected DEV or PRD."
            )
        }
    }
}