# &#x20;KwikMedical

# 

# &#x20;About the Project

# 

# KwikMedical is an emergency ambulance coordination system developed for the Software Architecture coursework.

# 

# The aim of the project is to help manage emergency requests, access patient medical information and allocate available ambulances through hospitals.

# 

# The system is built using Kotlin and Ktor, with an H2 database to store information. It also includes a simulated ambulance smartphone client to demonstrate how ambulance staff could receive emergency details and submit call-out records.

# 

# &#x20;Main Features

# 

# The system includes the following features:

# 

# \- \*\*Patient information:\*\* View patient records using their NHS number.

# \- \*\*Emergency requests:\*\* Create and view emergency requests.

# \- \*\*Ambulance allocation:\*\* Find and reserve an available ambulance through a hospital.

# \- \*\*Hospital management:\*\* View emergencies assigned to individual hospitals.

# \- \*\*Ambulance dispatch:\*\* Send emergency and patient information to the simulated ambulance client.

# \- \*\*Emergency status updates:\*\* Update the progress of an emergency as it moves through different stages.

# \- \*\*Call-out records:\*\* Record details such as crew members, incident location, actions taken and outcome.

# \- \*\*Patient record updates:\*\* Allow hospital workflows to update a patient's medical condition.

# \- \*\*Database storage:\*\* Store patient, emergency, ambulance and call-out information.

# \- \*\*Concurrent allocation:\*\* Use database transactions and row locking to help prevent the same ambulance from being allocated to multiple emergencies at the same time.

# 

# &#x20;Project Structure

# 

# The project is organised into separate packages to keep the code easier to understand and maintain.

# 

# | Package | Purpose |

# |---|---|

# | `ambulance` | Ambulance information and the simulated smartphone client |

# | `database` | Database setup and connections |

# | `emergency` | Emergency and call-out data models |

# | `hospital` | Hospital-related information and request models |

# | `repository` | Handles communication with the database |

# | `service` | Contains the main application logic |

# 

# The application uses a Ktor REST API to handle requests and communicate with the different parts of the system.

# 

# &#x20;Technologies Used

# 

# \- Kotlin

# \- Ktor 3.6.0

# \- Gradle

# \- H2 Database

# \- kotlinx.serialization

# \- Java 17

# \- Ktor HTTP Client

# 

# &#x20;Requirements

# 

# To run the project, you will need:

# 

# \- Java Development Kit (JDK) 17

# \- Windows PowerShell or another terminal

# \- Git (if cloning the repository)

# 

# &#x20;How to Run the Project

# 

# First, clone the repository:

# 

# ```bash id="r2t4j6"

# git clone https://github.com/bestabalaka25/KwikMedical-Coursework.git

# cd KwikMedical-Coursework

# ```

# 

# &#x20;Build the project

# 

# On Windows, run:

# 

# ```powershell id="t3y6u8"

# .\\gradlew.bat build

# ```

# 

# &#x20;Run the tests

# 

# To run the automated tests:

# 

# ```powershell id="v4z7a9"

# .\\gradlew.bat clean test

# ```

# 

# &#x20;Start the application

# 

# To start the Ktor server:

# 

# ```powershell id="w5x8b1"

# .\\gradlew.bat run

# ```

# 

# The server runs on port `8080`.

# 

# &#x20;API Endpoints

# 

# The following table shows some of the main endpoints available in the application.

# 

# | Method | Endpoint | Purpose |

# |---|---|---|

# | GET | `/` | Check that the server is running |

# | GET | `/patients` | View all patients |

# | GET | `/patients/{nhsNumber}` | Find a patient using their NHS number |

# | POST | `/emergencies` | Create a new emergency request |

# | GET | `/emergencies` | View all emergencies |

# | GET | `/emergencies/{emergencyId}` | View a specific emergency |

# | GET | `/dispatch/{emergencyId}` | Retrieve dispatch information |

# | GET | `/hospitals/{hospitalId}/emergencies` | View emergencies assigned to a hospital |

# | PUT | `/hospitals/{hospitalId}/emergencies/{emergencyId}/status` | Update an emergency's status |

# | POST | `/hospitals/{hospitalId}/emergencies/{emergencyId}/callout` | Submit a call-out record |

# | GET | `/hospitals/{hospitalId}/emergencies/{emergencyId}/callout` | Retrieve a call-out record |

# | PUT | `/hospitals/{hospitalId}/patients/{nhsNumber}` | Update patient medical information |

# 

# &#x20;Database Information

# 

# The project uses an H2 database to store its information.

# 

# The database is created automatically when the application starts. By default, the database files are stored in:

# 

# `data/kwikmedical`

# 

# The project also creates sample patient and ambulance records for testing. These records are fictional and are not real patient information.

# 

# &#x20;Current Limitations and Future Improvements

# 

# This project is a working academic prototype, so some features are simulated rather than connected to real services.

# 

# Possible improvements in the future include:

# 

# \- Connecting hospitals as separate services to demonstrate a fully distributed system.

# \- Adding GPS functionality to help identify nearby hospitals and ambulances.

# \- Adding user authentication and stronger security.

# \- Using a production-ready central database.

# \- Integrating with real hospital and emergency service systems.

# \- Developing a dedicated mobile application for ambulance staff.

# 

# &#x20;Coursework Information

# 

# \*\*Module:\*\* Software Architecture  

# \*\*University:\*\* Edinburgh Napier University  

# \*\*Project:\*\* KwikMedical

