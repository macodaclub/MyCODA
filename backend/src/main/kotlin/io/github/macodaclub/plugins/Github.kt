package io.github.macodaclub.plugins

import org.kohsuke.github.GHRepository
import org.kohsuke.github.GitHub
import org.kohsuke.github.GitHubBuilder

fun configureGithub(): GHRepository? {
    return try {
        val github: GitHub = run {
            val fromEnvironment = GitHubBuilder.fromEnvironment().build()
            if (!fromEnvironment.isAnonymous) return@run fromEnvironment

            val propertyFilePath: String? = System.getenv("GITHUB_PROPERTY_FILE_PATH")
            val fromProperty = (
                propertyFilePath
                    ?.let { GitHubBuilder.fromPropertyFile(it) }
                    ?: GitHubBuilder.fromPropertyFile()
                ).build()

            if (!fromProperty.isAnonymous) return@run fromProperty

            throw IllegalStateException(
                "GitHub API Authentication Error: Cannot get valid OAUTH token in environment variable or property file."
            )
        }

        val repoName = "macodaclub/MyCODA"
        val ghRepo = github.getRepository(repoName)

        if (!ghRepo.isCollaborator(github.myself)) {
            throw IllegalStateException(
                "GitHub API Authentication Error: User is not a collaborator of repository $repoName"
            )
        }

        ghRepo
    } catch (e: Exception) {
        System.err.println(
            "WARNING: GitHub integration unavailable. Application will continue without GitHub integration."
        )
        e.printStackTrace()

        null
    }
}