
package uk.ac.napier.kwikmedical.repository

import uk.ac.napier.kwikmedical.database.DatabaseFactory
import uk.ac.napier.kwikmedical.Patient

class PatientRepository {

    fun findAll(): List<Patient> {
        val patients = mutableListOf<Patient>()

        DatabaseFactory.getConnection().use { connection ->

            connection.createStatement().use { statement ->

                statement.executeQuery(
                    """
                    SELECT
                        nhs_number,
                        name,
                        address,
                        medical_condition
                    FROM patients
                    ORDER BY nhs_number
                    """.trimIndent()
                ).use { result ->

                    while (result.next()) {
                        patients.add(
                            Patient(
                                nhsNumber = result.getString("nhs_number"),
                                name = result.getString("name"),
                                address = result.getString("address"),
                                medicalCondition = result.getString("medical_condition")
                            )
                        )
                    }
                }
            }
        }

        return patients
    }

    fun findByNhsNumber(nhsNumber: String): Patient? {

        if (nhsNumber.isBlank()) {
            return null
        }

        DatabaseFactory.getConnection().use { connection ->

            connection.prepareStatement(
                """
                SELECT
                    nhs_number,
                    name,
                    address,
                    medical_condition
                FROM patients
                WHERE nhs_number = ?
                """.trimIndent()
            ).use { statement ->

                statement.setString(1, nhsNumber.trim())

                statement.executeQuery().use { result ->

                    return if (result.next()) {
                        Patient(
                            nhsNumber = result.getString("nhs_number"),
                            name = result.getString("name"),
                            address = result.getString("address"),
                            medicalCondition = result.getString("medical_condition")
                        )
                    } else {
                        null
                    }
                }
            }
        }
    }

    fun updateMedicalCondition(
        nhsNumber: String,
        medicalCondition: String
    ): Patient? {

        val cleanNhsNumber = nhsNumber.trim()
        val cleanMedicalCondition = medicalCondition.trim()

        if (
            cleanNhsNumber.isBlank() ||
            cleanMedicalCondition.isBlank()
        ) {
            return null
        }

        DatabaseFactory.getConnection().use { connection ->

            connection.prepareStatement(
                """
                UPDATE patients
                SET medical_condition = ?
                WHERE nhs_number = ?
                """.trimIndent()
            ).use { statement ->

                statement.setString(1, cleanMedicalCondition)
                statement.setString(2, cleanNhsNumber)

                val updatedRows = statement.executeUpdate()

                if (updatedRows != 1) {
                    return null
                }
            }
        }

        return findByNhsNumber(cleanNhsNumber)
    }
}

