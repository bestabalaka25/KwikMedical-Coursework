
package uk.ac.napier.kwikmedical.ambulance

import kotlinx.serialization.Serializable

@Serializable
data class Ambulance(
    val ambulanceId: String,
    val hospitalId: String,
    val vehicleCallSign: String,
    val available: Boolean = true
)
