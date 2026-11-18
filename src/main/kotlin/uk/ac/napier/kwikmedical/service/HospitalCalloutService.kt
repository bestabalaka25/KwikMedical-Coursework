
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

        val emergency = emergencyRepository.findById(emergencyId)
            ?: return false

        if (emergency.allocatedHospitalId != hospitalId) {
            return false
        }

        val expectedNextStatus = allowedTransitions[emergency.status]
            ?: return false

        if (newStatus != expectedNextStatus) {
            return false
        }

        if (newStatus == "COMPLETED") {
            return emergencyService.completeEmergency(emergencyId)
        }

        return emergencyRepository.updateStatus(
            emergencyId,
            newStatus
        )
    }
}
