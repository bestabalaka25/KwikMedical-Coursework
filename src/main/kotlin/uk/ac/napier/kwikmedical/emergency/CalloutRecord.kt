package uk.ac.napier.kwikmedical.emergency

import kotlinx.serialization.Serializable

@Serializable
data class CalloutRecord(
    val calloutId: String,
    val emergencyId: String,
    val crewMembers: String,
    val incidentDateTime: String,
    val incidentLocation: String,
    val actionsTaken: String,
    val outcome: String,
    val durationMinutes: Int
)