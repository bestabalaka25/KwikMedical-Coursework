
package uk.ac.napier.kwikmedical.hospital

import kotlinx.serialization.Serializable

@Serializable
data class Hospital(
    val hospitalId: String,
    val name: String,
    val address: String,
    val available: Boolean = true
)
