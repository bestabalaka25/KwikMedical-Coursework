package uk.ac.napier.kwikmedical.service

import uk.ac.napier.kwikmedical.emergency.CalloutRecord
import uk.ac.napier.kwikmedical.repository.CalloutRepository
import java.util.UUID
import java.time.LocalDateTime
import java.time.format.DateTimeParseException


class CalloutService {

    private val calloutRepository = CalloutRepository()
    private fun isValidDateTime(value: String): Boolean {
        return try {
            LocalDateTime.parse(value)
            true
        } catch (e: DateTimeParseException) {
            false
        }
    }


    fun recordCallout(
        emergencyId: String,
        crewMembers: String,
        incidentDateTime: String,
        incidentLocation: String,
        actionsTaken: String,
        outcome: String,
        durationMinutes: Int
    ): CalloutRecord? {

        if (
            crewMembers.isBlank() ||
            !isValidDateTime(incidentDateTime) ||
            incidentLocation.isBlank() ||
            actionsTaken.isBlank() ||
            outcome.isBlank() ||
            durationMinutes < 0
        ) {
            return null
        }

        val record = CalloutRecord(
            calloutId = UUID.randomUUID().toString(),
            emergencyId = emergencyId,
            crewMembers = crewMembers.trim(),
            incidentDateTime = incidentDateTime.trim(),
            incidentLocation = incidentLocation.trim(),
            actionsTaken = actionsTaken.trim(),
            outcome = outcome.trim(),
            durationMinutes = durationMinutes
        )

        return calloutRepository.save(record)
    }

    fun getCallout(emergencyId: String): CalloutRecord? {
        return calloutRepository.findByEmergencyId(emergencyId)
    }
}