
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
    // Ktor server
    implementation(ktorLibs.server.core)
    implementation(ktorLibs.server.netty)
    implementation(ktorLibs.server.contentNegotiation)
    implementation("io.ktor:ktor-server-status-pages-jvm:3.6.0")

    // JSON serialisation
    implementation("io.ktor:ktor-serialization-kotlinx-json:3.6.0")

    // Database
    implementation("com.h2database:h2:2.4.240")

    // Logging
    implementation(libs.logback.classic)

    // Ambulance smartphone HTTP client
    implementation("io.ktor:ktor-client-core:3.6.0")
    implementation("io.ktor:ktor-client-cio:3.6.0")
    implementation("io.ktor:ktor-client-content-negotiation:3.6.0")

    // Testing
    testImplementation(kotlin("test"))
    testImplementation(ktorLibs.server.testHost)
}
tasks.register<JavaExec>("runAmbulanceSimulation") {
    group = "application"
    description = "Runs the simulated ambulance smartphone client"

    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("uk.ac.napier.kwikmedical.ambulance.AmbulanceSimulationKt")

    if (project.hasProperty("emergencyId")) {
        args(project.property("emergencyId").toString())
    }
}