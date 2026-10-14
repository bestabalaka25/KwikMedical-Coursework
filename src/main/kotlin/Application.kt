
package uk.ac.napier.kwikmedical

import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*

fun Application.rootModule() {
    install(ContentNegotiation) {
        json()
    }

    configureRouting()
}
