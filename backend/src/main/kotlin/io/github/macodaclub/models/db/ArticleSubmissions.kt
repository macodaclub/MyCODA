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
    val susAnswer1: Column<Int?> = integer("sus_answer1").nullable()
    val susAnswer2: Column<Int?> = integer("sus_answer2").nullable()
    val susAnswer3: Column<Int?> = integer("sus_answer3").nullable()
    val susAnswer4: Column<Int?> = integer("sus_answer4").nullable()
    val susAnswer5: Column<Int?> = integer("sus_answer5").nullable()
    val susAnswer6: Column<Int?> = integer("sus_answer6").nullable()
    val susAnswer7: Column<Int?> = integer("sus_answer7").nullable()
    val susAnswer8: Column<Int?> = integer("sus_answer8").nullable()
    val susAnswer9: Column<Int?> = integer("sus_answer9").nullable()
    val susAnswer10: Column<Int?> = integer("sus_answer10").nullable()
    val feedback: Column<String?> = varchar("feedback", 2000).nullable()
}

class ArticleSubmission(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<ArticleSubmission>(ArticleSubmissions)
    var articleTitle by ArticleSubmissions.articleTitle
    var articleAbstract by ArticleSubmissions.articleAbstract
    var articleKeywords by ArticleSubmissions.articleKeywords
    var articleAuthors by ArticleSubmissions.articleAuthors
    var emailAddress by ArticleSubmissions.emailAddress
    var githubIssueUrl by ArticleSubmissions.githubIssueUrl
    var susAnswer1 by ArticleSubmissions.susAnswer1
    var susAnswer2 by ArticleSubmissions.susAnswer2
    var susAnswer3 by ArticleSubmissions.susAnswer3
    var susAnswer4 by ArticleSubmissions.susAnswer4
    var susAnswer5 by ArticleSubmissions.susAnswer5
    var susAnswer6 by ArticleSubmissions.susAnswer6
    var susAnswer7 by ArticleSubmissions.susAnswer7
    var susAnswer8 by ArticleSubmissions.susAnswer8
    var susAnswer9 by ArticleSubmissions.susAnswer9
    var susAnswer10 by ArticleSubmissions.susAnswer10
    var feedback by ArticleSubmissions.feedback
}