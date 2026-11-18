
package uk.ac.napier.kwikmedical.emergency

import kotlinx.serialization.Serializable

@Serializable
data class Emergency(
    val emergencyId: String,
    val patientNhsNumber: String,
    val incidentAddress: String,
    val description: String,
    val status: String = "PENDING",
    val allocatedHospitalId: String? = null,
    val allocatedAmbulanceId: String? = null
)
