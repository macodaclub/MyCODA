package io.github.macodaclub.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.io.path.Path
import kotlin.io.path.pathString

sealed class MdTemplate(val fileName: String) {
    class SubmitChangesIssue(val changesJson: String, val addedEntitiesHtmlTable: String) : MdTemplate("submit-changes-issue-template.md") {
        suspend fun get() = getMd(fileName)
            .replaceFirst("%changesJson%", changesJson)
            .replaceFirst("%addedEntitiesHtmlTable%", addedEntitiesHtmlTable)
    }

    private companion object {
        private suspend fun getMd(fileName: String): String = withContext(Dispatchers.IO) {
            object {}.javaClass.classLoader!!.getResourceAsStream(Path("md").resolve(fileName).pathString)
                ?.bufferedReader()
                ?.readText()
                ?: error("Error reading markdown file $fileName")
        }
    }
}