
package uk.ac.napier.kwikmedical.service

import uk.ac.napier.kwikmedical.ambulance.Ambulance
import uk.ac.napier.kwikmedical.hospital.Hospital
import uk.ac.napier.kwikmedical.hospital.HospitalData

class HospitalAllocationService {

    private val ambulanceAllocationService =
        AmbulanceAllocationService()

    fun allocateHospital(): Pair<Hospital, Ambulance>? {

        for (hospital in HospitalData.hospitals) {

            val ambulance = ambulanceAllocationService.selectAmbulance(
                hospital.hospitalId
            ) ?: continue

            return Pair(hospital, ambulance)
        }

        return null
    }
}
