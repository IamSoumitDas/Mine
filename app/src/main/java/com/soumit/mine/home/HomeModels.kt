package com.soumit.mine.home

import com.google.firebase.firestore.PropertyName
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Custom phone contact with a label (e.g., Father, Mother, Student, Home, etc.)
 */
data class ContactPhone(
    val label: String = "Father",
    val number: String = ""
)

/**
 * Schedule for a specific day of the week with a custom start time and duration.
 */
data class DaySchedule(
    val day: String = "Sunday",          // Sunday, Monday, Tuesday, etc. (Sunday first!)
    val startTime: String = "08:00 AM",  // e.g. 08:00 AM
    val duration: String = "1.5 hr"      // e.g. 1 hr, 1.5 hr, 2 hr
)

/**
 * Student profile entity stored in Firestore under users/{teacherId}/students/{id}
 */
data class TuitionStudent(
    @get:PropertyName("id") @set:PropertyName("id")
    var id: String = UUID.randomUUID().toString(),

    @get:PropertyName("teacherId") @set:PropertyName("teacherId")
    var teacherId: String = "",

    @get:PropertyName("name") @set:PropertyName("name")
    var name: String = "",

    @get:PropertyName("fatherName") @set:PropertyName("fatherName")
    var fatherName: String = "",

    @get:PropertyName("motherName") @set:PropertyName("motherName")
    var motherName: String = "",

    @get:PropertyName("phoneNumbers") @set:PropertyName("phoneNumbers")
    var phoneNumbers: List<ContactPhone> = emptyList(),

    @get:PropertyName("studentClass") @set:PropertyName("studentClass")
    var studentClass: String = "",

    @get:PropertyName("school") @set:PropertyName("school")
    var school: String = "",

    @get:PropertyName("dob") @set:PropertyName("dob")
    var dob: String = "",

    @get:PropertyName("startDate") @set:PropertyName("startDate")
    var startDate: String = "", // Format: DD/MM/YYYY

    @get:PropertyName("monthlyPayment") @set:PropertyName("monthlyPayment")
    var monthlyPayment: String = "", // e.g. "600" or "5000"

    @get:PropertyName("studyTimeHours") @set:PropertyName("studyTimeHours")
    var studyTimeHours: String = "1.5 hr",

    @get:PropertyName("weeklySchedule") @set:PropertyName("weeklySchedule")
    var weeklySchedule: List<DaySchedule> = emptyList(),

    @get:PropertyName("notes") @set:PropertyName("notes")
    var notes: String = "",

    @get:PropertyName("createdAt") @set:PropertyName("createdAt")
    var createdAt: Long = System.currentTimeMillis()
) {
    /**
     * Parses the monthly payment as Double.
     */
    fun monthlyPaymentDouble(): Double {
        return monthlyPayment.replace(",", "").replace("₹", "").trim().toDoubleOrNull() ?: 0.0
    }

    /**
     * Calculates the number of complete billing cycles elapsed from startDate until today.
     * Matches the user's Excel model:
     * - If startDate is 12/01/2026 and today is 09/11/2026: 9 months.
     * - If startDate is 07/01/2026 and today is 09/11/2026: 10 months.
     */
    fun getBillingCyclesElapsed(today: Calendar = Calendar.getInstance()): Int {
        if (startDate.isBlank()) return 0
        return try {
            val parts = startDate.split("/", "-")
            if (parts.size < 3) return 0
            val startDay = parts[0].toIntOrNull() ?: 1
            val startMonth = parts[1].toIntOrNull() ?: 1 // 1-12
            val startYear = parts[2].toIntOrNull() ?: today.get(Calendar.YEAR)

            val currentYear = today.get(Calendar.YEAR)
            val currentMonth = today.get(Calendar.MONTH) + 1 // 1-12
            val currentDay = today.get(Calendar.DAY_OF_MONTH)

            var months = (currentYear - startYear) * 12 + (currentMonth - startMonth)
            if (currentDay < startDay) {
                months -= 1
            }
            months.coerceAtLeast(0)
        } catch (_: Exception) {
            0
        }
    }

    /**
     * Total expected tuition fees billed to this student to date.
     */
    fun getTotalBilledToDate(today: Calendar = Calendar.getInstance()): Double {
        return getBillingCyclesElapsed(today) * monthlyPaymentDouble()
    }

    /**
     * Calculates the days remaining until the next monthly fee due date.
     */
    fun getPaymentCountdownInfo(): PaymentCountdown {
        if (startDate.isBlank()) {
            return PaymentCountdown(days = 0, text = "No start date set", isUrgent = false)
        }

        var dueDay = 1
        try {
            val parts = startDate.split("/", "-")
            if (parts.isNotEmpty()) {
                dueDay = parts[0].toIntOrNull() ?: 1
            }
        } catch (_: Exception) {
            dueDay = 1
        }

        val todayZero = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val nextDueCal = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, dueDay.coerceIn(1, getActualMaximum(Calendar.DAY_OF_MONTH)))
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // If this month's due date has already passed, move to next month
        if (nextDueCal.before(todayZero)) {
            nextDueCal.add(Calendar.MONTH, 1)
            val maxDay = nextDueCal.getActualMaximum(Calendar.DAY_OF_MONTH)
            nextDueCal.set(Calendar.DAY_OF_MONTH, dueDay.coerceAtMost(maxDay))
        }

        val diffMillis = nextDueCal.timeInMillis - todayZero.timeInMillis
        val days = (diffMillis / (1000 * 60 * 60 * 24)).toInt()

        val sdf = SimpleDateFormat("MMM d", Locale.getDefault())
        val formattedDate = sdf.format(nextDueCal.time)

        return when {
            days == 0 -> PaymentCountdown(days = 0, text = "Fee Due Today!", isUrgent = true)
            days == 1 -> PaymentCountdown(days = 1, text = "Fee Due Tomorrow ($formattedDate)", isUrgent = true)
            days in 2..5 -> PaymentCountdown(days = days, text = "Due in $days d ($formattedDate)", isUrgent = true)
            else -> PaymentCountdown(days = days, text = "Next: $formattedDate (in $days d)", isUrgent = false)
        }
    }

    /**
     * Estimates weekly study hours from schedule (or studyTimeHours if not specified).
     */
    fun getEstimatedWeeklyHours(): Double {
        if (weeklySchedule.isEmpty()) return 0.0
        // Parse duration like "1.5 hr", "1 hr", "2 hr"
        val total = weeklySchedule.sumOf { item ->
            val num = item.duration.replace("hr", "").replace("hrs", "").replace("hour", "").trim().toDoubleOrNull() ?: 1.5
            num
        }
        return total
    }

    /**
     * Hourly rate calculation matching Excel:
     * Monthly Pay / (Weekly Hours * 4.2857 weeks per month)
     */
    fun getHourlyRate(): Double {
        val weeklyHours = getEstimatedWeeklyHours()
        if (weeklyHours <= 0.0) return 0.0
        val monthlyHours = weeklyHours * (52.0 / 12.0) // ~4.33 weeks per month
        val pay = monthlyPaymentDouble()
        return if (monthlyHours > 0) pay / monthlyHours else 0.0
    }
}

