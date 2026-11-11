package com.soumit.mine.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Calendar

data class TuitionUiState(
    val isLoading: Boolean = false,
    val students: List<TuitionStudent> = emptyList(),
    val payments: List<FeePayment> = emptyList(),
    val errorMessage: String? = null,
    val successMessage: String? = null
) {
    /**
     * Total expected monthly earnings across all enrolled students.
     */
    val totalMonthlyEarnings: Double
        get() = students.sumOf { it.monthlyPaymentDouble() }

    /**
     * Total fees received to date.
     */
    val totalCollectedAllTime: Double
        get() = payments.sumOf { it.amount }

    /**
     * Summary list with financial calculations for each student.
     */
    val studentSummaries: List<StudentFeeSummary>
        get() {
            val today = Calendar.getInstance()
            return students.map { student ->
                val studentPayments = payments.filter { it.studentId == student.id }
                val totalPaid = studentPayments.sumOf { it.amount }
                val totalBilled = student.getTotalBilledToDate(today)
                val diff = totalBilled - totalPaid
                val due = diff.coerceAtLeast(0.0)
                val advance = if (diff < 0) -diff else 0.0

                val lastPayment = studentPayments.maxByOrNull { it.timestamp }
                val lastPaymentDateStr = lastPayment?.formattedDate()

                val weeklyHours = student.getEstimatedWeeklyHours()
                val weeklyHoursFormatted = if (weeklyHours > 0) String.format("%.1f hrs", weeklyHours) else student.studyTimeHours

                StudentFeeSummary(
                    student = student,
                    monthlyPay = student.monthlyPaymentDouble(),
                    totalPaid = totalPaid,
                    totalBilled = totalBilled,
                    totalDue = due,
                    advanceAmount = advance,
                    weeklyHoursFormatted = weeklyHoursFormatted,
                    hourlyRate = student.getHourlyRate(),
                    countdown = student.getPaymentCountdownInfo(),
                    paymentCount = studentPayments.size,
                    lastPaymentDate = lastPaymentDateStr
                )
            }.sortedByDescending { it.totalDue }
        }

    /**
     * Sum of all pending dues across all students.
     */
    val totalPendingDues: Double
        get() = studentSummaries.sumOf { it.totalDue }
}

class HomeViewModel : ViewModel() {

    private val firestore = Firebase.firestore
    private val auth = Firebase.auth

    private val _uiState = MutableStateFlow(TuitionUiState(isLoading = true))
    val uiState: StateFlow<TuitionUiState> = _uiState.asStateFlow()

    private var studentsListener: ListenerRegistration? = null
    private var paymentsListener: ListenerRegistration? = null

    init {
        startListening()
    }

    fun startListening() {
        val currentUserId = auth.currentUser?.uid
        if (currentUserId.isNullOrEmpty()) {
            _uiState.update { it.copy(isLoading = false) }
            return
        }

        listenStudents(currentUserId)
        listenPayments(currentUserId)
    }

