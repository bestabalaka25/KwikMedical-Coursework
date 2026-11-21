
package uk.ac.napier.kwikmedical.hospital

import kotlinx.serialization.Serializable

@Serializable
data class PatientRecordUpdateRequest(
    val medicalCondition: String
)
