KWIKMEDICAL
Overview
KwikMedical is an application which has been produced for the Software Architecture module at Edinburgh Napier University. It enables users to both manage emergency requests and patient data and to assign ambulances to hospitals.

It has been developed using the Kotlin and Ktor technologies. A database has been used to store data. Furthermore, it also includes a simulated ambulance smart phone application which can be used to show how ambulance personnel can both receive emergency data and also check call out data.

KEY FEATURES
1. Patient data can be viewed by searching using a patient’s NHS number.
2. Emergency requests can be produced and viewed.
3. Hospitals can search for and assign an ambulance.
4. Users can view which emergencies have been allocated to particular hospitals.
5. Emergency and patient data can be sent to the simulated ambulance smart phone application.
6. Emergency progress can be updated.
7. Details, such as ambulance personnel, location and response to an emergency can be documented.
8. Hospital processes can edit a patient’s medical data.
9. Patient, emergency, ambulance and call out data can be stored in a database.
10. Database transactions and row locking can be used to ensure that an ambulance is not assigned to more than one emergency at a time.

APPLICATION DESIGN
Packages have been formed to assist with the comprehension and ongoing maintenance of the application code.
Ambulance – ambulance data and the simulated smart phone application.
Database – database configuration and connection.
Emergency – emergency and call out data models.
Hospital – hospital data and request data models.
Repository – database interaction.
Service – core application logic.
Requests are managed via a Ktor REST API and this is also used to interact with various sections of the application.

TOOLS AND TECHNOLOGIES
Kotlin
Ktor 3.6.0

Gradle
H2 Database
kotlinx.serialization
Java 17
Ktor HTTP Client

SYSTEM REQUIREMENTS
You will require:
A Java Development Kit (JDK) version 17
Access to Windows PowerShell or another terminal
Git (optional – for use when cloning a version control repository)

RUNNING THE PROJECT
Clone the project:
git clone https://github.com/bestabalaka25/KwikMedical-Coursework.git
Change to the project folder:
cd KwikMedical-Coursework

Build the project:
.\gradlew.bat build

Execute automated tests:
.\gradlew.bat clean test

Launch the Ktor server:
.\gradlew.bat run
The server is hosted on port 8080.

APIs
GET /Verifies that the server is operational.
GET /patients
Displays all patient data.
GET /patients/{nhsNumber}
Displays patient data relating to a particular NHS number.
POST /emergencies
Adds a new emergency request.
GET /emergencies
Displays emergency data.
GET /emergencies/{emergencyId}
Displays data for an individual emergency.
GET /dispatch/{emergencyId}
Displays data relating to the dispatch of an ambulance.
GET /hospitals/{hospitalId}/emergencies
Displays the emergencies which have been allocated to a given hospital.
PUT /hospitals/{hospitalId}/emergencies/{emergencyId}/status
Alters an emergency’s status.
POST /hospitals/{hospitalId}/emergencies/{emergencyId}/callout
Adds ambulance call out data.
GET /hospitals/{hospitalId}/emergencies/{emergencyId}/callout
Displays ambulance call out data.
PUT /hospitals/{hospitalId}/patients/{nhsNumber}
Edits patient medical data.

DATABASE DETAILS
An H2 database is used in this project to store data.
On initiation of the application, the database will be automatically generated. By default, database files are saved in the following location:
data/kwikmedical
Dummy patient and ambulance data has also been added to the database to facilitate testing. However, this is not based on any real patient data.

EXISTING CONSTRAINTS AND POTENTIAL DEVELOPMENT
As this is an academic prototype, not all elements are linked to live services and so some aspects are simulated.

Potential areas for further development are:
1. Enable hospitals to act as independent services thereby providing a true example of a distributed system.
2. Include GPS so that hospitals and ambulances can be located.
3. Implement user authentication and enhanced security.
4. Employ a suitable production quality database.
5. Link with live hospital and emergency service systems.
6. Construct a specialised smart phone application for ambulance personnel.

COURSEWORK DETAILS
Module: Software Architecture
University: Edinburgh Napier University
Project: KwikMedical
