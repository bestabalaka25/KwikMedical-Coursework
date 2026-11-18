
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
        val allocation = hospitalAllocationService.allocateHospital()
            ?: return null

        val hospital = allocation.first
        val ambulance = allocation.second

        // 3. Create emergency record
        val emergency = Emergency(
            emergencyId = UUID.randomUUID().toString(),
            patientNhsNumber = patient.nhsNumber,
            incidentAddress = incidentAddress,
            description = description,
            status = "ALLOCATED",
            allocatedHospitalId = hospital.hospitalId,
            allocatedAmbulanceId = ambulance.ambulanceId
        )

        // 4. Save emergency to database

        return try {
            emergencyRepository.save(emergency)
        } catch (exception: Exception) {
            ambulanceAllocationService.releaseAmbulance(
                ambulance.ambulanceId
            )
            throw exception
        }

    }


    fun completeEmergency(emergencyId: String): Boolean {
        val emergency = emergencyRepository.findById(emergencyId)
            ?: return false

        if (emergency.status != "ARRIVED") {
            return false
        }

        return emergencyRepository.completeAndReleaseAmbulance(
            emergencyId
        )
    }

}
