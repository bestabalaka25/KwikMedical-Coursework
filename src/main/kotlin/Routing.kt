
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
import uk.ac.napier.kwikmedical.service.CalloutService
import uk.ac.napier.kwikmedical.hospital.CalloutRecordRequest
import uk.ac.napier.kwikmedical.hospital.HospitalData
import uk.ac.napier.kwikmedical.hospital.PatientRecordUpdateRequest

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
    val calloutService = CalloutService()


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

        post("/hospitals/{hospitalId}/emergencies/{emergencyId}/callout") {

            val hospitalId = call.parameters["hospitalId"]
            val emergencyId = call.parameters["emergencyId"]

            if (hospitalId == null || emergencyId == null) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Hospital ID and emergency ID are required")
                )
                return@post
            }

            val emergency = emergencyRepository.findById(emergencyId)

            if (emergency == null) {
                call.respond(
                    HttpStatusCode.NotFound,
                    mapOf("error" to "Emergency not found")
                )
                return@post
            }

            if (emergency.allocatedHospitalId != hospitalId) {
                call.respond(
                    HttpStatusCode.Forbidden,
                    mapOf("error" to "This hospital is not assigned to the emergency")
                )
                return@post
            }

            if (emergency.status != "COMPLETED") {
                call.respond(
                    HttpStatusCode.Conflict,
                    mapOf("error" to "Emergency must be completed before submitting the call-out record")
                )
                return@post
            }

            if (calloutService.getCallout(emergencyId) != null) {
                call.respond(
                    HttpStatusCode.Conflict,
                    mapOf("error" to "A call-out record already exists for this emergency")
                )
                return@post
            }

            val request = call.receive<CalloutRecordRequest>()

            val record = calloutService.recordCallout(
                emergencyId = emergencyId,
                crewMembers = request.crewMembers,
                incidentDateTime = request.incidentDateTime,
                incidentLocation = request.incidentLocation,
                actionsTaken = request.actionsTaken,
                outcome = request.outcome,
                durationMinutes = request.durationMinutes
            )

            if (record != null) {
                call.respond(HttpStatusCode.Created, record)
            } else {
                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "All call-out fields must be valid")
                )
            }
        }

        get("/hospitals/{hospitalId}/emergencies/{emergencyId}/callout") {

            val hospitalId = call.parameters["hospitalId"]
            val emergencyId = call.parameters["emergencyId"]

            if (hospitalId == null || emergencyId == null) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Hospital ID and emergency ID are required")
                )
                return@get
            }

            val emergency = emergencyRepository.findById(emergencyId)

            if (emergency == null) {
                call.respond(
                    HttpStatusCode.NotFound,
                    mapOf("error" to "Emergency not found")
                )
                return@get
            }

            if (emergency.allocatedHospitalId != hospitalId) {
                call.respond(
                    HttpStatusCode.Forbidden,
                    mapOf("error" to "This hospital is not assigned to the emergency")
                )
                return@get
            }

            val record = calloutService.getCallout(emergencyId)

            if (record == null) {
                call.respond(
                    HttpStatusCode.NotFound,
                    mapOf("error" to "Call-out record not found")
                )
                return@get
            }

            call.respond(HttpStatusCode.OK, record)
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



    put("/hospitals/{hospitalId}/patients/{nhsNumber}") {
        val hospitalId = call.parameters["hospitalId"]
        val nhsNumber = call.parameters["nhsNumber"]

        if (hospitalId == null || nhsNumber == null) {
            call.respond(HttpStatusCode.BadRequest, "Missing hospital ID or NHS number")
            return@put
        }

        val request = call.receive<PatientRecordUpdateRequest>()

        if (request.medicalCondition.isBlank()) {
            call.respond(HttpStatusCode.BadRequest, "Medical condition cannot be empty")
            return@put
        }

        val hospitalExists = HospitalData.hospitals.any {
            it.hospitalId == hospitalId
        }

        if (!hospitalExists) {
            call.respond(HttpStatusCode.NotFound, "Hospital not found")
            return@put
        }
        val patientExists = patientRepository.findByNhsNumber(nhsNumber)

        if (patientExists == null) {
            call.respond(HttpStatusCode.NotFound, "Patient not found")
            return@put
        }

        val hasCompletedEmergency = emergencyRepository
            .findByHospitalId(hospitalId)
            .any {
                it.patientNhsNumber == nhsNumber && it.status == "COMPLETED"
            }

        if (!hasCompletedEmergency) {
            call.respond(
                HttpStatusCode.Forbidden,
                "This hospital is not authorised to update this patient's record"
            )
            return@put
        }

        val updatedPatient = patientRepository.updateMedicalCondition(
            nhsNumber,
            request.medicalCondition.trim()
        )

        if (updatedPatient == null) {
            call.respond(HttpStatusCode.NotFound, "Patient not found")
            return@put
        }

        call.respond(HttpStatusCode.OK, updatedPatient)
    }
    }
}

