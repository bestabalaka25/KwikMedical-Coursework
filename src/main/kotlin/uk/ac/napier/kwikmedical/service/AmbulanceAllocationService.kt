
package uk.ac.napier.kwikmedical.service

import uk.ac.napier.kwikmedical.ambulance.Ambulance
import uk.ac.napier.kwikmedical.repository.AmbulanceRepository

class AmbulanceAllocationService {

    private val ambulanceRepository = AmbulanceRepository()

    fun selectAmbulance(hospitalId: String): Ambulance? {
        return ambulanceRepository.reserveAvailableForHospital(hospitalId)
    }
    fun releaseAmbulance(ambulanceId: String): Boolean {
        return ambulanceRepository.release(ambulanceId)
    }
}
