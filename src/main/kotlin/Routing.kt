
package uk.ac.napier.kwikmedical

import kotlinx.serialization.Serializable
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

@Serializable
data class Patient(
    val nhsNumber: String,
    val name: String,
    val address: String,
    val medicalCondition: String
)

private val patients = listOf(
    Patient(
        nhsNumber = "NHS001",
        name = "Alex Morgan",
        address = "10 Example Street, Edinburgh",
        medicalCondition = "Asthma"
    ),
    Patient(
        nhsNumber = "NHS002",
        name = "Jamie Taylor",
        address = "20 Sample Road, Edinburgh",
        medicalCondition = "Diabetes"
    )
)

fun Application.configureRouting() {
    routing {
        get("/") {
            call.respondText("Hello, World!")
        }

        get("/patients"){
            call.respond(patients)
        }

        get("/patients/{nhsNumber}") {
            val nhsNumber = call.parameters["nhsNumber"]

            val patient = patients.find {
                it.nhsNumber == nhsNumber
            }

            if (patient != null) {
                call.respond(patient)
            } else {
                call.respond(
                    HttpStatusCode.NotFound,
                    mapOf("error" to "Patient not found")
                )
            }
        }
    }
}
