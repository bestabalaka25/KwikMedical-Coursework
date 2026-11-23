
package uk.ac.napier.kwikmedical.ambulance

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.call.body
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import uk.ac.napier.kwikmedical.DispatchResponse
import uk.ac.napier.kwikmedical.emergency.CalloutRecord
import uk.ac.napier.kwikmedical.hospital.CalloutRecordRequest
class AmbulanceSmartphoneClient(
    baseUrl: String = "http://localhost:8080",
    private val client: HttpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                }
            )
        }
    }
) {

    private val baseUrl = baseUrl.trimEnd('/')

    private fun endpoint(path: String): String {
        require(path.startsWith("/")) {
            "Endpoint path must start with /"
        }

        return if (this.baseUrl.isBlank()) {
            path
        } else {
            "${this.baseUrl}$path"
        }
    }

    // Receive emergency dispatch and patient information
    suspend fun receiveDispatch(
        emergencyId: String
    ): DispatchResponse {
        require(emergencyId.isNotBlank()) {
            "Emergency ID cannot be empty"
        }

        return client.get(
            endpoint("/dispatch/$emergencyId")
        ).body()
    }

    // Submit the completed ambulance call-out record
    suspend fun submitCallout(
        hospitalId: String,
        emergencyId: String,
        request: CalloutRecordRequest
    ): CalloutRecord {
        require(hospitalId.isNotBlank()) {
            "Hospital ID cannot be empty"
        }

        require(emergencyId.isNotBlank()) {
            "Emergency ID cannot be empty"
        }

        return client.post(
            endpoint("/hospitals/$hospitalId/emergencies/$emergencyId/callout")
        ) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    // Retrieve a completed ambulance call-out record
    suspend fun retrieveCallout(
        hospitalId: String,
        emergencyId: String
    ): CalloutRecord {
        require(hospitalId.isNotBlank()) {
            "Hospital ID cannot be empty"
        }

        require(emergencyId.isNotBlank()) {
            "Emergency ID cannot be empty"
        }

        return client.get(
            endpoint("/hospitals/$hospitalId/emergencies/$emergencyId/callout")
        ).body()
    }

    // Close the HTTP client when it is no longer needed
    fun close() {
        client.close()
    }
}

