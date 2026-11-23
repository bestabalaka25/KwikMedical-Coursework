
const emergencyInput = document.getElementById("emergencyId");
const loadButton = document.getElementById("loadDispatch");
const message = document.getElementById("message");
const dispatchDetails = document.getElementById("dispatchDetails");

const calloutSection = document.getElementById("calloutSection");
const calloutForm = document.getElementById("calloutForm");
const calloutMessage = document.getElementById("calloutMessage");

const patientUpdateSection = document.getElementById("patientUpdateSection");
const patientUpdateForm = document.getElementById("patientUpdateForm");
const patientUpdateMessage = document.getElementById("patientUpdateMessage");

const nextStatuses = {
    ALLOCATED: "EN_ROUTE",
    EN_ROUTE: "ARRIVED",
    ARRIVED: "COMPLETED"
};

// Event listeners
loadButton.addEventListener("click", loadDispatch);
calloutForm.addEventListener("submit", submitCallout);
patientUpdateForm.addEventListener("submit", updatePatientRecord);


// Load emergency dispatch information
async function loadDispatch() {
    const emergencyId = emergencyInput.value.trim();

    dispatchDetails.hidden = true;
    dispatchDetails.innerHTML = "";

    calloutSection.hidden = true;
    patientUpdateSection.hidden = true;

    if (!emergencyId) {
        message.textContent = "Please enter an emergency ID.";
        message.style.color = "#c0392b";
        return;
    }

    message.textContent = "Retrieving dispatch information...";
    message.style.color = "#737373";

    loadButton.disabled = true;

    try {
        const response = await fetch(
            `/dispatch/${encodeURIComponent(emergencyId)}`
        );

        if (!response.ok) {
            throw new Error(
                response.status === 404
                    ? "Emergency or patient record not found."
                    : `Server error: ${response.status}`
            );
        }

        const data = await response.json();

        displayDispatch(data);

        message.textContent = "Dispatch information loaded successfully.";
        message.style.color = "#16845b";

    } catch (error) {
        message.textContent = error.message;
        message.style.color = "#c0392b";

    } finally {
        loadButton.disabled = false;
    }
}


// Display emergency and patient information
function displayDispatch(data) {
    const emergency = data.emergency;
    const patient = data.patient;

    const nextStatus = nextStatuses[emergency.status];

    dispatchDetails.innerHTML = `
        <div class="dispatch-card">
            <h3>Emergency Information</h3>

            <div class="detail-item">
                <span>Emergency ID</span>
                <strong>${escapeHTML(emergency.emergencyId)}</strong>
            </div>

            <div class="detail-item">
                <span>Status</span>
                <strong>${escapeHTML(emergency.status)}</strong>
            </div>

            <div class="detail-item">
                <span>Incident Address</span>
                <strong>${escapeHTML(emergency.incidentAddress)}</strong>
            </div>

            <div class="detail-item">
                <span>Description</span>
                <strong>${escapeHTML(emergency.description)}</strong>
            </div>
        </div>

        <div class="dispatch-card">
            <h3>Patient Information</h3>

            <div class="detail-item">
                <span>Patient Name</span>
                <strong>${escapeHTML(patient.name)}</strong>
            </div>

            <div class="detail-item">
                <span>NHS Number</span>
                <strong>${escapeHTML(patient.nhsNumber)}</strong>
            </div>

            <div class="detail-item">
                <span>Patient Address</span>
                <strong>${escapeHTML(patient.address)}</strong>
            </div>

            <div class="detail-item">
                <span>Medical Condition</span>
                <strong>${escapeHTML(patient.medicalCondition)}</strong>
            </div>
        </div>

        <div class="dispatch-card">
            <h3>Ambulance Status Management</h3>

            <div class="detail-item">
                <span>Current Status</span>
                <strong>${escapeHTML(emergency.status)}</strong>
            </div>

            ${
        nextStatus
            ? `
                        <p>Update the emergency when the crew reaches the next stage.</p>

                        <button type="button" id="updateStatusButton">
                            Mark as ${nextStatus}
                        </button>
                    `
            : "<p>This emergency has been completed.</p>"
    }
        </div>
    `;

    dispatchDetails.hidden = false;

    // Attach status update button
    if (nextStatus) {
        const statusButton = document.getElementById("updateStatusButton");

        statusButton.addEventListener("click", () => {
            updateEmergencyStatus(
                emergency.allocatedHospitalId,
                emergency.emergencyId,
                nextStatus
            );
        });
    }

    // Show additional functionality only for completed emergencies
    calloutSection.hidden = emergency.status !== "COMPLETED";
    patientUpdateSection.hidden = emergency.status !== "COMPLETED";

    // Populate current medical condition
    document.getElementById("medicalCondition").value =
        patient.medicalCondition;

    if (emergency.status === "COMPLETED") {
        loadCalloutReport(
            emergency.allocatedHospitalId,
            emergency.emergencyId
        );
    }
}


