val ktor_version: String by project
val kotlin_version: String by project
// val corenlp_version: String by project

plugins {
    kotlin("jvm") version "2.0.0"
    id("io.ktor.plugin") version "2.3.12"
    id("org.jetbrains.kotlin.plugin.serialization") version "2.0.0"
}

group = "io.github.macodaclub"
version = "0.0.1"

application {
    mainClass.set("io.ktor.server.netty.EngineMain")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xcontext-receivers")
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.ktor:ktor-server-core-jvm")
    implementation("io.ktor:ktor-server-netty-jvm")
    implementation("io.ktor:ktor-server-sessions-jvm")
    implementation("io.ktor:ktor-server-compression-jvm")
    implementation("io.ktor:ktor-server-default-headers-jvm")
    implementation("io.ktor:ktor-server-http-redirect-jvm")
    implementation("io.ktor:ktor-server-call-logging-jvm")
    implementation("io.ktor:ktor-server-call-id-jvm")
    implementation("io.ktor:ktor-server-metrics-jvm")
    implementation("io.ktor:ktor-server-content-negotiation-jvm")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm")
    implementation("org.jetbrains.exposed:exposed-core:0.53.0")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.53.0")
    implementation("org.jetbrains.exposed:exposed-dao:0.53.0")
    implementation("com.mysql:mysql-connector-j:9.0.0")
    implementation("io.ktor:ktor-server-host-common-jvm")
    implementation("io.ktor:ktor-server-status-pages-jvm")
    implementation("io.ktor:ktor-server-resources")
    implementation("io.ktor:ktor-server-partial-content")
    implementation("ch.qos.logback:logback-classic:1.5.6")
    implementation("org.codehaus.janino:janino:3.1.12")
    implementation("io.ktor:ktor-server-config-yaml")
    implementation("io.ktor:ktor-server-cors")
    implementation("io.ktor:ktor-server-auth")

    //implementation("net.sourceforge.owlapi:owlapi-api:5.5.0")
    implementation("edu.stanford.swrl:swrlapi:2.1.2")
    implementation("edu.stanford.swrl:swrlapi-drools-engine:2.1.2")

    //implementation("edu.stanford.nlp:stanford-corenlp:$corenlp_version")
    //implementation("edu.stanford.nlp:stanford-corenlp:$corenlp_version:models")

    implementation("org.kohsuke:github-api:1.323")
}
