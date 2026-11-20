package uk.ac.napier.kwikmedical.hospital

import kotlinx.serialization.Serializable

@Serializable
data class CalloutRecordRequest(
    val crewMembers: String,
    val incidentDateTime: String,
    val incidentLocation: String,
    val actionsTaken: String,
    val outcome: String,
    val durationMinutes: Int
)