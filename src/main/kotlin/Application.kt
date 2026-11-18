
package uk.ac.napier.kwikmedical

import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import kotlinx.serialization.SerializationException
import uk.ac.napier.kwikmedical.database.DatabaseFactory
import io.ktor.server.plugins.BadRequestException

fun Application.rootModule() {

    install(ContentNegotiation) {
        json()
    }

    install(StatusPages) {

        exception<SerializationException> { call, cause ->
            call.respond(
                HttpStatusCode.BadRequest,
                mapOf("error" to "Invalid JSON request format")
            )
        }
        exception<BadRequestException> { call, cause ->
            call.respond(
                HttpStatusCode.BadRequest,
                mapOf("error" to "Invalid request body")
            )
        }

        exception<Throwable> { call, cause ->
            call.application.environment.log.error(
                "An unexpected error occurred",
                cause
            )

            call.respond(
                HttpStatusCode.InternalServerError,
                mapOf("error" to "An unexpected server error occurred")
            )
        }
    }

    DatabaseFactory.initialize()

    configureRouting()
}
