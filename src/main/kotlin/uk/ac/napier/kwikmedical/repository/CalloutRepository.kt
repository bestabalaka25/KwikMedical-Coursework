
package uk.ac.napier.kwikmedical.repository

import uk.ac.napier.kwikmedical.database.DatabaseFactory
import uk.ac.napier.kwikmedical.emergency.CalloutRecord
import java.sql.SQLException

class DuplicateCalloutException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)

class CalloutRepository {

    fun save(record: CalloutRecord): CalloutRecord {

        try {
            DatabaseFactory.getConnection().use { connection ->

                connection.prepareStatement(
                    """
                    INSERT INTO callout_records (
                        callout_id,
                        emergency_id,
                        crew_members,
                        incident_date_time,
                        incident_location,
                        actions_taken,
                        outcome,
                        duration_minutes
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                    """.trimIndent()
                ).use { statement ->

                    statement.setString(1, record.calloutId)
                    statement.setString(2, record.emergencyId)
                    statement.setString(3, record.crewMembers)
                    statement.setString(4, record.incidentDateTime)
                    statement.setString(5, record.incidentLocation)
                    statement.setString(6, record.actionsTaken)
                    statement.setString(7, record.outcome)
                    statement.setInt(8, record.durationMinutes)

                    val rowsInserted = statement.executeUpdate()

                    check(rowsInserted == 1) {
                        "Call-out record could not be saved."
                    }
                }
            }
        } catch (exception: SQLException) {

            if (exception.sqlState == "23505") {
                throw DuplicateCalloutException(
                    "A call-out record already exists for this emergency.",
                    exception
                )
            }

            throw exception
        }

        return record
    }

    fun findByEmergencyId(emergencyId: String): CalloutRecord? {

        if (emergencyId.isBlank()) {
            return null
        }

        DatabaseFactory.getConnection().use { connection ->

            connection.prepareStatement(
                """
                SELECT
                    callout_id,
                    emergency_id,
                    crew_members,
                    incident_date_time,
                    incident_location,
                    actions_taken,
                    outcome,
                    duration_minutes
                FROM callout_records
                WHERE emergency_id = ?
                """.trimIndent()
            ).use { statement ->

                statement.setString(1, emergencyId)

                statement.executeQuery().use { results ->

                    if (results.next()) {
                        return CalloutRecord(
                            calloutId = results.getString("callout_id"),
                            emergencyId = results.getString("emergency_id"),
                            crewMembers = results.getString("crew_members"),
                            incidentDateTime = results.getString("incident_date_time"),
                            incidentLocation = results.getString("incident_location"),
                            actionsTaken = results.getString("actions_taken"),
                            outcome = results.getString("outcome"),
                            durationMinutes = results.getInt("duration_minutes")
                        )
                    }
                }
            }
        }

        return null
    }
}
