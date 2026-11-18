
package uk.ac.napier.kwikmedical.service

import uk.ac.napier.kwikmedical.emergency.Emergency
import uk.ac.napier.kwikmedical.repository.PatientRepository
import uk.ac.napier.kwikmedical.repository.EmergencyRepository
import java.util.UUID

class EmergencyService {

    private val patientRepository = PatientRepository()
    private val emergencyRepository = EmergencyRepository()
    private val hospitalAllocationService = HospitalAllocationService()
    private val ambulanceAllocationService = AmbulanceAllocationService()

    fun createEmergency(
        patientNhsNumber: String,
        incidentAddress: String,
        description: String
    ): Emergency? {

        // 1. Validate patient
        val patient = patientRepository.findByNhsNumber(patientNhsNumber)
            ?: return null

        // 2. Allocate hospital
        val hospital = hospitalAllocationService.selectHospital()
            ?: return null

        // 3. Allocate ambulance belonging to that hospital
        val ambulance = ambulanceAllocationService.selectAmbulance(
            hospital.hospitalId
        ) ?: return null

        // 4. Create emergency record
        val emergency = Emergency(
            emergencyId = UUID.randomUUID().toString(),
            patientNhsNumber = patient.nhsNumber,
            incidentAddress = incidentAddress,
            description = description,
            status = "ALLOCATED",
            allocatedHospitalId = hospital.hospitalId,
            allocatedAmbulanceId = ambulance.ambulanceId
        )

        // 5. Save emergency to database
        return emergencyRepository.save(emergency)
    }

    fun completeEmergency(emergencyId: String): Boolean {
        val emergency = emergencyRepository.findById(emergencyId)
            ?: return false

        if (emergency.status == "COMPLETED") {
            return false
        }

        val updated = emergencyRepository.updateStatus(
            emergencyId,
            "COMPLETED"
        )

        if (!updated) {
            return false
        }

        emergency.allocatedAmbulanceId?.let { ambulanceId ->
            ambulanceAllocationService.releaseAmbulance(ambulanceId)
        }

        return true
    }
}
