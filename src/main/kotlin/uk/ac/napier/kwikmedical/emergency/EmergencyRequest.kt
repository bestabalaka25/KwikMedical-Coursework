
package uk.ac.napier.kwikmedical.emergency

import kotlinx.serialization.Serializable

@Serializable
data class EmergencyRequest(
    val patientNhsNumber: String,
    val incidentAddress: String,
    val description: String
)
