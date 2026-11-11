package com.soumit.mine.home

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateScreen(
    students: List<TuitionStudent>,
    onBack: () -> Unit,
    onNavigateToCreateStudent: () -> Unit,
    onRecordPayment: (studentId: String, studentName: String, amount: Double, date: String, notes: String) -> Unit,
    onAddScheduleSlot: (studentId: String, day: String, startTime: String, duration: String) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    var showPaymentDialog by remember { mutableStateOf(false) }
    var showAddSlotDialog by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Tuition Actions",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "What would you like to do?",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )

            // 1. New Tuition Student
            CreateActionCard(
                title = "New Tuition Student",
                subtitle = "Add new student profile, parent contacts, monthly fee & weekly routine",
                icon = Icons.Default.PersonAdd,
                iconTint = MaterialTheme.colorScheme.primary,
                onClick = onNavigateToCreateStudent
            )

            // 2. Receive Fee Payment
            CreateActionCard(
                title = "Receive Fee Payment",
                subtitle = "Record fees received from a student to update their paid and due balance",
                icon = Icons.Default.CurrencyRupee,
                iconTint = MaterialTheme.colorScheme.tertiary,
                onClick = { showPaymentDialog = true }
            )

            // 3. Add Class Slot to Routine
            CreateActionCard(
                title = "Add Routine Time Slot",
                subtitle = "Add an extra or new class day/time to an existing student's weekly routine",
                icon = Icons.Default.DateRange,
                iconTint = MaterialTheme.colorScheme.secondary,
                onClick = { showAddSlotDialog = true }
            )
        }
    }

    if (showPaymentDialog) {
        RecordPaymentDialog(
            students = students,
            onDismiss = { showPaymentDialog = false },
            onConfirm = { studentId, studentName, amount, date, notes ->
                onRecordPayment(studentId, studentName, amount, date, notes)
                showPaymentDialog = false
            }
        )
    }

    if (showAddSlotDialog) {
        AddSlotDialog(
            students = students,
            onDismiss = { showAddSlotDialog = false },
            onConfirm = { studentId, day, time, duration ->
                onAddScheduleSlot(studentId, day, time, duration)
                showAddSlotDialog = false
            }
        )
    }
}

