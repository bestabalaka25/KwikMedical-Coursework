
package uk.ac.napier.kwikmedical.service

import uk.ac.napier.kwikmedical.emergency.CalloutRecord
import uk.ac.napier.kwikmedical.repository.CalloutRepository
import java.time.LocalDateTime
import java.time.format.DateTimeParseException
import java.util.UUID

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

        // Normalise input by removing surrounding whitespace
        val cleanCrewMembers = crewMembers.trim()
        val cleanDateTime = incidentDateTime.trim()
        val cleanLocation = incidentLocation.trim()
        val cleanActions = actionsTaken.trim()
        val cleanOutcome = outcome.trim()

        // Validate input
        if (
            emergencyId.isBlank() ||
            cleanCrewMembers.isBlank() ||
            !isValidDateTime(cleanDateTime) ||
            cleanLocation.isBlank() ||
            cleanActions.isBlank() ||
            cleanOutcome.isBlank() ||
            durationMinutes < 0
        ) {
            return null
        }

        // Create call-out record
        val record = CalloutRecord(
            calloutId = UUID.randomUUID().toString(),
            emergencyId = emergencyId,
            crewMembers = cleanCrewMembers,
            incidentDateTime = cleanDateTime,
            incidentLocation = cleanLocation,
            actionsTaken = cleanActions,
            outcome = cleanOutcome,
            durationMinutes = durationMinutes
        )

        return calloutRepository.save(record)
    }

    fun getCallout(emergencyId: String): CalloutRecord? {
        if (emergencyId.isBlank()) {
            return null
        }

        return calloutRepository.findByEmergencyId(emergencyId)
    }
}
