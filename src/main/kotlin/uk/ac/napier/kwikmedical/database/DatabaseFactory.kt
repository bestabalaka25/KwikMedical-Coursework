
package uk.ac.napier.kwikmedical.database

import java.nio.file.Files
import java.nio.file.Path
import java.sql.Connection
import java.sql.DriverManager

object DatabaseFactory {

    private fun databaseUrl() =
        System.getProperty("kwikmedical.database.url")
            ?: "jdbc:h2:file:./data/kwikmedical"

    fun getConnection() =
        DriverManager.getConnection(databaseUrl(), "sa", "")

    fun initialize() {
        Files.createDirectories(Path.of("data"))

        getConnection().use { connection ->

            // Create patients table
            connection.createStatement().use { statement ->
                statement.execute(
                    """
                    CREATE TABLE IF NOT EXISTS patients (
                        nhs_number VARCHAR(20) PRIMARY KEY,
                        name VARCHAR(100) NOT NULL,
                        address VARCHAR(255) NOT NULL,
                        medical_condition VARCHAR(255) NOT NULL
                    )
                    """.trimIndent()
                )
            }

            // Create emergencies table
            connection.createStatement().use { statement ->
                statement.execute(
                    """
                    CREATE TABLE IF NOT EXISTS emergencies (
                        emergency_id VARCHAR(36) PRIMARY KEY,
                        patient_nhs_number VARCHAR(20) NOT NULL,
                        incident_address VARCHAR(255) NOT NULL,
                        description VARCHAR(1000) NOT NULL,
                        status VARCHAR(30) NOT NULL,
                        allocated_hospital_id VARCHAR(20),
                        allocated_ambulance_id VARCHAR(20),
                        FOREIGN KEY (patient_nhs_number)
                            REFERENCES patients(nhs_number)
                    )
                    """.trimIndent()
                )
            }

            // Update existing emergencies table if necessary
            connection.createStatement().use { statement ->
                statement.execute(
                    """
                    ALTER TABLE emergencies
                    ADD COLUMN IF NOT EXISTS allocated_ambulance_id VARCHAR(20)
                    """.trimIndent()
                )
            }

            // Create ambulances table
            connection.createStatement().use { statement ->
                statement.execute(
                    """
                    CREATE TABLE IF NOT EXISTS ambulances (
                        ambulance_id VARCHAR(20) PRIMARY KEY,
                        hospital_id VARCHAR(20) NOT NULL,
                        vehicle_call_sign VARCHAR(20) NOT NULL,
                        available BOOLEAN NOT NULL DEFAULT TRUE
                    )
                    """.trimIndent()
                )
            }

            // Create call-out records table
            connection.createStatement().use { statement ->
                statement.execute(
                    """
                    CREATE TABLE IF NOT EXISTS callout_records (
                        callout_id VARCHAR(36) PRIMARY KEY,
                        emergency_id VARCHAR(36) NOT NULL UNIQUE,
                        crew_members VARCHAR(500) NOT NULL,
                        incident_date_time VARCHAR(50) NOT NULL,
                        incident_location VARCHAR(255) NOT NULL,
                        actions_taken VARCHAR(2000) NOT NULL,
                        outcome VARCHAR(1000) NOT NULL,
                        duration_minutes INT NOT NULL CHECK (duration_minutes >= 0),
                        FOREIGN KEY (emergency_id)
                            REFERENCES emergencies(emergency_id)
                    )
                    """.trimIndent()
                )
            }

            // Seed sample data
            seedPatients(connection)
            seedAmbulances(connection)

            // Add ambulance foreign key
            connection.createStatement().use { statement ->
                statement.execute(
                    """
                    ALTER TABLE emergencies
                    ADD CONSTRAINT IF NOT EXISTS fk_emergencies_ambulance
                    FOREIGN KEY (allocated_ambulance_id)
                    REFERENCES ambulances(ambulance_id)
                    """.trimIndent()
                )
            }
        }
    }

    private fun seedPatients(connection: Connection) {
        val samplePatients = listOf(
            arrayOf(
                "NHS001",
                "Alex Morgan",
                "10 Example Street, Edinburgh",
                "Asthma"
            ),
            arrayOf(
                "NHS002",
                "Jamie Taylor",
                "20 Sample Road, Edinburgh",
                "Diabetes"
            )
        )

        connection.prepareStatement(
            """
            INSERT INTO patients
                (nhs_number, name, address, medical_condition)
            SELECT ?, ?, ?, ?
            WHERE NOT EXISTS (
                SELECT 1 FROM patients WHERE nhs_number = ?
            )
            """.trimIndent()
        ).use { statement ->

            for (patient in samplePatients) {
                statement.setString(1, patient[0])
                statement.setString(2, patient[1])
                statement.setString(3, patient[2])
                statement.setString(4, patient[3])
                statement.setString(5, patient[0])

                statement.executeUpdate()
            }
        }
    }

    private fun seedAmbulances(connection: Connection) {
        for (i in 1..20) {
            val id = i.toString().padStart(3, '0')

            connection.prepareStatement(
                """
                INSERT INTO ambulances (
                    ambulance_id,
                    hospital_id,
                    vehicle_call_sign,
                    available
                )
                SELECT ?, ?, ?, TRUE
                WHERE NOT EXISTS (
                    SELECT 1 FROM ambulances WHERE ambulance_id = ?
                )
                """.trimIndent()
            ).use { statement ->

                statement.setString(1, "A$id")
                statement.setString(2, "H$id")
                statement.setString(3, "AMB-$id")
                statement.setString(4, "A$id")

                statement.executeUpdate()
            }
        }
    }
}
