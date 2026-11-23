
package uk.ac.napier.kwikmedical.service

import uk.ac.napier.kwikmedical.repository.EmergencyRepository

class HospitalCalloutService {

    private val emergencyRepository = EmergencyRepository()
    private val emergencyService = EmergencyService()

    private val allowedTransitions = mapOf(
        "ALLOCATED" to "EN_ROUTE",
        "EN_ROUTE" to "ARRIVED",
        "ARRIVED" to "COMPLETED"
    )

    fun updateCalloutStatus(
        hospitalId: String,
        emergencyId: String,
        newStatus: String
    ): Boolean {

        // Validate input
        if (
            hospitalId.isBlank() ||
            emergencyId.isBlank() ||
            newStatus.isBlank()
        ) {
            return false
        }

        // Retrieve emergency
        val emergency = emergencyRepository.findById(emergencyId)
            ?: return false

        // Check hospital ownership
        if (emergency.allocatedHospitalId != hospitalId) {
            return false
        }

        // Determine the next permitted status
        val expectedNextStatus = allowedTransitions[emergency.status]
            ?: return false

        // Reject invalid transitions
        if (newStatus != expectedNextStatus) {
            return false
        }

        // Complete emergency and release ambulance
        if (newStatus == "COMPLETED") {
            return emergencyService.completeEmergency(emergencyId)
        }

        // Update emergency status
        return emergencyRepository.updateStatus(
            emergencyId,
            emergency.status,
            newStatus
        )
    }
}
