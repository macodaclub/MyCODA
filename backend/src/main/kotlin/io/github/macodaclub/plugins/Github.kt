package io.github.macodaclub.plugins

import org.kohsuke.github.GHRepository
import org.kohsuke.github.GitHubBuilder

fun configureGithub(): GHRepository {
    return GitHubBuilder.fromEnvironment().build()?.getRepository("macodaclub/MyCODA")
        ?: error("GitHub macodaclub/MyCODA repository not found")
}