    private fun listenStudents(userId: String) {
        studentsListener?.remove()
        val ref = firestore.collection("users").document(userId).collection("students")
        studentsListener = ref.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("HomeViewModel", "Error fetching students", error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val list = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(TuitionStudent::class.java)?.apply { id = doc.id }
                }.sortedBy { it.name.lowercase() }
                _uiState.update { it.copy(isLoading = false, students = list) }
            }
        }
    }

    private fun listenPayments(userId: String) {
        paymentsListener?.remove()
        val ref = firestore.collection("users").document(userId).collection("fee_payments")
        paymentsListener = ref.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("HomeViewModel", "Error fetching fee payments", error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val list = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(FeePayment::class.java)?.apply { id = doc.id }
                }.sortedByDescending { it.timestamp }
                _uiState.update { it.copy(payments = list) }
            }
        }
    }

    // ==========================================
    // STUDENT ACTIONS
    // ==========================================

    fun addStudent(
        student: TuitionStudent,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val currentUserId = auth.currentUser?.uid
        if (currentUserId.isNullOrEmpty()) {
            onError("You must be logged in to save student data")
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val studentWithTeacher = student.copy(teacherId = currentUserId)
                val docRef = if (studentWithTeacher.id.isBlank()) {
                    firestore.collection("users").document(currentUserId).collection("students").document()
                } else {
                    firestore.collection("users").document(currentUserId).collection("students").document(studentWithTeacher.id)
                }

                studentWithTeacher.id = docRef.id
                docRef.set(studentWithTeacher).await()

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        successMessage = "Student ${student.name} saved successfully!"
                    )
                }
                onSuccess()
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error adding student", e)
                val errorMsg = e.localizedMessage ?: "Failed to save student"
                _uiState.update { it.copy(isLoading = false, errorMessage = errorMsg) }
                onError(errorMsg)
            }
        }
    }

    fun updateStudentSchedule(
        studentId: String,
        oldDay: String,
        newDay: String,
        newTime: String,
        onSuccess: () -> Unit = {}
    ) {
        val currentUserId = auth.currentUser?.uid ?: return
        val currentStudent = _uiState.value.students.find { it.id == studentId } ?: return

        val updatedSchedule = currentStudent.weeklySchedule.map { schedule ->
            if (schedule.day.equals(oldDay, ignoreCase = true)) {
                schedule.copy(day = newDay, startTime = newTime)
            } else {
                schedule
            }
        }

        viewModelScope.launch {
            try {
                firestore.collection("users")
                    .document(currentUserId)
                    .collection("students")
                    .document(studentId)
                    .update("weeklySchedule", updatedSchedule)
                    .await()
                _uiState.update { it.copy(successMessage = "Schedule moved successfully!") }
                onSuccess()
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error updating schedule", e)
                _uiState.update { it.copy(errorMessage = "Could not update schedule: ${e.localizedMessage}") }
            }
        }
    }

    fun deleteScheduleSlot(
        studentId: String,
        day: String,
        startTime: String,
        onSuccess: () -> Unit = {}
    ) {
        val currentUserId = auth.currentUser?.uid ?: return
        val currentStudent = _uiState.value.students.find { it.id == studentId } ?: return

        val updatedSchedule = currentStudent.weeklySchedule.filterNot {
            it.day.equals(day, ignoreCase = true) && it.startTime.equals(startTime, ignoreCase = true)
        }

        viewModelScope.launch {
            try {
                firestore.collection("users")
                    .document(currentUserId)
                    .collection("students")
                    .document(studentId)
                    .update("weeklySchedule", updatedSchedule)
                    .await()
                _uiState.update { it.copy(successMessage = "Schedule slot deleted!") }
                onSuccess()
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error deleting slot", e)
            }
        }
    }

    fun deleteStudent(studentId: String, onSuccess: () -> Unit = {}) {
        val currentUserId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                // Delete student doc
                firestore.collection("users")
                    .document(currentUserId)
                    .collection("students")
                    .document(studentId)
                    .delete()
                    .await()
                _uiState.update { it.copy(successMessage = "Student removed successfully!") }
                onSuccess()
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error deleting student", e)
            }
        }
    }

    // ==========================================
    // FEE RECEIPT ACTIONS
    // ==========================================

    fun recordPayment(
        studentId: String,
        studentName: String,
        amount: Double,
        date: String,
        notes: String = "",
        onSuccess: () -> Unit = {}
    ) {
        val currentUserId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                val ref = firestore.collection("users")
                    .document(currentUserId)
                    .collection("fee_payments")
                    .document()

                val payment = FeePayment(
                    id = ref.id,
                    teacherId = currentUserId,
                    studentId = studentId,
                    studentName = studentName,
                    amount = amount,
                    date = date.trim(),
                    timestamp = System.currentTimeMillis(),
                    notes = notes.trim()
                )
                ref.set(payment).await()
                _uiState.update {
                    it.copy(successMessage = "₹$amount fee received from $studentName!")
                }
                onSuccess()
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error recording payment", e)
            }
        }
    }

    fun deletePayment(paymentId: String, onSuccess: () -> Unit = {}) {
        val currentUserId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                firestore.collection("users")
                    .document(currentUserId)
                    .collection("fee_payments")
                    .document(paymentId)
                    .delete()
                    .await()
                _uiState.update { it.copy(successMessage = "Payment record deleted!") }
                onSuccess()
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error deleting payment", e)
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }

    override fun onCleared() {
        super.onCleared()
        studentsListener?.remove()
        paymentsListener?.remove()
    }
}