// Update ambulance emergency status
async function updateEmergencyStatus(hospitalId, emergencyId, newStatus) {
    const statusButton = document.getElementById("updateStatusButton");

    try {
        statusButton.disabled = true;

        message.textContent = "Updating emergency status...";
        message.style.color = "#737373";

        const response = await fetch(
            `/hospitals/${encodeURIComponent(hospitalId)}/emergencies/${encodeURIComponent(emergencyId)}/status`,
            {
                method: "PUT",
                headers: {
                    "Content-Type": "application/json"
                },
                body: JSON.stringify({
                    status: newStatus
                })
            }
        );

        if (!response.ok) {
            throw new Error(
                `Status update failed. Server returned ${response.status}.`
            );
        }

        emergencyInput.value = emergencyId;

        message.textContent = "Status updated successfully. Refreshing dispatch...";
        message.style.color = "#16845b";

        await loadDispatch();

    } catch (error) {
        message.textContent = error.message;
        message.style.color = "#c0392b";

        if (statusButton) {
            statusButton.disabled = false;
        }
    }
}


// Submit a new callout report
async function submitCallout(event) {
    event.preventDefault();

    const emergencyId = emergencyInput.value.trim();

    const duration = parseInt(
        document.getElementById("durationMinutes").value,
        10
    );

    if (!Number.isInteger(duration) || duration < 1) {
        calloutMessage.textContent = "Please enter a valid duration.";
        calloutMessage.style.color = "#c0392b";
        return;
    }

    const requestBody = {
        crewMembers: document.getElementById("crewMembers").value.trim(),
        incidentDateTime: document.getElementById("incidentDateTime").value,
        incidentLocation: document.getElementById("incidentLocation").value.trim(),
        actionsTaken: document.getElementById("actionsTaken").value.trim(),
        outcome: document.getElementById("outcome").value.trim(),
        durationMinutes: duration
    };

    const submitButton = document.getElementById("submitCallout");

    submitButton.disabled = true;

    calloutMessage.textContent = "Submitting callout report...";
    calloutMessage.style.color = "#737373";

    try {
        const dispatchResponse = await fetch(
            `/dispatch/${encodeURIComponent(emergencyId)}`
        );

        if (!dispatchResponse.ok) {
            throw new Error("Could not retrieve emergency information.");
        }

        const dispatch = await dispatchResponse.json();
        const hospitalId = dispatch.emergency.allocatedHospitalId;

        const response = await fetch(
            `/hospitals/${encodeURIComponent(hospitalId)}/emergencies/${encodeURIComponent(emergencyId)}/callout`,
            {
                method: "POST",
                headers: {
                    "Content-Type": "application/json"
                },
                body: JSON.stringify(requestBody)
            }
        );

        if (!response.ok) {
            throw new Error(
                response.status === 409
                    ? "A callout report already exists for this emergency."
                    : `Callout submission failed. Server returned ${response.status}.`
            );
        }

        calloutForm.reset();

        calloutMessage.textContent = "Callout report submitted successfully.";
        calloutMessage.style.color = "#16845b";

        await loadCalloutReport(hospitalId, emergencyId);

        calloutMessage.textContent = "Callout report submitted successfully.";
        calloutMessage.style.color = "#16845b";

    } catch (error) {
        calloutMessage.textContent = error.message;
        calloutMessage.style.color = "#c0392b";

    } finally {
        submitButton.disabled = false;
    }
}


