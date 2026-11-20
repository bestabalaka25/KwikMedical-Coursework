
package uk.ac.napier.kwikmedical

import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlin.test.*
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.json.Json
import uk.ac.napier.kwikmedical.emergency.Emergency
import kotlinx.serialization.decodeFromString
import io.ktor.client.request.put
import uk.ac.napier.kwikmedical.repository.AmbulanceRepository
import uk.ac.napier.kwikmedical.service.AmbulanceAllocationService
import kotlinx.coroutines.runBlocking
import uk.ac.napier.kwikmedical.ambulance.AmbulanceSmartphoneClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import uk.ac.napier.kwikmedical.hospital.CalloutRecordRequest

private fun configureTestDatabase() {
    System.setProperty(
        "kwikmedical.database.url",
        "jdbc:h2:mem:testdb-${java.util.UUID.randomUUID()};DB_CLOSE_DELAY=-1"
    )
}

class ServerTest {

    @Test
    fun `test root endpoint`() = testApplication {
        configureTestDatabase()
        application {
            rootModule()
        }

        assertEquals(HttpStatusCode.OK, client.get("/").status)
    }

    @Test
    fun `test get all patients`() = testApplication {
        configureTestDatabase()
        application {
            rootModule()
        }

        val response = client.get("/patients")

        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `test get existing patient`() = testApplication {
        configureTestDatabase()
        application {
            rootModule()
        }

        val response = client.get("/patients/NHS001")

        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `test get unknown patient`() = testApplication {
        configureTestDatabase()
        application {
            rootModule()
        }

        val response = client.get("/patients/UNKNOWN")

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `test create emergency successfully`() = testApplication {
        configureTestDatabase()

        application {
            rootModule()
        }

        val response = client.post("/emergencies") {
            contentType(ContentType.Application.Json)
            setBody(
                """
            {
                "patientNhsNumber": "NHS001",
                "incidentAddress": "15 Example Street, Edinburgh",
                "description": "Patient requires urgent medical assistance"
            }
            """.trimIndent()
            )
        }

        assertEquals(HttpStatusCode.Created, response.status)

        val emergency = Json.decodeFromString<Emergency>(
            response.bodyAsText()
        )

        assertEquals("NHS001", emergency.patientNhsNumber)
        assertEquals("ALLOCATED", emergency.status)
        assertNotNull(emergency.allocatedHospitalId)
        assertNotNull(emergency.allocatedAmbulanceId)
    }
    @Test
    fun `test create emergency for unknown patient`() = testApplication {
        configureTestDatabase()

        application {
            rootModule()
        }

        val response = client.post("/emergencies") {
            contentType(ContentType.Application.Json)
            setBody(
                """
            {
                "patientNhsNumber": "UNKNOWN",
                "incidentAddress": "15 Example Street, Edinburgh",
                "description": "Patient requires urgent medical assistance"
            }
            """.trimIndent()
            )
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }
    @Test
    fun `test create emergency with missing address`() = testApplication {
        configureTestDatabase()

        application {
            rootModule()
        }

        val response = client.post("/emergencies") {
            contentType(ContentType.Application.Json)
            setBody(
                """
            {
                "patientNhsNumber": "NHS001",
                "incidentAddress": " ",
                "description": "Patient requires urgent medical assistance"
            }
            """.trimIndent()
            )
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }
    @Test
    fun `test create emergency with missing description`() = testApplication {
        configureTestDatabase()

        application {
            rootModule()
        }

        val response = client.post("/emergencies") {
            contentType(ContentType.Application.Json)
            setBody(
                """
            {
                "patientNhsNumber": "NHS001",
                "incidentAddress": "15 Example Street, Edinburgh",
                "description": " "
            }
            """.trimIndent()
            )
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }
    @Test
    fun `test create emergency with missing patient number`() = testApplication {
        configureTestDatabase()

        application {
            rootModule()
        }

        val response = client.post("/emergencies") {
            contentType(ContentType.Application.Json)
            setBody(
                """
            {
                "patientNhsNumber": " ",
                "incidentAddress": "15 Example Street, Edinburgh",
                "description": "Patient requires urgent medical assistance"
            }
            """.trimIndent()
            )
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }
    @Test
    fun `test reject invalid emergency status transition`() = testApplication {
        configureTestDatabase()

        application {
            rootModule()
        }

        val createResponse = client.post("/emergencies") {
            contentType(ContentType.Application.Json)
            setBody(
                """
            {
                "patientNhsNumber": "NHS001",
                "incidentAddress": "15 Example Street, Edinburgh",
                "description": "Urgent medical assistance required"
            }
            """.trimIndent()
            )
        }

        assertEquals(HttpStatusCode.Created, createResponse.status)

        val emergency = Json.decodeFromString<Emergency>(
            createResponse.bodyAsText()
        )

        val response = client.put(
            "/hospitals/${emergency.allocatedHospitalId}/emergencies/${emergency.emergencyId}/status"
        ) {
            contentType(ContentType.Application.Json)
            setBody("""{"status":"COMPLETED"}""")
        }

        assertEquals(HttpStatusCode.Conflict, response.status)
    }
    @Test
    fun `test reject status update from wrong hospital`() = testApplication {
        configureTestDatabase()

        application {
            rootModule()
        }

        val createResponse = client.post("/emergencies") {
            contentType(ContentType.Application.Json)
            setBody(
                """
            {
                "patientNhsNumber": "NHS001",
                "incidentAddress": "15 Example Street, Edinburgh",
                "description": "Urgent medical assistance required"
            }
            """.trimIndent()
            )
        }

        assertEquals(HttpStatusCode.Created, createResponse.status)

        val emergency = Json.decodeFromString<Emergency>(
            createResponse.bodyAsText()
        )

        val response = client.put(
            "/hospitals/H020/emergencies/${emergency.emergencyId}/status"
        ) {
            contentType(ContentType.Application.Json)
            setBody("""{"status":"EN_ROUTE"}""")
        }

        assertEquals(HttpStatusCode.Conflict, response.status)
    }
    @Test
    fun `test valid emergency status update`() = testApplication {
        configureTestDatabase()

        application {
            rootModule()
        }

        val createResponse = client.post("/emergencies") {
            contentType(ContentType.Application.Json)
            setBody(
                """
            {
                "patientNhsNumber": "NHS001",
                "incidentAddress": "15 Example Street, Edinburgh",
                "description": "Urgent medical assistance required"
            }
            """.trimIndent()
            )
        }

        assertEquals(HttpStatusCode.Created, createResponse.status)

        val emergency = Json.decodeFromString<Emergency>(
            createResponse.bodyAsText()
        )

        val response = client.put(
            "/hospitals/${emergency.allocatedHospitalId}/emergencies/${emergency.emergencyId}/status"
        ) {
            contentType(ContentType.Application.Json)
            setBody("""{"status":"EN_ROUTE"}""")
        }

        assertEquals(HttpStatusCode.OK, response.status)

        val getResponse = client.get(
            "/emergencies/${emergency.emergencyId}"
        )

        assertEquals(HttpStatusCode.OK, getResponse.status)

        val updatedEmergency = Json.decodeFromString<Emergency>(
            getResponse.bodyAsText()
        )

        assertEquals("EN_ROUTE", updatedEmergency.status)
    }
    @Test
    fun `test emergency can transition to arrived`() = testApplication {
        configureTestDatabase()

        application {
            rootModule()
        }

        val createResponse = client.post("/emergencies") {
            contentType(ContentType.Application.Json)
            setBody(
                """
            {
                "patientNhsNumber": "NHS001",
                "incidentAddress": "15 Example Street, Edinburgh",
                "description": "Urgent medical assistance required"
            }
            """.trimIndent()
            )
        }

        assertEquals(HttpStatusCode.Created, createResponse.status)

        val emergency = Json.decodeFromString<Emergency>(
            createResponse.bodyAsText()
        )

        val hospitalUrl =
            "/hospitals/${emergency.allocatedHospitalId}/emergencies/${emergency.emergencyId}/status"

        // First transition: ALLOCATED -> EN_ROUTE
        val enRouteResponse = client.put(hospitalUrl) {
            contentType(ContentType.Application.Json)
            setBody("""{"status":"EN_ROUTE"}""")
        }

        assertEquals(HttpStatusCode.OK, enRouteResponse.status)

        // Second transition: EN_ROUTE -> ARRIVED
        val arrivedResponse = client.put(hospitalUrl) {
            contentType(ContentType.Application.Json)
            setBody("""{"status":"ARRIVED"}""")
        }

        assertEquals(HttpStatusCode.OK, arrivedResponse.status)

        val getResponse = client.get(
            "/emergencies/${emergency.emergencyId}"
        )

        val updatedEmergency = Json.decodeFromString<Emergency>(
            getResponse.bodyAsText()
        )

        assertEquals("ARRIVED", updatedEmergency.status)
    }
    @Test
    fun `test emergency can be completed successfully`() = testApplication {
        configureTestDatabase()

        application {
            rootModule()
        }

        val createResponse = client.post("/emergencies") {
            contentType(ContentType.Application.Json)
            setBody(
                """
            {
                "patientNhsNumber": "NHS001",
                "incidentAddress": "15 Example Street, Edinburgh",
                "description": "Urgent medical assistance required"
            }
            """.trimIndent()
            )
        }

        assertEquals(HttpStatusCode.Created, createResponse.status)

        val emergency = Json.decodeFromString<Emergency>(
            createResponse.bodyAsText()
        )

        val hospitalUrl =
            "/hospitals/${emergency.allocatedHospitalId}/emergencies/${emergency.emergencyId}/status"

        // ALLOCATED -> EN_ROUTE
        val enRouteResponse = client.put(hospitalUrl) {
            contentType(ContentType.Application.Json)
            setBody("""{"status":"EN_ROUTE"}""")
        }
        assertEquals(HttpStatusCode.OK, enRouteResponse.status)

        // EN_ROUTE -> ARRIVED
        val arrivedResponse = client.put(hospitalUrl) {
            contentType(ContentType.Application.Json)
            setBody("""{"status":"ARRIVED"}""")
        }
        assertEquals(HttpStatusCode.OK, arrivedResponse.status)

        // ARRIVED -> COMPLETED
        val completedResponse = client.put(hospitalUrl) {
            contentType(ContentType.Application.Json)
            setBody("""{"status":"COMPLETED"}""")
        }
        assertEquals(HttpStatusCode.OK, completedResponse.status)

        // Verify final status
        val getResponse = client.get(
            "/emergencies/${emergency.emergencyId}"
        )

        val completedEmergency = Json.decodeFromString<Emergency>(
            getResponse.bodyAsText()
        )

        assertEquals("COMPLETED", completedEmergency.status)
    }
    @Test
    fun `test ambulance is released after emergency completion`() = testApplication {
        configureTestDatabase()

        application {
            rootModule()
        }

        val ambulanceRepository = AmbulanceRepository()

        // Create emergency
        val createResponse = client.post("/emergencies") {
            contentType(ContentType.Application.Json)
            setBody(
                """
            {
                "patientNhsNumber": "NHS001",
                "incidentAddress": "15 Example Street, Edinburgh",
                "description": "Urgent medical assistance required"
            }
            """.trimIndent()
            )
        }

        assertEquals(HttpStatusCode.Created, createResponse.status)

        val emergency = Json.decodeFromString<Emergency>(
            createResponse.bodyAsText()
        )

        val ambulanceId = emergency.allocatedAmbulanceId!!

        // Check ambulance is initially unavailable
        var ambulance = ambulanceRepository.findAll()
            .first { it.ambulanceId == ambulanceId }

        assertFalse(ambulance.available)

        val hospitalUrl =
            "/hospitals/${emergency.allocatedHospitalId}/emergencies/${emergency.emergencyId}/status"

        // ALLOCATED -> EN_ROUTE
        val enRouteResponse = client.put(hospitalUrl) {
            contentType(ContentType.Application.Json)
            setBody("""{"status":"EN_ROUTE"}""")
        }

        assertEquals(HttpStatusCode.OK, enRouteResponse.status)

        // EN_ROUTE -> ARRIVED
        val arrivedResponse = client.put(hospitalUrl) {
            contentType(ContentType.Application.Json)
            setBody("""{"status":"ARRIVED"}""")
        }

        assertEquals(HttpStatusCode.OK, arrivedResponse.status)

        // ARRIVED -> COMPLETED
        val completedResponse = client.put(hospitalUrl) {
            contentType(ContentType.Application.Json)
            setBody("""{"status":"COMPLETED"}""")
        }

        assertEquals(HttpStatusCode.OK, completedResponse.status)

        // Verify ambulance is available again
        ambulance = ambulanceRepository.findAll()
            .first { it.ambulanceId == ambulanceId }

        assertTrue(ambulance.available)
    }
    @Test
    fun `test reject emergency when all ambulances are occupied`() = testApplication {
        configureTestDatabase()

        application {
            rootModule()
        }

        val ambulanceRepository = AmbulanceRepository()

        // Allocate all 20 ambulances
        repeat(20) {
            val response = client.post("/emergencies") {
                contentType(ContentType.Application.Json)
                setBody(
                    """
                {
                    "patientNhsNumber": "NHS001",
                    "incidentAddress": "15 Example Street, Edinburgh",
                    "description": "Urgent medical assistance required"
                }
                """.trimIndent()
                )
            }

            assertEquals(HttpStatusCode.Created, response.status)
        }

        // Confirm all 20 ambulances are unavailable
        val ambulances = ambulanceRepository.findAll()

        assertEquals(20, ambulances.size)
        assertTrue(ambulances.none { it.available })

        // Attempt to create a 21st emergency
        val response = client.post("/emergencies") {
            contentType(ContentType.Application.Json)
            setBody(
                """
            {
                "patientNhsNumber": "NHS001",
                "incidentAddress": "25 Example Road, Edinburgh",
                "description": "Another urgent medical emergency"
            }
            """.trimIndent()
            )
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `test create callout record for completed emergency`() = testApplication {
        configureTestDatabase()
        application { rootModule() }

        // Create an emergency
        val emergencyResponse = client.post("/emergencies") {
            contentType(ContentType.Application.Json)
            setBody(
                """
            {
                "patientNhsNumber": "NHS001",
                "incidentAddress": "15 Example Street, Edinburgh",
                "description": "Patient requires urgent medical assistance"
            }
            """.trimIndent()
            )
        }

        assertEquals(HttpStatusCode.Created, emergencyResponse.status)

        val emergency = Json.decodeFromString<Emergency>(
            emergencyResponse.bodyAsText()
        )

        val hospitalId = emergency.allocatedHospitalId!!
        val emergencyId = emergency.emergencyId

        // Complete the emergency lifecycle
        for (status in listOf("EN_ROUTE", "ARRIVED", "COMPLETED")) {
            val response = client.put(
                "/hospitals/$hospitalId/emergencies/$emergencyId/status"
            ) {
                contentType(ContentType.Application.Json)
                setBody("""{"status":"$status"}""")
            }

            assertEquals(HttpStatusCode.OK, response.status)
        }

        // Submit the call-out record
        val calloutResponse = client.post(
            "/hospitals/$hospitalId/emergencies/$emergencyId/callout"
        ) {
            contentType(ContentType.Application.Json)
            setBody(
                """
            {
                "crewMembers": "Paramedic A, Paramedic B",
                "incidentDateTime": "2026-11-20T10:30:00",
                "incidentLocation": "15 Example Street, Edinburgh",
                "actionsTaken": "Initial assessment and treatment provided",
                "outcome": "Patient stabilised and transported to hospital",
                "durationMinutes": 45
            }
            """.trimIndent()
            )
        }

        assertEquals(HttpStatusCode.Created, calloutResponse.status)
    }

    @Test
    fun `test reject duplicate callout record for emergency`() = testApplication {
        configureTestDatabase()
        application { rootModule() }

        // Create an emergency
        val emergencyResponse = client.post("/emergencies") {
            contentType(ContentType.Application.Json)
            setBody(
                """
            {
                "patientNhsNumber": "NHS001",
                "incidentAddress": "15 Example Street, Edinburgh",
                "description": "Urgent medical assistance required"
            }
            """.trimIndent()
            )
        }

        assertEquals(HttpStatusCode.Created, emergencyResponse.status)

        val emergency = Json.decodeFromString<Emergency>(
            emergencyResponse.bodyAsText()
        )

        val hospitalId = emergency.allocatedHospitalId!!
        val emergencyId = emergency.emergencyId

        // Complete the emergency lifecycle
        for (status in listOf("EN_ROUTE", "ARRIVED", "COMPLETED")) {
            val response = client.put(
                "/hospitals/$hospitalId/emergencies/$emergencyId/status"
            ) {
                contentType(ContentType.Application.Json)
                setBody("""{"status":"$status"}""")
            }

            assertEquals(HttpStatusCode.OK, response.status)
        }

        val calloutUrl =
            "/hospitals/$hospitalId/emergencies/$emergencyId/callout"

        val calloutBody =
            """
        {
            "crewMembers": "Paramedic A, Paramedic B",
            "incidentDateTime": "2026-11-20T10:30:00",
            "incidentLocation": "15 Example Street, Edinburgh",
            "actionsTaken": "Initial assessment and treatment provided",
            "outcome": "Patient stabilised and transported to hospital",
            "durationMinutes": 45
        }
        """.trimIndent()

        // First submission should succeed
        val firstResponse = client.post(calloutUrl) {
            contentType(ContentType.Application.Json)
            setBody(calloutBody)
        }

        assertEquals(HttpStatusCode.Created, firstResponse.status)

        // Second submission should be rejected
        val secondResponse = client.post(calloutUrl) {
            contentType(ContentType.Application.Json)
            setBody(calloutBody)
        }

        assertEquals(HttpStatusCode.Conflict, secondResponse.status)
    }

    @Test
    fun `test retrieve saved callout record`() = testApplication {
        configureTestDatabase()
        application { rootModule() }

        // Create an emergency
        val emergencyResponse = client.post("/emergencies") {
            contentType(ContentType.Application.Json)
            setBody(
                """
            {
                "patientNhsNumber": "NHS001",
                "incidentAddress": "15 Example Street, Edinburgh",
                "description": "Urgent medical assistance required"
            }
            """.trimIndent()
            )
        }

        assertEquals(HttpStatusCode.Created, emergencyResponse.status)

        val emergency = Json.decodeFromString<Emergency>(
            emergencyResponse.bodyAsText()
        )

        val hospitalId = emergency.allocatedHospitalId!!
        val emergencyId = emergency.emergencyId

        // Complete the emergency lifecycle
        for (status in listOf("EN_ROUTE", "ARRIVED", "COMPLETED")) {
            val response = client.put(
                "/hospitals/$hospitalId/emergencies/$emergencyId/status"
            ) {
                contentType(ContentType.Application.Json)
                setBody("""{"status":"$status"}""")
            }

            assertEquals(HttpStatusCode.OK, response.status)
        }

        // Save a call-out record
        val calloutUrl =
            "/hospitals/$hospitalId/emergencies/$emergencyId/callout"

        val createResponse = client.post(calloutUrl) {
            contentType(ContentType.Application.Json)
            setBody(
                """
            {
                "crewMembers": "Paramedic A, Paramedic B",
                "incidentDateTime": "2026-11-20T10:30:00",
                "incidentLocation": "15 Example Street, Edinburgh",
                "actionsTaken": "Initial assessment and treatment provided",
                "outcome": "Patient stabilised and transported to hospital",
                "durationMinutes": 45
            }
            """.trimIndent()
            )
        }

        assertEquals(HttpStatusCode.Created, createResponse.status)

        // Assigned hospital retrieves the record
        val getResponse = client.get(calloutUrl)

        assertEquals(HttpStatusCode.OK, getResponse.status)
        assertTrue(getResponse.bodyAsText().contains("Paramedic A, Paramedic B"))
        assertTrue(getResponse.bodyAsText().contains("45"))

        // An unrelated hospital must be denied access
        val unauthorizedResponse = client.get(
            "/hospitals/H020/emergencies/$emergencyId/callout"
        )

        assertEquals(HttpStatusCode.Forbidden, unauthorizedResponse.status)
    }

    @Test
    fun `test reject callout record with invalid date time`() = testApplication {
        configureTestDatabase()
        application { rootModule() }

        // Create an emergency
        val emergencyResponse = client.post("/emergencies") {
            contentType(ContentType.Application.Json)
            setBody(
                """
            {
                "patientNhsNumber": "NHS001",
                "incidentAddress": "15 Example Street, Edinburgh",
                "description": "Urgent medical assistance required"
            }
            """.trimIndent()
            )
        }

        assertEquals(HttpStatusCode.Created, emergencyResponse.status)

        val emergency = Json.decodeFromString<Emergency>(
            emergencyResponse.bodyAsText()
        )

        val hospitalId = emergency.allocatedHospitalId!!
        val emergencyId = emergency.emergencyId

        // Complete the emergency lifecycle
        for (status in listOf("EN_ROUTE", "ARRIVED", "COMPLETED")) {
            val response = client.put(
                "/hospitals/$hospitalId/emergencies/$emergencyId/status"
            ) {
                contentType(ContentType.Application.Json)
                setBody("""{"status":"$status"}""")
            }

            assertEquals(HttpStatusCode.OK, response.status)
        }

        // Submit a call-out record with an invalid date-time
        val response = client.post(
            "/hospitals/$hospitalId/emergencies/$emergencyId/callout"
        ) {
            contentType(ContentType.Application.Json)
            setBody(
                """
            {
                "crewMembers": "Paramedic A, Paramedic B",
                "incidentDateTime": "not-a-valid-date",
                "incidentLocation": "15 Example Street, Edinburgh",
                "actionsTaken": "Initial assessment and treatment provided",
                "outcome": "Patient stabilised and transported to hospital",
                "durationMinutes": 45
            }
            """.trimIndent()
            )
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `test reject callout record with negative duration`() = testApplication {
        configureTestDatabase()
        application { rootModule() }

        // Create an emergency
        val emergencyResponse = client.post("/emergencies") {
            contentType(ContentType.Application.Json)
            setBody(
                """
            {
                "patientNhsNumber": "NHS001",
                "incidentAddress": "15 Example Street, Edinburgh",
                "description": "Urgent medical assistance required"
            }
            """.trimIndent()
            )
        }

        assertEquals(HttpStatusCode.Created, emergencyResponse.status)

        val emergency = Json.decodeFromString<Emergency>(
            emergencyResponse.bodyAsText()
        )

        val hospitalId = emergency.allocatedHospitalId!!
        val emergencyId = emergency.emergencyId

        // Complete the emergency lifecycle
        for (status in listOf("EN_ROUTE", "ARRIVED", "COMPLETED")) {
            val response = client.put(
                "/hospitals/$hospitalId/emergencies/$emergencyId/status"
            ) {
                contentType(ContentType.Application.Json)
                setBody("""{"status":"$status"}""")
            }

            assertEquals(HttpStatusCode.OK, response.status)
        }

        // Submit a call-out record with a negative duration
        val response = client.post(
            "/hospitals/$hospitalId/emergencies/$emergencyId/callout"
        ) {
            contentType(ContentType.Application.Json)
            setBody(
                """
            {
                "crewMembers": "Paramedic A, Paramedic B",
                "incidentDateTime": "2026-11-20T10:30:00",
                "incidentLocation": "15 Example Street, Edinburgh",
                "actionsTaken": "Initial assessment and treatment provided",
                "outcome": "Patient stabilised and transported to hospital",
                "durationMinutes": -10
            }
            """.trimIndent()
            )
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `test reject callout record with missing crew members`() = testApplication {
        configureTestDatabase()
        application { rootModule() }

        // Create an emergency
        val emergencyResponse = client.post("/emergencies") {
            contentType(ContentType.Application.Json)
            setBody(
                """
            {
                "patientNhsNumber": "NHS001",
                "incidentAddress": "15 Example Street, Edinburgh",
                "description": "Urgent medical assistance required"
            }
            """.trimIndent()
            )
        }

        assertEquals(HttpStatusCode.Created, emergencyResponse.status)

        val emergency = Json.decodeFromString<Emergency>(
            emergencyResponse.bodyAsText()
        )

        val hospitalId = emergency.allocatedHospitalId!!
        val emergencyId = emergency.emergencyId

        // Complete the emergency lifecycle
        for (status in listOf("EN_ROUTE", "ARRIVED", "COMPLETED")) {
            val response = client.put(
                "/hospitals/$hospitalId/emergencies/$emergencyId/status"
            ) {
                contentType(ContentType.Application.Json)
                setBody("""{"status":"$status"}""")
            }

            assertEquals(HttpStatusCode.OK, response.status)
        }

        // Submit a record without crew members
        val response = client.post(
            "/hospitals/$hospitalId/emergencies/$emergencyId/callout"
        ) {
            contentType(ContentType.Application.Json)
            setBody(
                """
            {
                "crewMembers": "",
                "incidentDateTime": "2026-11-20T10:30:00",
                "incidentLocation": "15 Example Street, Edinburgh",
                "actionsTaken": "Initial assessment and treatment provided",
                "outcome": "Patient stabilised and transported to hospital",
                "durationMinutes": 45
            }
            """.trimIndent()
            )
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `test reject callout record with missing incident location`() = testApplication {
        configureTestDatabase()
        application { rootModule() }

        val emergencyResponse = client.post("/emergencies") {
            contentType(ContentType.Application.Json)
            setBody(
                """
            {
                "patientNhsNumber": "NHS001",
                "incidentAddress": "15 Example Street, Edinburgh",
                "description": "Urgent medical assistance required"
            }
            """.trimIndent()
            )
        }

        assertEquals(HttpStatusCode.Created, emergencyResponse.status)

        val emergency = Json.decodeFromString<Emergency>(
            emergencyResponse.bodyAsText()
        )

        val hospitalId = emergency.allocatedHospitalId!!
        val emergencyId = emergency.emergencyId

        // Complete the emergency lifecycle
        for (status in listOf("EN_ROUTE", "ARRIVED", "COMPLETED")) {
            val response = client.put(
                "/hospitals/$hospitalId/emergencies/$emergencyId/status"
            ) {
                contentType(ContentType.Application.Json)
                setBody("""{"status":"$status"}""")
            }

            assertEquals(HttpStatusCode.OK, response.status)
        }

        // Submit a record without an incident location
        val response = client.post(
            "/hospitals/$hospitalId/emergencies/$emergencyId/callout"
        ) {
            contentType(ContentType.Application.Json)
            setBody(
                """
            {
                "crewMembers": "Paramedic A, Paramedic B",
                "incidentDateTime": "2026-11-20T10:30:00",
                "incidentLocation": "",
                "actionsTaken": "Initial assessment and treatment provided",
                "outcome": "Patient stabilised and transported to hospital",
                "durationMinutes": 45
            }
            """.trimIndent()
            )
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `test ambulance smartphone receives dispatch information`() = testApplication {
        configureTestDatabase()
        application { rootModule() }

        // Create an emergency
        val emergencyResponse = client.post("/emergencies") {
            contentType(ContentType.Application.Json)
            setBody(
                """
            {
                "patientNhsNumber": "NHS001",
                "incidentAddress": "15 Example Street, Edinburgh",
                "description": "Urgent medical assistance required"
            }
            """.trimIndent()
            )
        }

        assertEquals(HttpStatusCode.Created, emergencyResponse.status)

        val emergency = Json.decodeFromString<Emergency>(
            emergencyResponse.bodyAsText()
        )

        // Create an HTTP client connected to the in-memory test server
        val smartphoneHttpClient = createClient {
            install(ContentNegotiation) {
                json()
            }
        }

        val smartphoneClient = AmbulanceSmartphoneClient(
            baseUrl = "",
            client = smartphoneHttpClient
        )

        try {
            val dispatch = smartphoneClient.receiveDispatch(emergency.emergencyId)

            assertEquals(emergency.emergencyId, dispatch.emergency.emergencyId)
            assertEquals("NHS001", dispatch.patient.nhsNumber)
            assertEquals("Alex Morgan", dispatch.patient.name)
            assertEquals("Asthma", dispatch.patient.medicalCondition)
        } finally {
            smartphoneClient.close()
        }
    }

    @Test
    fun `test ambulance smartphone submits and retrieves callout record`() = testApplication {
        configureTestDatabase()
        application { rootModule() }

        // Create an emergency
        val emergencyResponse = client.post("/emergencies") {
            contentType(ContentType.Application.Json)
            setBody(
                """
            {
                "patientNhsNumber": "NHS001",
                "incidentAddress": "15 Example Street, Edinburgh",
                "description": "Urgent medical assistance required"
            }
            """.trimIndent()
            )
        }

        assertEquals(HttpStatusCode.Created, emergencyResponse.status)

        val emergency = Json.decodeFromString<Emergency>(
            emergencyResponse.bodyAsText()
        )

        val hospitalId = emergency.allocatedHospitalId!!
        val emergencyId = emergency.emergencyId

        // Complete the emergency lifecycle
        for (status in listOf("EN_ROUTE", "ARRIVED", "COMPLETED")) {
            val response = client.put(
                "/hospitals/$hospitalId/emergencies/$emergencyId/status"
            ) {
                contentType(ContentType.Application.Json)
                setBody("""{"status":"$status"}""")
            }

            assertEquals(HttpStatusCode.OK, response.status)
        }

        // Create the smartphone HTTP client
        val smartphoneHttpClient = createClient {
            install(ContentNegotiation) {
                json()
            }
        }

        val smartphoneClient = AmbulanceSmartphoneClient(
            baseUrl = "",
            client = smartphoneHttpClient
        )

        try {
            // Submit the call-out record using the smartphone client
            val request = CalloutRecordRequest(
                crewMembers = "Paramedic A, Paramedic B",
                incidentDateTime = "2026-11-20T10:30:00",
                incidentLocation = "15 Example Street, Edinburgh",
                actionsTaken = "Initial assessment and treatment provided",
                outcome = "Patient stabilised and transported to hospital",
                durationMinutes = 45
            )

            val submittedRecord = smartphoneClient.submitCallout(
                hospitalId,
                emergencyId,
                request
            )

            assertEquals(emergencyId, submittedRecord.emergencyId)
            assertEquals("Paramedic A, Paramedic B", submittedRecord.crewMembers)

            // Retrieve the saved record using the smartphone client
            val retrievedRecord = smartphoneClient.retrieveCallout(
                hospitalId,
                emergencyId
            )

            assertEquals(submittedRecord.calloutId, retrievedRecord.calloutId)
            assertEquals(emergencyId, retrievedRecord.emergencyId)
            assertEquals("Initial assessment and treatment provided", retrievedRecord.actionsTaken)
            assertEquals(45, retrievedRecord.durationMinutes)

        } finally {
            smartphoneClient.close()
        }
    }



}
