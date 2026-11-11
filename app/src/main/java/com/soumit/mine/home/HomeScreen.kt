package com.soumit.mine.home

import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soumit.mine.ui.theme.MineTheme
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    userName: String?,
    userEmail: String?,
    students: List<TuitionStudent>,
    payments: List<FeePayment>,
    onNavigateToCreate: () -> Unit,
    onRescheduleStudent: (studentId: String, oldDay: String, newDay: String, newTime: String) -> Unit,
    onDeleteScheduleSlot: (studentId: String, day: String, startTime: String) -> Unit,
    onDeleteStudent: (String) -> Unit,
    onRecordPayment: (studentId: String, studentName: String, amount: Double, date: String, notes: String) -> Unit,
    onDeletePayment: (String) -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val today = remember { Calendar.getInstance() }
    val todayName = remember { ScheduleHelper.getTodayName() }
    val tomorrowName = remember { ScheduleHelper.getTomorrowName() }

    // Selected routine filter: TODAY, TOMORROW, THIS_WEEK, FULL_TIMETABLE
    var selectedPeriod by rememberSaveable { mutableStateOf(RoutinePeriod.TODAY) }
    var selectedCalendarDay by rememberSaveable { mutableStateOf(todayName) }

    // Dialog & Sheet States
    var showSignOutDialog by remember { mutableStateOf(false) }
    var showDeleteManageSheet by remember { mutableStateOf(false) }
    var showRecordPaymentDialog by remember { mutableStateOf(false) }
    var studentForPayment by remember { mutableStateOf<TuitionStudent?>(null) }
    var classForAction by remember { mutableStateOf<ScheduledClass?>(null) }
    var showRescheduleDialog by remember { mutableStateOf(false) }

    // Financial summaries calculation
    val studentSummaries = remember(students, payments) {
        val cal = Calendar.getInstance()
        students.map { s ->
            val sPayments = payments.filter { it.studentId == s.id }
            val paid = sPayments.sumOf { it.amount }
            val billed = s.getTotalBilledToDate(cal)
            val diff = billed - paid
            val due = diff.coerceAtLeast(0.0)
            val advance = if (diff < 0) -diff else 0.0
            val lastPayment = sPayments.maxByOrNull { it.timestamp }

            val weeklyHours = s.getEstimatedWeeklyHours()
            val weeklyHoursFormatted = if (weeklyHours > 0) String.format(Locale.getDefault(), "%.1f hrs", weeklyHours) else s.studyTimeHours

            StudentFeeSummary(
                student = s,
                monthlyPay = s.monthlyPaymentDouble(),
                totalPaid = paid,
                totalBilled = billed,
                totalDue = due,
                advanceAmount = advance,
                weeklyHoursFormatted = weeklyHoursFormatted,
                hourlyRate = s.getHourlyRate(),
                countdown = s.getPaymentCountdownInfo(),
                paymentCount = sPayments.size,
                lastPaymentDate = lastPayment?.formattedDate()
            )
        }.sortedByDescending { it.totalDue }
    }

    val totalMonthlyRevenue = remember(students) {
        students.sumOf { it.monthlyPaymentDouble() }
    }
    val totalCollectedAllTime = remember(payments) {
        payments.sumOf { it.amount }
    }
    val totalPendingDues = remember(studentSummaries) {
        studentSummaries.sumOf { it.totalDue }
    }

    // Schedule per day
    val classesByDay = remember(students) {
        val map = mutableMapOf<String, MutableList<ScheduledClass>>()
        ScheduleHelper.DAYS_OF_WEEK.forEach { day -> map[day] = mutableListOf() }
        students.forEach { s ->
            s.weeklySchedule.forEach { slot ->
                val dayKey = ScheduleHelper.DAYS_OF_WEEK.firstOrNull { it.equals(slot.day, ignoreCase = true) } ?: slot.day
                map.getOrPut(dayKey) { mutableListOf() }.add(
                    ScheduledClass(
                        student = s,
                        dayOfWeek = dayKey,
                        startTime = slot.startTime,
                        duration = slot.duration
                    )
                )
            }
        }
        map.mapValues { (_, list) -> list.sortedBy { it.startTime } }
    }

    val displayedClasses = remember(selectedPeriod, selectedCalendarDay, classesByDay, todayName, tomorrowName) {
        when (selectedPeriod) {
            RoutinePeriod.TODAY -> classesByDay[todayName] ?: emptyList()
            RoutinePeriod.TOMORROW -> classesByDay[tomorrowName] ?: emptyList()
            RoutinePeriod.THIS_WEEK -> classesByDay[selectedCalendarDay] ?: emptyList()
            RoutinePeriod.FULL_TIMETABLE -> classesByDay.values.flatten()
        }
    }

    fun shareFullSchedule() {
        val shareText = buildString {
            appendLine("📚 Tuition Routine & Schedule")
            appendLine("═════════════════════════════")
            ScheduleHelper.DAYS_OF_WEEK.forEach { day ->
                val dayClasses = classesByDay[day] ?: emptyList()
                if (dayClasses.isNotEmpty()) {
                    appendLine()
                    appendLine("📅 $day (${dayClasses.size} classes):")
                    dayClasses.forEach { cls ->
                        appendLine("  • ${cls.startTime} (${cls.duration}): ${cls.student.name}")
                    }
                }
            }
            appendLine()
            appendLine("Total Students: ${students.size}")
            appendLine("Generated via Mine Tuition Diary")
        }

        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, shareText)
            type = "text/plain"
        }
        context.startActivity(Intent.createChooser(sendIntent, "Share Routine"))
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Mine",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = ScheduleHelper.getTodayFormattedDate(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { shareFullSchedule() }) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share Full Routine",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = { showSignOutDialog = true }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Sign Out"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            // Dual FABs: Delete / Manage + Create / Receive
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Red Danger FAB: "Delete / Manage"
                FloatingActionButton(
                    onClick = { showDeleteManageSheet = true },
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete & Manage")
                        Text("Delete", fontWeight = FontWeight.Bold)
                    }
                }

                // Primary FAB: "Create / Add"
                ExtendedFloatingActionButton(
                    onClick = onNavigateToCreate,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp),
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Create", fontWeight = FontWeight.Bold) }
                )
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Sunday-First Calendar Week Strip
            item {
                SundayFirstCalendarStrip(
                    selectedDay = selectedCalendarDay,
                    todayName = todayName,
                    classesByDay = classesByDay,
                    onSelectDay = { day ->
                        selectedCalendarDay = day
                        selectedPeriod = RoutinePeriod.THIS_WEEK
                    }
                )
            }

            // 2. Routine Filter Buttons (Today, Tomorrow, Week Day, Full Timetable)
            item {
                RoutineFilterBar(
                    selectedPeriod = selectedPeriod,
                    todayCount = classesByDay[todayName]?.size ?: 0,
                    tomorrowCount = classesByDay[tomorrowName]?.size ?: 0,
                    selectedDayName = selectedCalendarDay,
                    selectedDayCount = classesByDay[selectedCalendarDay]?.size ?: 0,
                    onSelectPeriod = { selectedPeriod = it }
                )
            }

            // 3. Compact Routine List / Timetable
            item {
                RoutineHeaderSection(
                    selectedPeriod = selectedPeriod,
                    selectedDay = selectedCalendarDay,
                    classCount = displayedClasses.size
                )
            }

            if (selectedPeriod == RoutinePeriod.FULL_TIMETABLE) {
                // Full Sunday to Saturday Timetable view matching Excel
                items(ScheduleHelper.DAYS_OF_WEEK) { day ->
                    val dayList = classesByDay[day] ?: emptyList()
                    TimetableDaySection(
                        day = day,
                        isToday = day.equals(todayName, ignoreCase = true),
                        classes = dayList,
                        onClassClick = { cls ->
                            classForAction = cls
                        }
                    )
                }
            } else {
                if (displayedClasses.isEmpty()) {
                    item {
                        EmptyRoutineCard(period = selectedPeriod, dayName = selectedCalendarDay)
                    }
                } else {
                    items(displayedClasses, key = { "${it.student.id}_${it.dayOfWeek}_${it.startTime}" }) { item ->
                        val summary = studentSummaries.find { it.student.id == item.student.id }
                        CompactClassCard(
                            scheduledClass = item,
                            dueAmount = summary?.totalDue ?: 0.0,
                            countdown = summary?.countdown,
                            onClick = { classForAction = item }
                        )
                    }
                }
            }

            // 4. Divider
            item {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
            }

            // 5. Tuition Fees & Dues Overview (All-in-One Dashboard)
            item {
                TuitionFinancialOverview(
                    totalMonthly = totalMonthlyRevenue,
                    totalCollected = totalCollectedAllTime,
                    totalPendingDue = totalPendingDues,
                    studentCount = students.size,
                    onReceiveFeeClick = {
                        studentForPayment = null
                        showRecordPaymentDialog = true
                    }
                )
            }

            // 6. Student Fees Ledger Table/Cards
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Student Ledger & Dues",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Based on start date & recorded payments",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (studentSummaries.isEmpty()) {
                item {
                    EmptyStudentsCard(onAddStudent = onNavigateToCreate)
                }
            } else {
                items(studentSummaries, key = { it.student.id }) { summary ->
                    StudentLedgerCard(
                        summary = summary,
                        onReceiveFee = {
                            studentForPayment = summary.student
                            showRecordPaymentDialog = true
                        }
                    )
                }
            }

            // 7. Recent Fee Receipts (Payment Ledger)
            item {
                RecentPaymentsSection(
                    payments = payments.take(10),
                    onDeletePayment = onDeletePayment
                )
            }
        }
    }

    // Class Action Bottom Sheet (Reschedule / Receive Fee / Call Parents)
    classForAction?.let { cls ->
        ClassActionSheet(
            scheduledClass = cls,
            onDismiss = { classForAction = null },
            onReschedule = {
                showRescheduleDialog = true
            },
            onReceiveFee = {
                studentForPayment = cls.student
                showRecordPaymentDialog = true
                classForAction = null
            },
            onDeleteSlot = {
                onDeleteScheduleSlot(cls.student.id, cls.dayOfWeek, cls.startTime)
                classForAction = null
            }
        )
    }

    // Reschedule Dialog
    if (showRescheduleDialog && classForAction != null) {
        val cls = classForAction!!
        RescheduleClassDialog(
            scheduledClass = cls,
            onDismiss = {
                showRescheduleDialog = false
                classForAction = null
            },
            onConfirm = { newDay, newTime ->
                onRescheduleStudent(cls.student.id, cls.dayOfWeek, newDay, newTime)
                showRescheduleDialog = false
                classForAction = null
            }
        )
    }

    // Record Payment Dialog
    if (showRecordPaymentDialog) {
        RecordPaymentDialog(
            students = students,
            preselectedStudentId = studentForPayment?.id,
            onDismiss = {
                showRecordPaymentDialog = false
                studentForPayment = null
            },
            onConfirm = { studentId, studentName, amount, date, notes ->
                onRecordPayment(studentId, studentName, amount, date, notes)
                showRecordPaymentDialog = false
                studentForPayment = null
            }
        )
    }

    // Delete / Manage Sheet
    if (showDeleteManageSheet) {
        DeleteAndManageSheet(
            students = students,
            payments = payments,
            onDismiss = { showDeleteManageSheet = false },
            onDeleteStudent = onDeleteStudent,
            onDeletePayment = onDeletePayment,
            onDeleteSlot = onDeleteScheduleSlot
        )
    }

    // Sign Out Dialog
    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            title = { Text("Sign Out", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to sign out of Mine?") },
            confirmButton = {
                Button(
                    onClick = {
                        showSignOutDialog = false
                        onSignOut()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Sign Out")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// ==========================================
// CALENDAR & ROUTINE COMPONENTS
// ==========================================

@Composable
private fun SundayFirstCalendarStrip(
    selectedDay: String,
    todayName: String,
    classesByDay: Map<String, List<ScheduledClass>>,
    onSelectDay: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ScheduleHelper.DAYS_OF_WEEK.forEach { dayName ->
                val isSelected = dayName.equals(selectedDay, ignoreCase = true)
                val isToday = dayName.equals(todayName, ignoreCase = true)
                val classCount = classesByDay[dayName]?.size ?: 0
                val dayLetter = dayName.take(3)

                Surface(
                    onClick = { onSelectDay(dayName) },
                    shape = RoundedCornerShape(16.dp),
                    color = when {
                        isSelected -> MaterialTheme.colorScheme.primary
                        isToday -> MaterialTheme.colorScheme.primaryContainer
                        else -> MaterialTheme.colorScheme.surfaceContainerHigh
                    },
                    contentColor = when {
                        isSelected -> MaterialTheme.colorScheme.onPrimary
                        isToday -> MaterialTheme.colorScheme.onPrimaryContainer
                        else -> MaterialTheme.colorScheme.onSurface
                    },
                    modifier = Modifier.width(62.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = dayLetter,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isSelected -> MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f)
                                        isToday -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                        else -> Color.Transparent
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$classCount",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = if (classCount > 0) FontWeight.Bold else FontWeight.Normal
                                )
                            )
                        }
                        if (isToday) {
                            Text(
                                text = "Today",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            )
                        } else {
                            Text(
                                text = if (classCount == 1) "class" else "classes",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RoutineFilterBar(
    selectedPeriod: RoutinePeriod,
    todayCount: Int,
    tomorrowCount: Int,
    selectedDayName: String,
    selectedDayCount: Int,
    onSelectPeriod: (RoutinePeriod) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = selectedPeriod == RoutinePeriod.TODAY,
            onClick = { onSelectPeriod(RoutinePeriod.TODAY) },
            label = { Text("Today ($todayCount)") },
            shape = RoundedCornerShape(12.dp)
        )
        FilterChip(
            selected = selectedPeriod == RoutinePeriod.TOMORROW,
            onClick = { onSelectPeriod(RoutinePeriod.TOMORROW) },
            label = { Text("Tomorrow ($tomorrowCount)") },
            shape = RoundedCornerShape(12.dp)
        )
        FilterChip(
            selected = selectedPeriod == RoutinePeriod.THIS_WEEK,
            onClick = { onSelectPeriod(RoutinePeriod.THIS_WEEK) },
            label = { Text("$selectedDayName ($selectedDayCount)") },
            shape = RoundedCornerShape(12.dp)
        )
        FilterChip(
            selected = selectedPeriod == RoutinePeriod.FULL_TIMETABLE,
            onClick = { onSelectPeriod(RoutinePeriod.FULL_TIMETABLE) },
            label = { Text("Weekly Routine") },
            leadingIcon = {
                Icon(
                    Icons.Default.CalendarMonth,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            },
            shape = RoundedCornerShape(12.dp)
        )
    }
}

@Composable
private fun RoutineHeaderSection(
    selectedPeriod: RoutinePeriod,
    selectedDay: String,
    classCount: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = when (selectedPeriod) {
                RoutinePeriod.TODAY -> "Today's Routine ($classCount)"
                RoutinePeriod.TOMORROW -> "Tomorrow's Routine ($classCount)"
                RoutinePeriod.THIS_WEEK -> "$selectedDay's Routine ($classCount)"
                RoutinePeriod.FULL_TIMETABLE -> "Full Weekly Timetable Matrix"
            },
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "Tap to move/fee",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

/**
 * Basic and compact class card showing strictly:
 * Student Name, Start Time, Duration, and Next Payment / Due status badge.
 */
@Composable
private fun CompactClassCard(
    scheduledClass: ScheduledClass,
    dueAmount: Double,
    countdown: PaymentCountdown?,
    onClick: () -> Unit
) {
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Student avatar icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = scheduledClass.student.name.take(1).uppercase(),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }

            // Name + Time
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = scheduledClass.student.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "${scheduledClass.startTime} (${scheduledClass.duration})",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Fee Status & Due Badge
            Column(horizontalAlignment = Alignment.End) {
                if (dueAmount > 0) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Due: ₹${String.format(Locale.getDefault(), "%.0f", dueAmount)}",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                } else {
                    Surface(
                        color = Color(0xFFE8F5E9),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Paid ✓",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF2E7D32),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                if (countdown != null) {
                    Text(
                        text = countdown.text,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            color = if (countdown.isUrgent) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
    }
}

/**
 * Full Timetable Day Section (Sunday to Saturday) matching Excel matrix.
 */
@Composable
private fun TimetableDaySection(
    day: String,
    isToday: Boolean,
    classes: List<ScheduledClass>,
    onClassClick: (ScheduledClass) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = day,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
            if (isToday) {
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "TODAY",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, color = Color.White),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                    )
                }
            }
            Text(
                text = "(${classes.size})",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (classes.isEmpty()) {
            Text(
                text = "No classes scheduled",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
            )
        } else {
            classes.forEach { cls ->
                OutlinedCard(
                    onClick = { onClassClick(cls) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = cls.startTime,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = cls.student.name,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }
                        Text(
                            text = cls.duration,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyRoutineCard(period: RoutinePeriod, dayName: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
            Text(
                text = when (period) {
                    RoutinePeriod.TODAY -> "No tuition classes scheduled for today!"
                    RoutinePeriod.TOMORROW -> "No tuition classes scheduled for tomorrow."
                    RoutinePeriod.THIS_WEEK -> "No classes scheduled for $dayName."
                    RoutinePeriod.FULL_TIMETABLE -> "No routine timetable entries created yet."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ==========================================
// FINANCIAL & LEDGER COMPONENTS
// ==========================================

@Composable
private fun TuitionFinancialOverview(
    totalMonthly: Double,
    totalCollected: Double,
    totalPendingDue: Double,
    studentCount: Int,
    onReceiveFeeClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Tuition Revenue & Dues",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "$studentCount enrolled students",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Button(
                onClick = onReceiveFeeClick,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.CurrencyRupee, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Receive Fee")
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Card 1: Monthly Potential
            OverviewStatCard(
                title = "Monthly",
                amount = "₹${String.format(Locale.getDefault(), "%,.0f", totalMonthly)}",
                subtitle = "Total Expected",
                icon = Icons.Default.CalendarMonth,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )

            // Card 2: Received / Collected
            OverviewStatCard(
                title = "Received",
                amount = "₹${String.format(Locale.getDefault(), "%,.0f", totalCollected)}",
                subtitle = "All-Time Paid",
                icon = Icons.Default.TrendingUp,
                tint = Color(0xFF2E7D32),
                modifier = Modifier.weight(1f)
            )

            // Card 3: Pending Due
            OverviewStatCard(
                title = "Due",
                amount = "₹${String.format(Locale.getDefault(), "%,.0f", totalPendingDue)}",
                subtitle = "To Collect",
                icon = Icons.Default.TrendingDown,
                tint = if (totalPendingDue > 0) MaterialTheme.colorScheme.error else Color(0xFF2E7D32),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun OverviewStatCard(
    title: String,
    amount: String,
    subtitle: String,
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = amount,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = tint,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Individual Student Ledger Card matching Excel calculations.
 */
@Composable
private fun StudentLedgerCard(
    summary: StudentFeeSummary,
    onReceiveFee: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header: Name & Receive Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = summary.student.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Started: ${summary.student.startDate.ifBlank { "N/A" }} • ${summary.weeklyHoursFormatted}/wk",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Button(
                    onClick = onReceiveFee,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("Receive", style = MaterialTheme.typography.labelMedium)
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            // Excel Metrics Grid: Monthly Pay, Total Paid, Total Due, Hourly Rate
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Monthly Pay
                Column {
                    Text("Monthly", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("₹${String.format(Locale.getDefault(), "%,.0f", summary.monthlyPay)}", fontWeight = FontWeight.Bold)
                }

                // Total Paid
                Column {
                    Text("Paid", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("₹${String.format(Locale.getDefault(), "%,.0f", summary.totalPaid)}", fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                }

                // Due
                Column {
                    Text("Due", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = "₹${String.format(Locale.getDefault(), "%,.0f", summary.totalDue)}",
                        fontWeight = FontWeight.Bold,
                        color = if (summary.totalDue > 0) MaterialTheme.colorScheme.error else Color(0xFF2E7D32)
                    )
                }

                // Hourly Rate
                Column {
                    Text("Hourly", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("₹${String.format(Locale.getDefault(), "%.1f", summary.hourlyRate)}/hr", fontWeight = FontWeight.SemiBold)
                }
            }

            // Countdown tag
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = summary.countdown.text,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = if (summary.countdown.isUrgent) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                )

                if (summary.lastPaymentDate != null) {
                    Text(
                        text = "Last paid: ${summary.lastPaymentDate}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyStudentsCard(onAddStudent: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(Icons.Default.PersonAdd, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp))
            Text(
                text = "No students enrolled yet",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = "Add your first tuition student to set up their routine timetable and monthly fees.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Button(onClick = onAddStudent, shape = RoundedCornerShape(10.dp)) {
                Text("Add New Student")
            }
        }
    }
}

/**
 * Recent fee payments log / receipts ledger.
 */
@Composable
private fun RecentPaymentsSection(
    payments: List<FeePayment>,
    onDeletePayment: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Recent Fee Receipts (${payments.size})",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        }

        if (payments.isEmpty()) {
            Text(
                text = "No payments recorded yet. Use 'Receive Fee' to record your first tuition payment.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            payments.forEach { p ->
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(p.studentName, fontWeight = FontWeight.Bold)
                            Text(
                                text = "${p.formattedDate()}${if (p.notes.isNotBlank()) " • ${p.notes}" else ""}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "₹${String.format(Locale.getDefault(), "%,.0f", p.amount)}",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2E7D32)
                            )
                            IconButton(onClick = { onDeletePayment(p.id) }) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete Payment", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// ACTION SHEETS & DIALOGS
// ==========================================

/**
 * Bottom sheet triggered when tapping a class card.
 * Provides: Reschedule / Move Time, Receive Fee, and Parent Call actions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClassActionSheet(
    scheduledClass: ScheduledClass,
    onDismiss: () -> Unit,
    onReschedule: () -> Unit,
    onReceiveFee: () -> Unit,
    onDeleteSlot: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        scheduledClass.student.name.take(1).uppercase(),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
                Column {
                    Text(
                        text = scheduledClass.student.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "${scheduledClass.dayOfWeek} at ${scheduledClass.startTime} (${scheduledClass.duration})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider()

            // 1. Reschedule / Move Time Action
            ElevatedCard(
                onClick = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        onDismiss()
                        onReschedule()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Default.EditCalendar, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Column {
                        Text("Reschedule / Move Time", fontWeight = FontWeight.Bold)
                        Text("Change class day of week or start time", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // 2. Receive Fee Action
            ElevatedCard(
                onClick = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        onDismiss()
                        onReceiveFee()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Default.CurrencyRupee, contentDescription = null, tint = Color(0xFF2E7D32))
                    Column {
                        Text("Receive Fee Payment", fontWeight = FontWeight.Bold)
                        Text("Record tuition payment for ${scheduledClass.student.name}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // 3. Contact Numbers (Call Father / Mother / Student)
            if (scheduledClass.student.phoneNumbers.isNotEmpty()) {
                Text("Contact Numbers", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                scheduledClass.student.phoneNumbers.forEach { contact ->
                    OutlinedCard(
                        onClick = {
                            val intent = Intent(Intent.ACTION_DIAL).apply {
                                data = Uri.parse("tel:${contact.number}")
                            }
                            context.startActivity(intent)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(contact.label, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                Text(contact.number, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Icon(Icons.Default.Call, contentDescription = "Call", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

            // 4. Delete Routine Slot
            TextButton(
                onClick = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        onDismiss()
                        onDeleteSlot()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.Default.DeleteOutline, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Remove This Class Slot From Routine")
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * Dialog to reschedule a class slot (change day of week and/or start time).
 */
@Composable
fun RescheduleClassDialog(
    scheduledClass: ScheduledClass,
    onDismiss: () -> Unit,
    onConfirm: (newDay: String, newTime: String) -> Unit
) {
    val context = LocalContext.current
    var selectedDay by remember { mutableStateOf(scheduledClass.dayOfWeek) }
    var selectedTime by remember { mutableStateOf(scheduledClass.startTime) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Reschedule Class", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Moving class for ${scheduledClass.student.name}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text("Select Day (Sunday First)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ScheduleHelper.DAYS_OF_WEEK.forEach { day ->
                        FilterChip(
                            selected = day.equals(selectedDay, ignoreCase = true),
                            onClick = { selectedDay = day },
                            label = { Text(day.take(3)) },
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }

                Text("Select Start Time", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                Button(
                    onClick = {
                        val cal = Calendar.getInstance()
                        TimePickerDialog(
                            context,
                            { _, hour, minute ->
                                val ampm = if (hour < 12) "AM" else "PM"
                                val h12 = when {
                                    hour == 0 -> 12
                                    hour > 12 -> hour - 12
                                    else -> hour
                                }
                                selectedTime = String.format(Locale.getDefault(), "%02d:%02d %s", h12, minute, ampm)
                            },
                            cal.get(Calendar.HOUR_OF_DAY),
                            cal.get(Calendar.MINUTE),
                            false
                        ).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Schedule, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(selectedTime)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedDay, selectedTime) },
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Confirm Move")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

/**
 * Dedicated Delete & Manage Sheet requested by user:
 * "add a delete button in the fab like the crate button which allows me to create the things i wanna delete"
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeleteAndManageSheet(
    students: List<TuitionStudent>,
    payments: List<FeePayment>,
    onDismiss: () -> Unit,
    onDeleteStudent: (String) -> Unit,
    onDeletePayment: (String) -> Unit,
    onDeleteSlot: (studentId: String, day: String, startTime: String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Students, 1: Payments, 2: Routine Slots
    var studentToDelete by remember { mutableStateOf<TuitionStudent?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                Text(
                    text = "Delete & Manage Records",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            }

            SecondaryTabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Students (${students.size})") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Payments (${payments.size})") }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Routine Slots") }
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(340.dp)
            ) {
                when (selectedTab) {
                    0 -> {
                        // Delete Students
                        if (students.isEmpty()) {
                            Text("No students to delete", modifier = Modifier.align(Alignment.Center))
                        } else {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(students) { s ->
                                    OutlinedCard(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column {
                                                Text(s.name, fontWeight = FontWeight.Bold)
                                                Text("Monthly: ₹${s.monthlyPayment}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                            IconButton(onClick = { studentToDelete = s }) {
                                                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    1 -> {
                        // Delete Payments
                        if (payments.isEmpty()) {
                            Text("No payment receipts recorded yet", modifier = Modifier.align(Alignment.Center))
                        } else {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(payments) { p ->
                                    OutlinedCard(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column {
                                                Text(p.studentName, fontWeight = FontWeight.Bold)
                                                Text("₹${p.amount} on ${p.formattedDate()}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                            IconButton(onClick = { onDeletePayment(p.id) }) {
                                                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    2 -> {
                        // Delete Routine Slots
                        val allSlots = students.flatMap { s ->
                            s.weeklySchedule.map { slot -> Pair(s, slot) }
                        }
                        if (allSlots.isEmpty()) {
                            Text("No routine slots to delete", modifier = Modifier.align(Alignment.Center))
                        } else {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(allSlots) { (s, slot) ->
                                    OutlinedCard(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column {
                                                Text("${slot.day}: ${s.name}", fontWeight = FontWeight.Bold)
                                                Text("${slot.startTime} (${slot.duration})", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                            IconButton(onClick = { onDeleteSlot(s.id, slot.day, slot.startTime) }) {
                                                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Confirmation dialog before deleting student
    studentToDelete?.let { s ->
        AlertDialog(
            onDismissRequest = { studentToDelete = null },
            title = { Text("Delete Student?", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete ${s.name}? This will remove their profile and routine slots.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteStudent(s.id)
                        studentToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { studentToDelete = null }) { Text("Cancel") }
            }
        )
    }
}

@Preview(showBackground = true, name = "Tuition Dashboard Preview")
@Composable
fun HomeScreenPreview() {
    val sampleStudents = listOf(
        TuitionStudent(
            id = "1",
            name = "Nidhruv Sarkar",
            startDate = "12/01/2026",
            monthlyPayment = "600",
            studyTimeHours = "6 hrs",
            weeklySchedule = listOf(
                DaySchedule(day = "Monday", startTime = "08:00 AM", duration = "1.5 hr"),
                DaySchedule(day = "Wednesday", startTime = "08:00 AM", duration = "1.5 hr"),
                DaySchedule(day = "Friday", startTime = "08:00 AM", duration = "1.5 hr"),
                DaySchedule(day = "Friday", startTime = "08:30 AM", duration = "1.5 hr")
            )
        ),
        TuitionStudent(
            id = "2",
            name = "Aradhya Dutta",
            startDate = "07/01/2026",
            monthlyPayment = "800",
            studyTimeHours = "4.5 hrs",
            weeklySchedule = listOf(
                DaySchedule(day = "Tuesday", startTime = "08:00 AM", duration = "1.5 hr"),
                DaySchedule(day = "Thursday", startTime = "08:00 AM", duration = "1.5 hr"),
                DaySchedule(day = "Thursday", startTime = "03:30 PM", duration = "1.5 hr")
            )
        ),
        TuitionStudent(
            id = "3",
            name = "Rudra Debnath",
            startDate = "06/04/2026",
            monthlyPayment = "1200",
            studyTimeHours = "6 hrs",
            weeklySchedule = listOf(
                DaySchedule(day = "Sunday", startTime = "08:30 AM", duration = "1.5 hr"),
                DaySchedule(day = "Monday", startTime = "09:00 PM", duration = "1.5 hr"),
                DaySchedule(day = "Wednesday", startTime = "08:30 PM", duration = "1.5 hr"),
                DaySchedule(day = "Friday", startTime = "08:30 PM", duration = "1.5 hr")
            )
        )
    )

    val samplePayments = listOf(
        FeePayment(id = "p1", studentId = "1", studentName = "Nidhruv Sarkar", amount = 600.0, date = "16/02/2026"),
        FeePayment(id = "p2", studentId = "1", studentName = "Nidhruv Sarkar", amount = 600.0, date = "10/03/2026"),
        FeePayment(id = "p3", studentId = "2", studentName = "Aradhya Dutta", amount = 800.0, date = "11/03/2026")
    )

    MineTheme {
        HomeScreen(
            userName = "Teacher",
            userEmail = "teacher@mine.app",
            students = sampleStudents,
            payments = samplePayments,
            onNavigateToCreate = {},
            onRescheduleStudent = { _, _, _, _ -> },
            onDeleteScheduleSlot = { _, _, _ -> },
            onDeleteStudent = {},
            onRecordPayment = { _, _, _, _, _ -> },
            onDeletePayment = {},
            onSignOut = {}
        )
    }
}

