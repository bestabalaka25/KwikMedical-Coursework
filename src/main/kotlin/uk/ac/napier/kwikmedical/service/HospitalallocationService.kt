
package uk.ac.napier.kwikmedical.service

import uk.ac.napier.kwikmedical.hospital.Hospital
import uk.ac.napier.kwikmedical.hospital.HospitalData
import uk.ac.napier.kwikmedical.repository.AmbulanceRepository

class HospitalAllocationService {

    private val ambulanceRepository = AmbulanceRepository()

    fun selectHospital(): Hospital? {
        return HospitalData.hospitals.firstOrNull { hospital ->
            ambulanceRepository.hasAvailableForHospital(hospital.hospitalId)
        }
    }
}