/**
 * Fee payment receipt record stored under users/{teacherId}/fee_payments/{id}
 */
data class FeePayment(
    @get:PropertyName("id") @set:PropertyName("id")
    var id: String = UUID.randomUUID().toString(),

    @get:PropertyName("teacherId") @set:PropertyName("teacherId")
    var teacherId: String = "",

    @get:PropertyName("studentId") @set:PropertyName("studentId")
    var studentId: String = "",

    @get:PropertyName("studentName") @set:PropertyName("studentName")
    var studentName: String = "",

    @get:PropertyName("amount") @set:PropertyName("amount")
    var amount: Double = 0.0,

    @get:PropertyName("date") @set:PropertyName("date")
    var date: String = "", // e.g. "16/02/2026" or "Nov 9, 2026"

    @get:PropertyName("timestamp") @set:PropertyName("timestamp")
    var timestamp: Long = System.currentTimeMillis(),

    @get:PropertyName("notes") @set:PropertyName("notes")
    var notes: String = "" // e.g. "March fee", "Cash", "UPI", "Partial"
) {
    fun formattedDate(): String {
        return if (date.isNotBlank()) date
        else SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(timestamp))
    }
}

/**
 * Computed financial summary for a student.
 */
data class StudentFeeSummary(
    val student: TuitionStudent,
    val monthlyPay: Double,
    val totalPaid: Double,
    val totalBilled: Double,
    val totalDue: Double,
    val advanceAmount: Double,
    val weeklyHoursFormatted: String,
    val hourlyRate: Double,
    val countdown: PaymentCountdown,
    val paymentCount: Int,
    val lastPaymentDate: String?
)

data class PaymentCountdown(
    val days: Int,
    val text: String,
    val isUrgent: Boolean
)

/**
 * Scheduled class event for routine rendering
 */
data class ScheduledClass(
    val student: TuitionStudent,
    val dayOfWeek: String,
    val startTime: String,
    val duration: String
)

/**
 * Filter for home routine view: Today, Tomorrow, This Week, or All Timetable
 */
enum class RoutinePeriod {
    TODAY,
    TOMORROW,
    THIS_WEEK,
    FULL_TIMETABLE
}

object ScheduleHelper {
    // Sunday first as requested!
    val DAYS_OF_WEEK = listOf(
        "Sunday",
        "Monday",
        "Tuesday",
        "Wednesday",
        "Thursday",
        "Friday",
        "Saturday"
    )

    fun getDayOfWeekName(calendar: Calendar = Calendar.getInstance()): String {
        return SimpleDateFormat("EEEE", Locale.ENGLISH).format(calendar.time)
    }

    fun getTodayName(): String = getDayOfWeekName(Calendar.getInstance())

    fun getTomorrowName(): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, 1)
        return getDayOfWeekName(cal)
    }

    fun getTodayFormattedDate(): String {
        return SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault()).format(Date())
    }

    fun getTomorrowFormattedDate(): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, 1)
        return SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault()).format(cal.time)
    }
}
