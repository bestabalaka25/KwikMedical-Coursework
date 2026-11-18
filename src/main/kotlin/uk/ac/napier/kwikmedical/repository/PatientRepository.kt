
package uk.ac.napier.kwikmedical.repository

import uk.ac.napier.kwikmedical.database.DatabaseFactory
import uk.ac.napier.kwikmedical.Patient

class PatientRepository {

    fun findAll(): List<Patient> {
        val patients = mutableListOf<Patient>()

        DatabaseFactory.getConnection().use { connection ->
            connection.createStatement().use { statement ->
                statement.executeQuery(
                    "SELECT * FROM patients ORDER BY nhs_number"
                ).use { result ->
                    while (result.next()) {
                        patients.add(
                            Patient(
                                nhsNumber = result.getString("nhs_number"),
                                name = result.getString("name"),
                                address = result.getString("address"),
                                medicalCondition =
                                result.getString("medical_condition")
                            )
                        )
                    }
                }
            }
        }

        return patients
    }

    fun findByNhsNumber(nhsNumber: String): Patient? {
        DatabaseFactory.getConnection().use { connection ->
            connection.prepareStatement(
                "SELECT * FROM patients WHERE nhs_number = ?"
            ).use { statement ->
                statement.setString(1, nhsNumber)

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
}
