
package uk.ac.napier.kwikmedical.hospital

import kotlinx.serialization.Serializable

@Serializable
data class HospitalCalloutRequest(
    val status: String
)
