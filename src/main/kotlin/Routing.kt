
package uk.ac.napier.kwikmedical

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import uk.ac.napier.kwikmedical.repository.PatientRepository
import uk.ac.napier.kwikmedical.emergency.EmergencyRequest
import uk.ac.napier.kwikmedical.service.EmergencyService
import uk.ac.napier.kwikmedical.repository.EmergencyRepository
import uk.ac.napier.kwikmedical.emergency.Emergency
import uk.ac.napier.kwikmedical.service.DispatchService
@Serializable
data class Patient(
    val nhsNumber: String,
    val name: String,
    val address: String,
    val medicalCondition: String
)
@Serializable
data class DispatchResponse(
    val emergency: Emergency,
    val patient: Patient
)

fun Application.configureRouting() {
    val patientRepository = PatientRepository()
    val emergencyService = EmergencyService()
    val emergencyRepository = EmergencyRepository()
    val dispatchService = DispatchService()

    routing {

        get("/") {
            call.respondText("Hello, World!")
        }

        get("/patients") {
            call.respond(patientRepository.findAll())
        }

        get("/patients/{nhsNumber}") {
            val nhsNumber = call.parameters["nhsNumber"]

            if (nhsNumber == null) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Patient number is required")
                )
                return@get
            }

            val patient = patientRepository.findByNhsNumber(nhsNumber)

            if (patient != null) {
                call.respond(patient)
            } else {
                call.respond(
                    HttpStatusCode.NotFound,
                    mapOf("error" to "Patient not found")
                )
            }
        }

        post("/emergencies") {
            val request = call.receive<EmergencyRequest>()

            val emergency = emergencyService.createEmergency(
                patientNhsNumber = request.patientNhsNumber,
                incidentAddress = request.incidentAddress,
                description = request.description
            )

            if (emergency != null) {
                call.respond(HttpStatusCode.Created, emergency)
            } else {
                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Patient not found or no available hospital")
                )
            }
        }

        get("/emergencies") {
            call.respond(emergencyRepository.findAll())
        }

        get("/hospitals/{hospitalId}/emergencies") {
            val hospitalId = call.parameters["hospitalId"]

            if (hospitalId == null) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Hospital ID is required")
                )
                return@get
            }

            val emergencies = emergencyRepository.findByHospitalId(hospitalId)

            call.respond(emergencies)
        }



        get("/emergencies/{emergencyId}") {
            val emergencyId = call.parameters["emergencyId"]

            if (emergencyId == null) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Emergency ID is required")
                )
                return@get
            }

            val emergency = emergencyRepository.findById(emergencyId)

            if (emergency != null) {
                call.respond(emergency)
            } else {
                call.respond(
                    HttpStatusCode.NotFound,
                    mapOf("error" to "Emergency not found")
                )
            }
        }

        put("/emergencies/{emergencyId}/complete") {
            val emergencyId = call.parameters["emergencyId"]

            if (emergencyId == null) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Emergency ID is required")
                )
                return@put
            }

            val completed = emergencyService.completeEmergency(emergencyId)

            if (completed) {
                call.respond(
                    HttpStatusCode.OK,
                    mapOf("message" to "Emergency completed and ambulance released")
                )
            } else {
                call.respond(
                    HttpStatusCode.NotFound,
                    mapOf("error" to "Emergency not found or already completed")
                )
            }
        }

        get("/dispatch/{emergencyId}") {
            val emergencyId = call.parameters["emergencyId"]

            if (emergencyId == null) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Emergency ID is required")
                )
                return@get
            }

            val dispatch = dispatchService.getDispatchDetails(emergencyId)

            if (dispatch != null) {
                call.respond(
                    DispatchResponse(
                        emergency = dispatch.emergency,
                        patient = dispatch.patient
                    )
                )
            } else {
                call.respond(
                    HttpStatusCode.NotFound,
                    mapOf("error" to "Emergency or patient record not found")
                )
            }
        }


    }
}
