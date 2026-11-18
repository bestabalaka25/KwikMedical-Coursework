
package uk.ac.napier.kwikmedical.repository

import uk.ac.napier.kwikmedical.database.DatabaseFactory
import uk.ac.napier.kwikmedical.emergency.Emergency

class EmergencyRepository {

    fun save(emergency: Emergency): Emergency {
        DatabaseFactory.getConnection().use { connection ->
            connection.prepareStatement(
                """
                INSERT INTO emergencies (
                    emergency_id,
                    patient_nhs_number,
                    incident_address,
                    description,
                    status,
                    allocated_hospital_id,
                    allocated_ambulance_id
                ) VALUES (?, ?, ?, ?, ?, ?, ?)
                """.trimIndent()
            ).use { statement ->

                statement.setString(1, emergency.emergencyId)
                statement.setString(2, emergency.patientNhsNumber)
                statement.setString(3, emergency.incidentAddress)
                statement.setString(4, emergency.description)
                statement.setString(5, emergency.status)
                statement.setString(6, emergency.allocatedHospitalId)
                statement.setString(7, emergency.allocatedAmbulanceId)

                statement.executeUpdate()
            }
        }

        return emergency
    }

    fun findAll(): List<Emergency> {
        val emergencies = mutableListOf<Emergency>()

        DatabaseFactory.getConnection().use { connection ->
            connection.prepareStatement(
                "SELECT * FROM emergencies"
            ).use { statement ->

                val results = statement.executeQuery()

                while (results.next()) {
                    emergencies.add(mapEmergency(results))
                }
            }
        }

        return emergencies
    }

    fun findById(emergencyId: String): Emergency? {
        DatabaseFactory.getConnection().use { connection ->
            connection.prepareStatement(
                "SELECT * FROM emergencies WHERE emergency_id = ?"
            ).use { statement ->

                statement.setString(1, emergencyId)

                val results = statement.executeQuery()

                if (results.next()) {
                    return mapEmergency(results)
                }
            }
        }

        return null
    }

    fun findByHospitalId(hospitalId: String): List<Emergency> {
        val emergencies = mutableListOf<Emergency>()

        DatabaseFactory.getConnection().use { connection ->
            connection.prepareStatement(
                """
            SELECT * FROM emergencies
            WHERE allocated_hospital_id = ?
            """.trimIndent()
            ).use { statement ->
                statement.setString(1, hospitalId)

                val results = statement.executeQuery()

                while (results.next()) {
                    emergencies.add(mapEmergency(results))
                }
            }
        }

        return emergencies
    }

    fun completeAndReleaseAmbulance(emergencyId: String): Boolean {
        DatabaseFactory.getConnection().use { connection ->
            connection.autoCommit = false

            try {
                val ambulanceId = connection.prepareStatement(
                    """
                SELECT allocated_ambulance_id
                FROM emergencies
                WHERE emergency_id = ?
                AND status = 'ARRIVED'
                FOR UPDATE
                """.trimIndent()
                ).use { statement ->
                    statement.setString(1, emergencyId)

                    statement.executeQuery().use { results ->
                        if (results.next()) {
                            results.getString("allocated_ambulance_id")
                        } else {
                            null
                        }
                    }
                }

                if (ambulanceId == null) {
                    connection.rollback()
                    return false
                }

                val releasedRows = connection.prepareStatement(
                    """
                UPDATE ambulances
                SET available = TRUE
                WHERE ambulance_id = ?
                AND available = FALSE
                """.trimIndent()
                ).use { statement ->
                    statement.setString(1, ambulanceId)
                    statement.executeUpdate()
                }

                if (releasedRows != 1) {
                    connection.rollback()
                    return false
                }

                val updatedRows = connection.prepareStatement(
                    """
                UPDATE emergencies
                SET status = 'COMPLETED'
                WHERE emergency_id = ?
                AND status = 'ARRIVED'
                """.trimIndent()
                ).use { statement ->
                    statement.setString(1, emergencyId)
                    statement.executeUpdate()
                }

                if (updatedRows != 1) {
                    connection.rollback()
                    return false
                }

                connection.commit()
                return true

            } catch (exception: Exception) {
                connection.rollback()
                throw exception
            }
        }
    }



    fun updateStatus(emergencyId: String, expectedStatus: String, newStatus: String): Boolean {
        DatabaseFactory.getConnection().use { connection ->
            connection.prepareStatement(
                """
            UPDATE emergencies
            SET status = ?
            WHERE emergency_id = ?
            AND status = ?
            """.trimIndent()
            ).use { statement ->
                statement.setString(1, newStatus)
                statement.setString(2, emergencyId)
                statement.setString(3, expectedStatus)

                return statement.executeUpdate() == 1
            }
        }
    }



    private fun mapEmergency(
        results: java.sql.ResultSet
    ): Emergency {
        return Emergency(
            emergencyId = results.getString("emergency_id"),
            patientNhsNumber = results.getString("patient_nhs_number"),
            incidentAddress = results.getString("incident_address"),
            description = results.getString("description"),
            status = results.getString("status"),
            allocatedHospitalId = results.getString("allocated_hospital_id"),
            allocatedAmbulanceId = results.getString("allocated_ambulance_id")
        )
    }
}
