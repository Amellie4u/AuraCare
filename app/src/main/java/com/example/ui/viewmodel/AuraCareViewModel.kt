package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.DailyLog
import com.example.data.model.DoseLog
import com.example.data.model.EmergencyContact
import com.example.data.model.MedicalAppointment
import com.example.data.model.MedicalRecord
import com.example.data.model.MedicationEntry
import com.example.data.model.PatientProfile
import com.example.data.model.SeizureLog
import com.example.data.repository.AuraCareRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

data class SeizureTimerState(
    val isRunning: Boolean = false,
    val elapsedSeconds: Long = 0L,
    val startedAtMillis: Long = 0L
)

class AuraCareViewModel(
    private val repository: AuraCareRepository,
    val currentUserId: String
) : ViewModel() {

    private fun <T> Flow<T>.toUiState(label: String): StateFlow<UiState<T>> =
        map<T, UiState<T>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing $label", rootError)
                emit(UiState.Error(rootError.message ?: "Failed to load $label"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
                initialValue = UiState.Loading
            )

    val profileState: StateFlow<UiState<PatientProfile?>> =
        repository.observeProfile(currentUserId).toUiState("profile")

    val seizuresState: StateFlow<UiState<List<SeizureLog>>> =
        repository.observeSeizures(currentUserId).toUiState("seizures")

    val medicationsState: StateFlow<UiState<List<MedicationEntry>>> =
        repository.observeMedications(currentUserId).toUiState("medications")

    val doseLogsState: StateFlow<UiState<List<DoseLog>>> =
        repository.observeDoseLogs(currentUserId).toUiState("dose logs")

    val dailyLogsState: StateFlow<UiState<List<DailyLog>>> =
        repository.observeDailyLogs(currentUserId).toUiState("daily logs")

    val appointmentsState: StateFlow<UiState<List<MedicalAppointment>>> =
        repository.observeAppointments(currentUserId).toUiState("appointments")

    val contactsState: StateFlow<UiState<List<EmergencyContact>>> =
        repository.observeEmergencyContacts(currentUserId).toUiState("emergency contacts")

    val recordsState: StateFlow<UiState<List<MedicalRecord>>> =
        repository.observeMedicalRecords(currentUserId).toUiState("medical records")

    private val _timerState = MutableStateFlow(SeizureTimerState())
    val timerState: StateFlow<SeizureTimerState> = _timerState.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    private var timerJob: Job? = null

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    fun showMessage(message: String) {
        _snackbarMessage.value = message
    }

    fun startTimer() {
        if (_timerState.value.isRunning) return
        val startMs = if (_timerState.value.startedAtMillis > 0L) {
            _timerState.value.startedAtMillis
        } else {
            System.currentTimeMillis()
        }
        _timerState.value = _timerState.value.copy(isRunning = true, startedAtMillis = startMs)
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_timerState.value.isRunning) {
                delay(1000L)
                _timerState.value = _timerState.value.copy(
                    elapsedSeconds = _timerState.value.elapsedSeconds + 1L
                )
            }
        }
    }

    fun stopTimer(): Pair<Long, Long> {
        timerJob?.cancel()
        val current = _timerState.value
        _timerState.value = current.copy(isRunning = false)
        val started = if (current.startedAtMillis > 0L) current.startedAtMillis else System.currentTimeMillis()
        return current.elapsedSeconds to started
    }

    fun resetTimer() {
        timerJob?.cancel()
        _timerState.value = SeizureTimerState()
    }

    fun saveProfile(profile: PatientProfile) {
        viewModelScope.launch {
            repository.saveProfile(profile.copy(userId = currentUserId))
                .onSuccess { _snackbarMessage.value = "Patient profile saved" }
                .onFailure { _snackbarMessage.value = it.localizedMessage ?: "Could not save profile" }
        }
    }

    fun saveSeizure(entry: SeizureLog, onSaved: () -> Unit = {}) {
        viewModelScope.launch {
            repository.saveSeizure(entry)
                .onSuccess {
                    _snackbarMessage.value = "Seizure log recorded"
                    onSaved()
                }
                .onFailure { _snackbarMessage.value = it.localizedMessage ?: "Failed to save seizure log" }
        }
    }

    fun deleteSeizure(id: String) {
        viewModelScope.launch {
            repository.deleteSeizure(id)
                .onSuccess { _snackbarMessage.value = "Seizure entry removed" }
                .onFailure { _snackbarMessage.value = it.localizedMessage ?: "Delete failed" }
        }
    }

    fun saveMedication(med: MedicationEntry, onSaved: () -> Unit = {}) {
        viewModelScope.launch {
            repository.saveMedication(med)
                .onSuccess {
                    _snackbarMessage.value = if (med.status == "past") "Past medication archived" else "Medication saved"
                    onSaved()
                }
                .onFailure { _snackbarMessage.value = it.localizedMessage ?: "Failed to save medication" }
        }
    }

    fun toggleMedicationReminder(medId: String, enabled: Boolean) {
        viewModelScope.launch {
            repository.toggleMedicationReminder(medId, enabled)
                .onSuccess {
                    _snackbarMessage.value = if (enabled) "Dose alert enabled" else "Dose alert paused"
                }
                .onFailure { _snackbarMessage.value = it.localizedMessage ?: "Could not update reminder" }
        }
    }

    fun deleteMedication(id: String) {
        viewModelScope.launch {
            repository.deleteMedication(id)
                .onSuccess { _snackbarMessage.value = "Medication removed" }
                .onFailure { _snackbarMessage.value = it.localizedMessage ?: "Delete failed" }
        }
    }

    fun setDoseTaken(
        medicationId: String,
        medicationName: String,
        dosage: String,
        scheduledTime: String,
        dateKey: String,
        taken: Boolean
    ) {
        viewModelScope.launch {
            repository.setDoseTaken(medicationId, medicationName, dosage, scheduledTime, dateKey, taken)
                .onSuccess {
                    _snackbarMessage.value = if (taken) "$medicationName ($scheduledTime) marked taken" else "Dose marked pending"
                }
                .onFailure { _snackbarMessage.value = it.localizedMessage ?: "Failed to update dose" }
        }
    }

    fun saveDailyLog(log: DailyLog, onSaved: () -> Unit = {}) {
        viewModelScope.launch {
            repository.saveDailyLog(log)
                .onSuccess {
                    _snackbarMessage.value = if (log.entryType == "food") "Food intake logged" else "Activity logged"
                    onSaved()
                }
                .onFailure { _snackbarMessage.value = it.localizedMessage ?: "Failed to save log" }
        }
    }

    fun deleteDailyLog(id: String) {
        viewModelScope.launch {
            repository.deleteDailyLog(id)
                .onSuccess { _snackbarMessage.value = "Daily entry removed" }
                .onFailure { _snackbarMessage.value = it.localizedMessage ?: "Delete failed" }
        }
    }

    fun saveAppointment(appointment: MedicalAppointment, onSaved: () -> Unit = {}) {
        viewModelScope.launch {
            repository.saveAppointment(appointment)
                .onSuccess {
                    _snackbarMessage.value = "Appointment saved"
                    onSaved()
                }
                .onFailure { _snackbarMessage.value = it.localizedMessage ?: "Failed to save appointment" }
        }
    }

    fun markAppointmentLinked(appointmentId: String) {
        viewModelScope.launch {
            repository.markAppointmentLinkedToCalendar(appointmentId)
        }
    }

    fun deleteAppointment(id: String) {
        viewModelScope.launch {
            repository.deleteAppointment(id)
                .onSuccess { _snackbarMessage.value = "Appointment removed" }
                .onFailure { _snackbarMessage.value = it.localizedMessage ?: "Delete failed" }
        }
    }

    fun saveEmergencyContact(contact: EmergencyContact, onSaved: () -> Unit = {}) {
        viewModelScope.launch {
            repository.saveEmergencyContact(contact)
                .onSuccess {
                    _snackbarMessage.value = "Emergency contact saved"
                    onSaved()
                }
                .onFailure { _snackbarMessage.value = it.localizedMessage ?: "Failed to save contact" }
        }
    }

    fun deleteEmergencyContact(id: String) {
        viewModelScope.launch {
            repository.deleteEmergencyContact(id)
                .onSuccess { _snackbarMessage.value = "Emergency contact removed" }
                .onFailure { _snackbarMessage.value = it.localizedMessage ?: "Delete failed" }
        }
    }

    fun saveMedicalRecord(record: MedicalRecord, onSaved: () -> Unit = {}) {
        viewModelScope.launch {
            repository.saveMedicalRecord(record)
                .onSuccess {
                    _snackbarMessage.value = "Medical record added to profile"
                    onSaved()
                }
                .onFailure { _snackbarMessage.value = it.localizedMessage ?: "Failed to save record" }
        }
    }

    fun deleteMedicalRecord(id: String) {
        viewModelScope.launch {
            repository.deleteMedicalRecord(id)
                .onSuccess { _snackbarMessage.value = "Medical record removed" }
                .onFailure { _snackbarMessage.value = it.localizedMessage ?: "Delete failed" }
        }
    }

    private companion object {
        const val TAG = "AuraCareVM"
        const val STOP_TIMEOUT_MS = 5000L
    }
}
