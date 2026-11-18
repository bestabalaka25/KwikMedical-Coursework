
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(ktorLibs.plugins.ktor)
    alias(libs.plugins.kotlin.serialization)
}

group = "uk.ac.napier.kwikmedical"
version = "1.0.0-SNAPSHOT"

application {
    mainClass = "uk.ac.napier.kwikmedical.MainKt"
}

kotlin {
    jvmToolchain(17)
}
dependencies {
    implementation(ktorLibs.server.core)
    implementation(ktorLibs.server.netty)
    implementation(libs.logback.classic)
    implementation(ktorLibs.server.contentNegotiation)
    implementation("io.ktor:ktor-serialization-kotlinx-json:3.6.0")
    implementation("io.ktor:ktor-server-status-pages-jvm:3.6.0")
    implementation("com.h2database:h2:2.4.240")

    testImplementation(kotlin("test"))
    testImplementation(ktorLibs.server.testHost)
}