@Composable
private fun CreateActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(iconTint.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(28.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * Dialog to record tuition fee received from a student.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordPaymentDialog(
    students: List<TuitionStudent>,
    preselectedStudentId: String? = null,
    onDismiss: () -> Unit,
    onConfirm: (studentId: String, studentName: String, amount: Double, date: String, notes: String) -> Unit
) {
    val context = LocalContext.current
    var selectedStudent by remember {
        mutableStateOf(students.find { it.id == preselectedStudentId } ?: students.firstOrNull())
    }
    var expandedDropdown by remember { mutableStateOf(false) }

    var amountText by remember(selectedStudent) {
        mutableStateOf(selectedStudent?.monthlyPayment ?: "")
    }

    val todayStr = remember {
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
    }
    var paymentDate by remember { mutableStateOf(todayStr) }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CurrencyRupee,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Receive Fee",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (students.isEmpty()) {
                    Text(
                        text = "No students enrolled yet. Please add a student first.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    // Student Selector Dropdown
                    ExposedDropdownMenuBox(
                        expanded = expandedDropdown,
                        onExpandedChange = { expandedDropdown = !expandedDropdown }
                    ) {
                        OutlinedTextField(
                            value = selectedStudent?.name ?: "Select Student",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Student") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDropdown) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                            shape = RoundedCornerShape(12.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = expandedDropdown,
                            onDismissRequest = { expandedDropdown = false }
                        ) {
                            students.forEach { s ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(s.name, fontWeight = FontWeight.SemiBold)
                                            Text(
                                                "Monthly: ₹${s.monthlyPayment}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    },
                                    onClick = {
                                        selectedStudent = s
                                        amountText = s.monthlyPayment
                                        expandedDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    // Amount Field
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                        label = { Text("Amount Received (₹)") },
                        leadingIcon = {
                            Icon(Icons.Default.CurrencyRupee, contentDescription = null)
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Date Picker
                    OutlinedTextField(
                        value = paymentDate,
                        onValueChange = { paymentDate = it },
                        label = { Text("Payment Date (DD/MM/YYYY)") },
                        leadingIcon = {
                            Icon(Icons.Default.CalendarMonth, contentDescription = null)
                        },
                        trailingIcon = {
                            IconButton(onClick = {
                                val cal = Calendar.getInstance()
                                DatePickerDialog(
                                    context,
                                    { _, year, month, dayOfMonth ->
                                        paymentDate = String.format(Locale.getDefault(), "%02d/%02d/%04d", dayOfMonth, month + 1, year)
                                    },
                                    cal.get(Calendar.YEAR),
                                    cal.get(Calendar.MONTH),
                                    cal.get(Calendar.DAY_OF_MONTH)
                                ).show()
                            }) {
                                Icon(Icons.Default.CalendarMonth, contentDescription = "Pick Date")
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Notes / Mode
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Payment Mode / Notes (Optional)") },
                        placeholder = { Text("e.g. Cash, GPay, Partial, Advance") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val student = selectedStudent
                    val amt = amountText.toDoubleOrNull() ?: 0.0
                    if (student != null && amt > 0) {
                        onConfirm(student.id, student.name, amt, paymentDate, notes)
                    }
                },
                enabled = selectedStudent != null && (amountText.toDoubleOrNull() ?: 0.0) > 0.0,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Confirm Receipt")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Dialog to add a routine slot to an existing student.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSlotDialog(
    students: List<TuitionStudent>,
    onDismiss: () -> Unit,
    onConfirm: (studentId: String, day: String, time: String, duration: String) -> Unit
) {
    val context = LocalContext.current
    var selectedStudent by remember { mutableStateOf(students.firstOrNull()) }
    var expandedStudent by remember { mutableStateOf(false) }

    var selectedDay by remember { mutableStateOf(ScheduleHelper.DAYS_OF_WEEK.first()) }
    var expandedDay by remember { mutableStateOf(false) }

    var selectedTime by remember { mutableStateOf("08:00 AM") }
    var durationText by remember { mutableStateOf("1.5 hr") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(Icons.Default.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("Add Class Slot", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (students.isEmpty()) {
                    Text("No students available. Please add a student first.", color = MaterialTheme.colorScheme.error)
                } else {
                    // Student selector
                    ExposedDropdownMenuBox(
                        expanded = expandedStudent,
                        onExpandedChange = { expandedStudent = !expandedStudent }
                    ) {
                        OutlinedTextField(
                            value = selectedStudent?.name ?: "Select Student",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Student") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedStudent) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                            shape = RoundedCornerShape(12.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = expandedStudent,
                            onDismissRequest = { expandedStudent = false }
                        ) {
                            students.forEach { s ->
                                DropdownMenuItem(
                                    text = { Text(s.name) },
                                    onClick = {
                                        selectedStudent = s
                                        expandedStudent = false
                                    }
                                )
                            }
                        }
                    }

                    // Day of Week selector (Sunday first!)
                    ExposedDropdownMenuBox(
                        expanded = expandedDay,
                        onExpandedChange = { expandedDay = !expandedDay }
                    ) {
                        OutlinedTextField(
                            value = selectedDay,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Day of Week") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDay) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                            shape = RoundedCornerShape(12.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = expandedDay,
                            onDismissRequest = { expandedDay = false }
                        ) {
                            ScheduleHelper.DAYS_OF_WEEK.forEach { d ->
                                DropdownMenuItem(
                                    text = { Text(d) },
                                    onClick = {
                                        selectedDay = d
                                        expandedDay = false
                                    }
                                )
                            }
                        }
                    }

                    // Time Picker
                    OutlinedTextField(
                        value = selectedTime,
                        onValueChange = { selectedTime = it },
                        readOnly = true,
                        label = { Text("Start Time") },
                        leadingIcon = { Icon(Icons.Default.Schedule, contentDescription = null) },
                        trailingIcon = {
                            IconButton(onClick = {
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
                            }) {
                                Icon(Icons.Default.Schedule, contentDescription = "Pick Time")
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Duration
                    OutlinedTextField(
                        value = durationText,
                        onValueChange = { durationText = it },
                        label = { Text("Class Duration") },
                        placeholder = { Text("e.g. 1 hr, 1.5 hr, 2 hr") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val s = selectedStudent
                    if (s != null) {
                        onConfirm(s.id, selectedDay, selectedTime, durationText)
                    }
                },
                enabled = selectedStudent != null,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Add Slot")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
