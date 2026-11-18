
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
import uk.ac.napier.kwikmedical.hospital.HospitalCalloutRequest
import uk.ac.napier.kwikmedical.service.HospitalCalloutService
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
    val hospitalCalloutService = HospitalCalloutService()

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

            if (
                request.patientNhsNumber.isBlank() ||
                request.incidentAddress.isBlank() ||
                request.description.isBlank()
            ) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Patient number, incident address and description are required")
                )
                return@post
            }

            val emergency = emergencyService.createEmergency(
                patientNhsNumber = request.patientNhsNumber.trim(),
                incidentAddress = request.incidentAddress.trim(),
                description = request.description.trim()
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

        put("/hospitals/{hospitalId}/emergencies/{emergencyId}/status") {

            val hospitalId = call.parameters["hospitalId"]
            val emergencyId = call.parameters["emergencyId"]

            if (hospitalId == null || emergencyId == null) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Hospital ID and emergency ID are required")
                )
                return@put
            }

            val request = call.receive<HospitalCalloutRequest>()
            if (request.status.isBlank()) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Call-out status is required")
                )
                return@put
            }

            val updated = hospitalCalloutService.updateCalloutStatus(
                hospitalId = hospitalId,
                emergencyId = emergencyId,
                newStatus = request.status.trim().uppercase()
            )

            if (updated) {
                call.respond(
                    HttpStatusCode.OK,
                    mapOf("message" to "Hospital call-out status updated successfully")
                )
            } else {
                call.respond(
                    HttpStatusCode.Conflict,
                    mapOf("error" to "Invalid status transition, hospital assignment or emergency ID")
                )
            }
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
