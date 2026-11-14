package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.DailyLog
import com.example.data.model.DoseLog
import com.example.data.model.EmergencyContact
import com.example.data.model.MedicalAppointment
import com.example.data.model.MedicalRecord
import com.example.data.model.MedicationEntry
import com.example.data.model.PatientProfile
import com.example.data.model.SeizureLog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class AuraCareRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    private fun <T> Flow<T>.handleStreamErrors(uid: String, operation: OperationType, path: String): Flow<T> =
        catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val exceptionToLog = firestoreError ?: (error as? Exception)
            if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                handleFirestoreError(exceptionToLog, operation, path, auth)
            }
            throw error
        }

    // --- Patient Profile ---

    fun observeProfile(userId: String = requireUserId()): Flow<PatientProfile?> = flow {
        val path = "users/$userId"
        emitAll(
            db.collection("users").document(userId)
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(PatientProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    } else null
                }
                .handleStreamErrors(userId, OperationType.GET, path)
        )
    }

    suspend fun saveProfile(profile: PatientProfile): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        return try {
            val existing = docRef.get().await()
            if (existing.exists()) {
                val updates = mapOf(
                    "fullName" to profile.fullName.ifBlank { "Patient" }.take(100),
                    "dateOfBirth" to profile.dateOfBirth.take(30),
                    "epilepsyType" to profile.epilepsyType.take(100),
                    "primaryNeurologist" to profile.primaryNeurologist.take(100),
                    "neurologistEmail" to profile.neurologistEmail.take(120),
                    "dietaryPreference" to profile.dietaryPreference.ifBlank { "Balanced" }.take(40),
                    "allergies" to profile.allergies.take(300),
                    "medicalNotes" to profile.medicalNotes.take(1000),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.update(updates).await()
            } else {
                val payload = mapOf(
                    "userId" to uid,
                    "fullName" to profile.fullName.ifBlank { "Patient" }.take(100),
                    "dateOfBirth" to profile.dateOfBirth.take(30),
                    "epilepsyType" to profile.epilepsyType.take(100),
                    "primaryNeurologist" to profile.primaryNeurologist.take(100),
                    "neurologistEmail" to profile.neurologistEmail.take(120),
                    "dietaryPreference" to profile.dietaryPreference.ifBlank { "Balanced" }.take(40),
                    "allergies" to profile.allergies.take(300),
                    "medicalNotes" to profile.medicalNotes.take(1000),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path, auth)
            Result.failure(e)
        }
    }

    // --- Seizures ---

    fun observeSeizures(userId: String = requireUserId()): Flow<List<SeizureLog>> = flow {
        val path = "users/$userId/seizures"
        emitAll(
            db.collection("users").document(userId).collection("seizures")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(SeizureLog::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.occurredAtMillis }
                }
                .handleStreamErrors(userId, OperationType.LIST, path)
        )
    }

    suspend fun saveSeizure(entry: SeizureLog): Result<String> {
        val uid = requireUserId()
        val col = db.collection("users").document(uid).collection("seizures")
        val isCreate = entry.id.isBlank()
        val docRef = if (isCreate) col.document() else col.document(entry.id)
        return try {
            val baseFields = mapOf(
                "seizureType" to entry.seizureType.ifBlank { "Focal Aware" }.take(60),
                "durationSeconds" to entry.durationSeconds.coerceIn(0L, 86400L),
                "occurredAtMillis" to entry.occurredAtMillis.coerceAtLeast(0L),
                "triggers" to entry.triggers.take(15),
                "symptomsAndAura" to entry.symptomsAndAura.take(1000),
                "medicationSideEffects" to entry.medicationSideEffects.take(15),
                "postIctalNotes" to entry.postIctalNotes.take(1500),
                "severity" to entry.severity.coerceIn(1, 5),
                "emergencyNotified" to entry.emergencyNotified,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (isCreate) {
                docRef.set(baseFields + ("createdAt" to FieldValue.serverTimestamp())).await()
            } else {
                docRef.update(baseFields).await()
            }
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, if (isCreate) OperationType.CREATE else OperationType.UPDATE, docRef.path, auth)
            Result.failure(e)
        }
    }

    suspend fun deleteSeizure(seizureId: String): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("seizures").document(seizureId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path, auth)
            Result.failure(e)
        }
    }

    // --- Medications ---

    fun observeMedications(userId: String = requireUserId()): Flow<List<MedicationEntry>> = flow {
        val path = "users/$userId/medications"
        emitAll(
            db.collection("users").document(userId).collection("medications")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(MedicationEntry::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedWith(compareBy<MedicationEntry> { it.status != "current" }.thenBy { it.name.lowercase() })
                }
                .handleStreamErrors(userId, OperationType.LIST, path)
        )
    }

    suspend fun saveMedication(med: MedicationEntry): Result<String> {
        val uid = requireUserId()
        val col = db.collection("users").document(uid).collection("medications")
        val isCreate = med.id.isBlank()
        val docRef = if (isCreate) col.document() else col.document(med.id)
        val status = if (med.status == "past") "past" else "current"
        return try {
            val baseFields = mapOf(
                "name" to med.name.ifBlank { "Medication" }.take(100),
                "dosage" to med.dosage.ifBlank { "As prescribed" }.take(60),
                "frequency" to med.frequency.ifBlank { "Daily" }.take(60),
                "scheduleTimes" to med.scheduleTimes.take(8),
                "status" to status,
                "prescribedBy" to med.prescribedBy.take(100),
                "startDate" to med.startDate.take(30),
                "endDate" to med.endDate.take(30),
                "stopReason" to med.stopReason.take(300),
                "sideEffectsNoted" to med.sideEffectsNoted.take(15),
                "reminderEnabled" to med.reminderEnabled,
                "notes" to med.notes.take(500),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (isCreate) {
                docRef.set(baseFields + ("createdAt" to FieldValue.serverTimestamp())).await()
            } else {
                docRef.update(baseFields).await()
            }
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, if (isCreate) OperationType.CREATE else OperationType.UPDATE, docRef.path, auth)
            Result.failure(e)
        }
    }

    suspend fun toggleMedicationReminder(medicationId: String, enabled: Boolean): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("medications").document(medicationId)
        return try {
            docRef.update(
                mapOf(
                    "reminderEnabled" to enabled,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path, auth)
            Result.failure(e)
        }
    }

    suspend fun deleteMedication(medicationId: String): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("medications").document(medicationId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path, auth)
            Result.failure(e)
        }
    }

    // --- Dose Logs ---

    fun observeDoseLogs(userId: String = requireUserId()): Flow<List<DoseLog>> = flow {
        val path = "users/$userId/doseLogs"
        emitAll(
            db.collection("users").document(userId).collection("doseLogs")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(DoseLog::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .handleStreamErrors(userId, OperationType.LIST, path)
        )
    }

    suspend fun setDoseTaken(
        medicationId: String,
        medicationName: String,
        dosage: String,
        scheduledTime: String,
        dateKey: String,
        taken: Boolean
    ): Result<Unit> {
        val uid = requireUserId()
        val docId = "${medicationId}_${dateKey}_${scheduledTime.replace(":", "")}"
        val docRef = db.collection("users").document(uid).collection("doseLogs").document(docId)
        return try {
            val existing = docRef.get().await()
            if (existing.exists()) {
                docRef.update(
                    mapOf(
                        "taken" to taken,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "medicationId" to medicationId,
                        "medicationName" to medicationName.take(100),
                        "dosage" to dosage.take(60),
                        "scheduledTime" to scheduledTime.take(20),
                        "dateKey" to dateKey.take(20),
                        "taken" to taken,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path, auth)
            Result.failure(e)
        }
    }

    // --- Daily Activities & Food Intake ---

    fun observeDailyLogs(userId: String = requireUserId()): Flow<List<DailyLog>> = flow {
        val path = "users/$userId/dailyLogs"
        emitAll(
            db.collection("users").document(userId).collection("dailyLogs")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(DailyLog::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.loggedAtMillis }
                }
                .handleStreamErrors(userId, OperationType.LIST, path)
        )
    }

    suspend fun saveDailyLog(log: DailyLog): Result<String> {
        val uid = requireUserId()
        val col = db.collection("users").document(uid).collection("dailyLogs")
        val isCreate = log.id.isBlank()
        val docRef = if (isCreate) col.document() else col.document(log.id)
        val entryType = if (log.entryType == "food") "food" else "activity"
        return try {
            val baseFields = mapOf(
                "entryType" to entryType,
                "title" to log.title.ifBlank { "Daily Entry" }.take(120),
                "category" to log.category.ifBlank { "General" }.take(60),
                "mealTime" to log.mealTime.take(20),
                "durationMinutes" to log.durationMinutes.coerceIn(0, 1440),
                "caloriesOrAmount" to log.caloriesOrAmount.take(80),
                "dietTags" to log.dietTags.take(10),
                "notes" to log.notes.take(500),
                "loggedAtMillis" to log.loggedAtMillis.coerceAtLeast(0L),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (isCreate) {
                docRef.set(baseFields + ("createdAt" to FieldValue.serverTimestamp())).await()
            } else {
                docRef.update(baseFields).await()
            }
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, if (isCreate) OperationType.CREATE else OperationType.UPDATE, docRef.path, auth)
            Result.failure(e)
        }
    }

    suspend fun deleteDailyLog(logId: String): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("dailyLogs").document(logId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path, auth)
            Result.failure(e)
        }
    }

    // --- Appointments ---

    fun observeAppointments(userId: String = requireUserId()): Flow<List<MedicalAppointment>> = flow {
        val path = "users/$userId/appointments"
        emitAll(
            db.collection("users").document(userId).collection("appointments")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(MedicalAppointment::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.appointmentMillis }
                }
                .handleStreamErrors(userId, OperationType.LIST, path)
        )
    }

    suspend fun saveAppointment(appointment: MedicalAppointment): Result<String> {
        val uid = requireUserId()
        val col = db.collection("users").document(uid).collection("appointments")
        val isCreate = appointment.id.isBlank()
        val docRef = if (isCreate) col.document() else col.document(appointment.id)
        return try {
            val baseFields = mapOf(
                "title" to appointment.title.ifBlank { "Medical Visit" }.take(120),
                "doctorName" to appointment.doctorName.take(100),
                "clinicLocation" to appointment.clinicLocation.take(200),
                "providerEmail" to appointment.providerEmail.take(120),
                "appointmentMillis" to appointment.appointmentMillis.coerceAtLeast(0L),
                "notes" to appointment.notes.take(800),
                "linkedToCalendar" to appointment.linkedToCalendar,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (isCreate) {
                docRef.set(baseFields + ("createdAt" to FieldValue.serverTimestamp())).await()
            } else {
                docRef.update(baseFields).await()
            }
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, if (isCreate) OperationType.CREATE else OperationType.UPDATE, docRef.path, auth)
            Result.failure(e)
        }
    }

    suspend fun markAppointmentLinkedToCalendar(appointmentId: String): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("appointments").document(appointmentId)
        return try {
            docRef.update(
                mapOf(
                    "linkedToCalendar" to true,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path, auth)
            Result.failure(e)
        }
    }

    suspend fun deleteAppointment(appointmentId: String): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("appointments").document(appointmentId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path, auth)
            Result.failure(e)
        }
    }

    // --- Emergency Contacts ---

    fun observeEmergencyContacts(userId: String = requireUserId()): Flow<List<EmergencyContact>> = flow {
        val path = "users/$userId/emergencyContacts"
        emitAll(
            db.collection("users").document(userId).collection("emergencyContacts")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(EmergencyContact::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.isPrimary }
                }
                .handleStreamErrors(userId, OperationType.LIST, path)
        )
    }

    suspend fun saveEmergencyContact(contact: EmergencyContact): Result<String> {
        val uid = requireUserId()
        val col = db.collection("users").document(uid).collection("emergencyContacts")
        val isCreate = contact.id.isBlank()
        val docRef = if (isCreate) col.document() else col.document(contact.id)
        return try {
            val baseFields = mapOf(
                "name" to contact.name.ifBlank { "Contact" }.take(100),
                "relationship" to contact.relationship.take(60),
                "phone" to contact.phone.take(40),
                "email" to contact.email.take(120),
                "isPrimary" to contact.isPrimary,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (isCreate) {
                docRef.set(baseFields + ("createdAt" to FieldValue.serverTimestamp())).await()
            } else {
                docRef.update(baseFields).await()
            }
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, if (isCreate) OperationType.CREATE else OperationType.UPDATE, docRef.path, auth)
            Result.failure(e)
        }
    }

    suspend fun deleteEmergencyContact(contactId: String): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("emergencyContacts").document(contactId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path, auth)
            Result.failure(e)
        }
    }

    // --- Medical Records ---

    fun observeMedicalRecords(userId: String = requireUserId()): Flow<List<MedicalRecord>> = flow {
        val path = "users/$userId/medicalRecords"
        emitAll(
            db.collection("users").document(userId).collection("medicalRecords")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(MedicalRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.recordDate }
                }
                .handleStreamErrors(userId, OperationType.LIST, path)
        )
    }

    suspend fun saveMedicalRecord(record: MedicalRecord): Result<String> {
        val uid = requireUserId()
        val col = db.collection("users").document(uid).collection("medicalRecords")
        val isCreate = record.id.isBlank()
        val docRef = if (isCreate) col.document() else col.document(record.id)
        return try {
            val baseFields = mapOf(
                "title" to record.title.ifBlank { "Medical Record" }.take(120),
                "recordType" to record.recordType.ifBlank { "Lab Test" }.take(60),
                "recordDate" to record.recordDate.take(30),
                "facilityOrDoctor" to record.facilityOrDoctor.take(120),
                "findingsSummary" to record.findingsSummary.take(2000),
                "fileName" to record.fileName.take(200),
                "mimeType" to record.mimeType.take(100),
                "localFileUri" to record.localFileUri.take(500),
                "fileSizeBytes" to record.fileSizeBytes.coerceIn(0L, 500_000_000L),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (isCreate) {
                docRef.set(baseFields + ("createdAt" to FieldValue.serverTimestamp())).await()
            } else {
                docRef.update(baseFields).await()
            }
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, if (isCreate) OperationType.CREATE else OperationType.UPDATE, docRef.path, auth)
            Result.failure(e)
        }
    }

    suspend fun deleteMedicalRecord(recordId: String): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("medicalRecords").document(recordId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path, auth)
            Result.failure(e)
        }
    }
}
