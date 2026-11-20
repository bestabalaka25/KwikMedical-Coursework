package uk.ac.napier.kwikmedical.repository

import uk.ac.napier.kwikmedical.database.DatabaseFactory
import uk.ac.napier.kwikmedical.emergency.CalloutRecord

class CalloutRepository {

    fun save(record: CalloutRecord): CalloutRecord {
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

                statement.executeUpdate()
            }
        }

        return record
    }

    fun findByEmergencyId(emergencyId: String): CalloutRecord? {
        DatabaseFactory.getConnection().use { connection ->
            connection.prepareStatement(
                """
                SELECT * FROM callout_records
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