// Retrieve an existing callout report
async function loadCalloutReport(hospitalId, emergencyId) {
    let history = document.getElementById("calloutHistory");

    if (!history) {
        history = document.createElement("div");
        history.id = "calloutHistory";
        calloutSection.appendChild(history);
    }

    const form = document.getElementById("calloutForm");

    try {
        const response = await fetch(
            `/hospitals/${encodeURIComponent(hospitalId)}/emergencies/${encodeURIComponent(emergencyId)}/callout`
        );

        if (response.status === 404) {
            history.innerHTML = "";
            form.hidden = false;

            calloutMessage.textContent = "No callout report has been submitted yet.";
            calloutMessage.style.color = "#737373";
            return;
        }

        if (!response.ok) {
            throw new Error(
                `Could not retrieve callout report. Server returned ${response.status}.`
            );
        }

        const report = await response.json();

        form.hidden = true;

        displayCalloutReport(report, history);

    } catch (error) {
        form.hidden = false;

        calloutMessage.textContent = error.message;
        calloutMessage.style.color = "#c0392b";
    }
}


// Display saved callout report
function displayCalloutReport(report, history) {
    history.innerHTML = `
        <div class="dispatch-card">
            <h3>Submitted Callout Report</h3>

            <div class="detail-item">
                <span>Crew Members</span>
                <strong>${escapeHTML(report.crewMembers)}</strong>
            </div>

            <div class="detail-item">
                <span>Incident Date and Time</span>
                <strong>${escapeHTML(report.incidentDateTime)}</strong>
            </div>

            <div class="detail-item">
                <span>Incident Location</span>
                <strong>${escapeHTML(report.incidentLocation)}</strong>
            </div>

            <div class="detail-item">
                <span>Actions Taken</span>
                <strong>${escapeHTML(report.actionsTaken)}</strong>
            </div>

            <div class="detail-item">
                <span>Outcome</span>
                <strong>${escapeHTML(report.outcome)}</strong>
            </div>

            <div class="detail-item">
                <span>Duration</span>
                <strong>${escapeHTML(report.durationMinutes)} minutes</strong>
            </div>

            <p>Callout report successfully saved.</p>
        </div>
    `;
}


// Update the patient's medical record
async function updatePatientRecord(event) {
    event.preventDefault();

    const emergencyId = emergencyInput.value.trim();

    const medicalCondition = document
        .getElementById("medicalCondition")
        .value.trim();

    if (!medicalCondition) {
        patientUpdateMessage.textContent =
            "Please enter a medical condition.";

        patientUpdateMessage.style.color = "#c0392b";
        return;
    }

    const updateButton = document.getElementById("updatePatientButton");

    updateButton.disabled = true;

    patientUpdateMessage.textContent = "Updating patient record...";
    patientUpdateMessage.style.color = "#737373";

    try {
        const dispatchResponse = await fetch(
            `/dispatch/${encodeURIComponent(emergencyId)}`
        );

        if (!dispatchResponse.ok) {
            throw new Error("Could not retrieve patient information.");
        }

        const dispatch = await dispatchResponse.json();

        const hospitalId = dispatch.emergency.allocatedHospitalId;
        const nhsNumber = dispatch.patient.nhsNumber;

        const response = await fetch(
            `/hospitals/${encodeURIComponent(hospitalId)}/patients/${encodeURIComponent(nhsNumber)}`,
            {
                method: "PUT",
                headers: {
                    "Content-Type": "application/json"
                },
                body: JSON.stringify({
                    medicalCondition: medicalCondition
                })
            }
        );

        if (!response.ok) {
            throw new Error(
                `Patient record update failed. Server returned ${response.status}.`
            );
        }

        patientUpdateMessage.textContent =
            "Patient medical record updated successfully.";

        patientUpdateMessage.style.color = "#16845b";

        await loadDispatch();

        patientUpdateMessage.textContent =
            "Patient medical record updated successfully.";

        patientUpdateMessage.style.color = "#16845b";

    } catch (error) {
        patientUpdateMessage.textContent = error.message;
        patientUpdateMessage.style.color = "#c0392b";

    } finally {
        updateButton.disabled = false;
    }
}


// Escape HTML to prevent unsafe content being inserted into the page
function escapeHTML(value) {
    return String(value ?? "").replace(/[&<>"']/g, character => ({
        "&": "&amp;",
        "<": "&lt;",
        ">": "&gt;",
        '"': "&quot;",
        "'": "&#039;"
    })[character]);
}
