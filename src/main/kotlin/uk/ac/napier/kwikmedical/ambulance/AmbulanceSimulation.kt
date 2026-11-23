package uk.ac.napier.kwikmedical.ambulance

suspend fun main(args: Array<String>) {

    val emergencyId = args.firstOrNull()
        ?: error("Please provide an emergency ID.")

    val smartphone = AmbulanceSmartphoneClient()

    try {
        println("======================================")
        println("      KWIKMEDICAL AMBULANCE APP")
        println("======================================")

        println("\nConnecting to KwikMedical server...")

        val dispatch = smartphone.receiveDispatch(emergencyId)

        println("\nDISPATCH RECEIVED")
        println("--------------------------------------")

        println("Emergency ID: ${dispatch.emergency.emergencyId}")
        println("Status: ${dispatch.emergency.status}")
        println("Hospital: ${dispatch.emergency.allocatedHospitalId}")
        println("Ambulance: ${dispatch.emergency.allocatedAmbulanceId}")

        println("\nPATIENT INFORMATION")
        println("--------------------------------------")

        println("Name: ${dispatch.patient.name}")
        println("NHS Number: ${dispatch.patient.nhsNumber}")
        println("Medical Condition: ${dispatch.patient.medicalCondition}")
        println("Patient Address: ${dispatch.patient.address}")

        println("\nINCIDENT INFORMATION")
        println("--------------------------------------")

        println("Location: ${dispatch.emergency.incidentAddress}")
        println("Description: ${dispatch.emergency.description}")

        println("\nRetrieving ambulance callout record...")

        val hospitalId = dispatch.emergency.allocatedHospitalId
            ?: error("No hospital has been allocated to this emergency.")

        val callout = smartphone.retrieveCallout(
            hospitalId,
            emergencyId
        )

        println("\nAMBULANCE CALLOUT RECORD")
        println("--------------------------------------")

        println("Crew Members: ${callout.crewMembers}")
        println("Incident Date and Time: ${callout.incidentDateTime}")
        println("Incident Location: ${callout.incidentLocation}")
        println("Actions Taken: ${callout.actionsTaken}")
        println("Outcome: ${callout.outcome}")
        println("Duration: ${callout.durationMinutes} minutes")

        println("\nDispatch and callout retrieval successful.")

    } finally {
        smartphone.close()
    }
}
