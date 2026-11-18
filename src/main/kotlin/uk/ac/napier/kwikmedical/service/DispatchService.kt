
package uk.ac.napier.kwikmedical.service

import uk.ac.napier.kwikmedical.emergency.Emergency
import uk.ac.napier.kwikmedical.repository.EmergencyRepository
import uk.ac.napier.kwikmedical.repository.PatientRepository

class DispatchService {

    private val emergencyRepository = EmergencyRepository()
    private val patientRepository = PatientRepository()

    data class DispatchDetails(
        val emergency: Emergency,
        val patient: uk.ac.napier.kwikmedical.Patient
    )

    fun getDispatchDetails(emergencyId: String): DispatchDetails? {
        val emergency = emergencyRepository.findById(emergencyId)
            ?: return null

        val patient = patientRepository.findByNhsNumber(
            emergency.patientNhsNumber
        ) ?: return null

        return DispatchDetails(
            emergency = emergency,
            patient = patient
        )
    }
}
