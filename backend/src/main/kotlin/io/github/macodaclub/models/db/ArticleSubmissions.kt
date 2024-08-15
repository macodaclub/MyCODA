package io.github.macodaclub.models.db

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Column

object ArticleSubmissions : IntIdTable() {
    val articleTitle: Column<String> = varchar("article_title", 500)
    val articleAbstract: Column<String> = varchar("article_abstract", 2000)
    val articleKeywords: Column<String> = varchar("article_keywords", 300)
    val articleAuthors: Column<String> = varchar("article_authors", 500)
    val emailAddress: Column<String> = varchar("email_address", 100)
    val githubIssueUrl: Column<String> = varchar("github_issue_url", 300)
}

class ArticleSubmission(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<ArticleSubmission>(ArticleSubmissions)
    var articleTitle by ArticleSubmissions.articleTitle
    var articleAbstract by ArticleSubmissions.articleAbstract
    var articleKeywords by ArticleSubmissions.articleKeywords
    var articleAuthors by ArticleSubmissions.articleAuthors
    var emailAddress by ArticleSubmissions.emailAddress
    var githubIssueUrl by ArticleSubmissions.githubIssueUrl
}