package io.github.macodaclub.plugins

import org.kohsuke.github.GHRepository
import org.kohsuke.github.GitHub
import org.kohsuke.github.GitHubBuilder

fun configureGithub(): GHRepository {
    val github: GitHub = run {
        val fromEnvironment = GitHubBuilder.fromEnvironment().build()
        if (!fromEnvironment.isAnonymous) return@run fromEnvironment

        val propertyFilePath: String? = System.getenv("GITHUB_PROPERTY_FILE_PATH")
        val fromProperty = (propertyFilePath
            ?.let { GitHubBuilder.fromPropertyFile(it) }
            ?: GitHubBuilder.fromPropertyFile())
            .build()
        if (!fromProperty.isAnonymous) return@run fromProperty

        error("GitHub API Authentication Error: Cannot get valid OAUTH token in environment variable or property file. Please make sure the token is valid, or check your configuration: https://github-api.kohsuke.org/.")
    }

    val repoName = "macodaclub/MyCODA"
    val ghRepo: GHRepository = github.getRepository(repoName)

    if (!ghRepo.isCollaborator(github.myself)) error("GitHub API Authentication Error: User is not a collaborator of repository $repoName")

    return ghRepo
}