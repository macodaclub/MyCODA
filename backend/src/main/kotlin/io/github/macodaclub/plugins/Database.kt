package io.github.macodaclub.plugins

import io.github.macodaclub.models.db.ArticleSubmissions
import io.ktor.server.application.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

fun Application.configureDatabase() {
    val user = System.getenv("MYSQL_USER") ?: error("Database Error: MYSQL_USER not defined")
    val password = System.getenv("MYSQL_PASSWORD") ?: error("Database Error: MYSQL_PASSWORD not defined")
    val db = System.getenv("MYSQL_DATABASE") ?: error("Database Error: MYSQL_DATABASE not defined")
    val dbPort = System.getenv("MYSQL_PORT") ?: "3306"
    Database.connect(
        "jdbc:mysql://localhost:$dbPort/$db",
        driver = "com.mysql.cj.jdbc.Driver",
        user = user,
        password = password
    )
    transaction {
        SchemaUtils.createMissingTablesAndColumns(ArticleSubmissions)
    }
}