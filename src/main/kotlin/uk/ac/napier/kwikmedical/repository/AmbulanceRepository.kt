
package uk.ac.napier.kwikmedical.repository

import uk.ac.napier.kwikmedical.ambulance.Ambulance
import uk.ac.napier.kwikmedical.database.DatabaseFactory

class AmbulanceRepository {

    fun reserveAvailableForHospital(hospitalId: String): Ambulance? {
        DatabaseFactory.getConnection().use { connection ->

            connection.autoCommit = false

            try {
                val ambulance = connection.prepareStatement(
                    """
                    SELECT ambulance_id, hospital_id, vehicle_call_sign
                    FROM ambulances
                    WHERE hospital_id = ?
                    AND available = TRUE
                    ORDER BY ambulance_id
                    LIMIT 1
                    FOR UPDATE
                    """.trimIndent()
                ).use { statement ->
                    statement.setString(1, hospitalId)

                    statement.executeQuery().use { results ->
                        if (results.next()) {
                            Ambulance(
                                ambulanceId = results.getString("ambulance_id"),
                                hospitalId = results.getString("hospital_id"),
                                vehicleCallSign = results.getString("vehicle_call_sign"),
                                available = false
                            )
                        } else {
                            null
                        }
                    }
                }

                if (ambulance == null) {
                    connection.rollback()
                    return null
                }

                val updatedRows = connection.prepareStatement(
                    """
                    UPDATE ambulances
                    SET available = FALSE
                    WHERE ambulance_id = ?
                    AND available = TRUE
                    """.trimIndent()
                ).use { statement ->
                    statement.setString(1, ambulance.ambulanceId)
                    statement.executeUpdate()
                }

                if (updatedRows != 1) {
                    connection.rollback()
                    return null
                }

                connection.commit()
                return ambulance

            } catch (exception: Exception) {
                connection.rollback()
                throw exception
            }
        }
    }

    fun hasAvailableForHospital(hospitalId: String): Boolean {
        DatabaseFactory.getConnection().use { connection ->
            connection.prepareStatement(
                """
            SELECT COUNT(*) FROM ambulances
            WHERE hospital_id = ? AND available = TRUE
            """.trimIndent()
            ).use { statement ->
                statement.setString(1, hospitalId)

                statement.executeQuery().use { results ->
                    results.next()
                    return results.getInt(1) > 0
                }
            }
        }
    }

    fun release(ambulanceId: String) {
        DatabaseFactory.getConnection().use { connection ->
            connection.prepareStatement(
                """
                UPDATE ambulances
                SET available = TRUE
                WHERE ambulance_id = ?
                """.trimIndent()
            ).use { statement ->
                statement.setString(1, ambulanceId)
                statement.executeUpdate()
            }
        }
    }
}
