package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DietaryType
import com.example.data.model.DiningHall
import com.example.data.model.MealSession
import com.example.data.model.Student
import com.example.data.model.StudentStatus
import com.example.data.model.VerificationStatus
import com.example.ui.viewmodel.MealCardViewModel

@Composable
fun AdminDashboardScreen(
    viewModel: MealCardViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val students by viewModel.filteredStudents.collectAsState()
    val allStudentsCount by viewModel.studentsList.collectAsState()
    val scanLogs by viewModel.scanLogsList.collectAsState()
    val searchQuery by viewModel.studentSearchQuery.collectAsState()
    val dietaryFilter by viewModel.selectedDietaryFilter.collectAsState()
    val hallFilter by viewModel.selectedHallFilter.collectAsState()
    val statusFilter by viewModel.selectedStatusFilter.collectAsState()
    val unsyncedCount by viewModel.unsyncedCount.collectAsState()

    var showReportDialog by remember { mutableStateOf(false) }
    var showAddStudentDialog by remember { mutableStateOf(false) }
    var studentToEdit by remember { mutableStateOf<Student?>(null) }

    // Aggregate statistics
    val grantedScans = scanLogs.filter { it.status == VerificationStatus.GRANTED }
    val totalServedToday = grantedScans.size
    val breakfastCount = grantedScans.count { it.mealSession == MealSession.BREAKFAST }
    val lunchCount = grantedScans.count { it.mealSession == MealSession.LUNCH }
    val dinnerCount = grantedScans.count { it.mealSession == MealSession.DINNER }
    val passbackViolations = scanLogs.count { it.status == VerificationStatus.REJECTED_PASSBACK }

    val totalCampusCapacity = 3000
    val capacityFraction = (totalServedToday.toFloat() / totalCampusCapacity).coerceIn(0f, 1f)

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Spacer(modifier = Modifier.height(10.dp))

                // Real-time Consumption & Headcount Executive Card
                ElevatedCard(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_headcount_card")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "LIVE CAMPUS HEADCOUNT",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    text = "Daily Meal Consumption",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Button(
                                onClick = { showReportDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00796B)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("generate_report_button")
                            ) {
                                Icon(imageVector = Icons.Default.Assignment, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Daily Report", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Headcount progress bar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Text(
                                text = "$totalServedToday Meals Served",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF004D40)
                            )
                            Text(
                                text = "Capacity: $totalCampusCapacity (${(capacityFraction * 100).toInt()}%)",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { capacityFraction },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = Color(0xFF00796B),
                            trackColor = Color(0xFFB2DFDB)
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Session Breakdown Pillars
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SessionMetricCard(
                                title = "Breakfast",
                                count = breakfastCount,
                                time = "06:30 - 09:00",
                                color = Color(0xFF00897B),
                                modifier = Modifier.weight(1f)
                            )
                            SessionMetricCard(
                                title = "Lunch",
                                count = lunchCount,
                                time = "11:45 - 14:30",
                                color = Color(0xFF00796B),
                                modifier = Modifier.weight(1f)
                            )
                            SessionMetricCard(
                                title = "Dinner",
                                count = dinnerCount,
                                time = "18:00 - 21:00",
                                color = Color(0xFF004D40),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Anti-Passback & Fraud Interception Banner
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFFFEBEE),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = null,
                                    tint = Color(0xFFC62828),
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "$passbackViolations Anti-Passback Duplicate Frauds Prevented",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = Color(0xFFC62828)
                                    )
                                    Text(
                                        text = "Saved ~$passbackViolations meal rations from unauthorized double-scanning.",
                                        fontSize = 10.sp,
                                        color = Color(0xFF5D4037)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Student Directory & Eligibility Management Title
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "STUDENT ELIGIBILITY ROSTER",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "Manage Access & Dietary Profiles (${students.size} / ${allStudentsCount.size})",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Button(
                        onClick = { showAddStudentDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF004D40)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("add_student_button")
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add", fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.updateStudentSearchQuery(it) },
                    placeholder = { Text("Search by student name, MTU ID, or department...", fontSize = 12.sp) },
                    leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = Color(0xFF00796B)) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.updateStudentSearchQuery("") }) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_search_bar")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Filter Chips Row (Dietary, Hall, Status)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        FilterChip(
                            selected = dietaryFilter == null && hallFilter == null && statusFilter == null,
                            onClick = {
                                viewModel.setDietaryFilter(null)
                                viewModel.setHallFilter(null)
                                viewModel.setStatusFilter(null)
                            },
                            label = { Text("All Students", fontSize = 11.sp) }
                        )
                    }
                    items(DietaryType.values()) { dietary ->
                        FilterChip(
                            selected = dietaryFilter == dietary,
                            onClick = {
                                viewModel.setDietaryFilter(if (dietaryFilter == dietary) null else dietary)
                            },
                            label = { Text(dietary.displayName.split(" ").first(), fontSize = 11.sp) }
                        )
                    }
                    items(DiningHall.values()) { hall ->
                        FilterChip(
                            selected = hallFilter == hall,
                            onClick = {
                                viewModel.setHallFilter(if (hallFilter == hall) null else hall)
                            },
                            label = { Text(hall.code, fontSize = 11.sp) }
                        )
                    }
                    items(StudentStatus.values()) { status ->
                        FilterChip(
                            selected = statusFilter == status,
                            onClick = {
                                viewModel.setStatusFilter(if (statusFilter == status) null else status)
                            },
                            label = { Text(status.name, fontSize = 11.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
            }

            // Student Roster List
            items(students) { student ->
                val isEligible = student.status == StudentStatus.ACTIVE
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .testTag("student_row_${student.mtuId.replace("/", "_")}")
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF004D40))
                                ) {
                                    Text(
                                        text = student.avatarSeed,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column {
                                    Text(
                                        text = student.fullName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "${student.department} • Year ${student.yearOfStudy}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "ID: ${student.mtuId}",
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 11.sp,
                                        color = Color(0xFF00796B)
                                    )
                                }
                            }

                            // Toggle eligibility switch
                            Column(horizontalAlignment = Alignment.End) {
                                Switch(
                                    checked = isEligible,
                                    onCheckedChange = { viewModel.toggleStudentStatus(student) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color(0xFF00796B),
                                        checkedTrackColor = Color(0xFFB2DFDB)
                                    ),
                                    modifier = Modifier.testTag("toggle_status_${student.mtuId.replace("/", "_")}")
                                )
                                Text(
                                    text = if (isEligible) "Active" else "Suspended",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isEligible) Color(0xFF2E7D32) else Color(0xFFC62828)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(student.dietaryType.badgeColor).copy(alpha = 0.12f)
                                ) {
                                    Text(
                                        text = student.dietaryType.displayName,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(student.dietaryType.badgeColor),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                ) {
                                    Text(
                                        text = student.diningHall.code,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            TextButton(
                                onClick = { studentToEdit = student },
                                modifier = Modifier.height(30.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Edit Diet/Hall", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }

    // Daily Report Dialog
    if (showReportDialog) {
        val reportText = remember { viewModel.generateDailyReportSummary() }
        AlertDialog(
            onDismissRequest = { showReportDialog = false },
            title = {
                Text("Daily Cafeteria Verification Report", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            },
            text = {
                Column {
                    Text(
                        text = "Real-time consumption and fraud prevention report generated for MTU Tepi campus dining administration.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF263238),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp)
                    ) {
                        LazyColumn(modifier = Modifier.padding(12.dp)) {
                            item {
                                Text(
                                    text = reportText,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    color = Color(0xFF80CBC4),
                                    lineHeight = 14.sp
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(reportText))
                        Toast.makeText(context, "Report copied to clipboard!", Toast.LENGTH_SHORT).show()
                        showReportDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00796B))
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copy Report")
                }
            },
            dismissButton = {
                TextButton(onClick = { showReportDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Add Student Dialog
    if (showAddStudentDialog) {
        var newId by remember { mutableStateOf("") }
        var newName by remember { mutableStateOf("") }
        var newDept by remember { mutableStateOf("Information Systems") }
        var newYear by remember { mutableStateOf("4") }
        var newHall by remember { mutableStateOf(DiningHall.HALL_1_MAIN) }
        var newDietary by remember { mutableStateOf(DietaryType.REGULAR_NON_FASTING) }

        AlertDialog(
            onDismissRequest = { showAddStudentDialog = false },
            title = { Text("Register New Student", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newId,
                        onValueChange = { newId = it },
                        label = { Text("MTU ID (e.g. MTU/16010/16)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Full Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newDept,
                        onValueChange = { newDept = it },
                        label = { Text("Department") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    // Dietary choice
                    Text("Dietary Profile:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(DietaryType.values()) { d ->
                            FilterChip(
                                selected = newDietary == d,
                                onClick = { newDietary = d },
                                label = { Text(d.displayName.split(" ").first(), fontSize = 10.sp) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newId.isNotBlank() && newName.isNotBlank()) {
                            viewModel.registerNewStudent(
                                mtuId = newId,
                                fullName = newName,
                                department = newDept,
                                year = newYear.toIntOrNull() ?: 4,
                                hall = newHall,
                                dietary = newDietary
                            )
                            showAddStudentDialog = false
                            Toast.makeText(context, "Student registered!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00796B))
                ) {
                    Text("Register")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddStudentDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Edit Dietary / Hall Dialog
    studentToEdit?.let { s ->
        var currentDietary by remember { mutableStateOf(s.dietaryType) }
        var currentHall by remember { mutableStateOf(s.diningHall) }

        AlertDialog(
            onDismissRequest = { studentToEdit = null },
            title = { Text("Edit Student Credentials", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(text = "${s.fullName} (${s.mtuId})", fontWeight = FontWeight.Bold)

                    Text("Dietary Requirement:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        DietaryType.values().forEach { d ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { currentDietary = d }
                                    .padding(vertical = 4.dp)
                            ) {
                                androidx.compose.material3.RadioButton(
                                    selected = currentDietary == d,
                                    onClick = { currentDietary = d }
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(d.displayName, fontSize = 12.sp)
                            }
                        }
                    }

                    Text("Assigned Dining Hall:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        DiningHall.values().forEach { h ->
                            FilterChip(
                                selected = currentHall == h,
                                onClick = { currentHall = h },
                                label = { Text(h.displayName, fontSize = 10.sp) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateStudentDietary(s, currentDietary)
                        viewModel.updateStudentHall(s, currentHall)
                        studentToEdit = null
                        Toast.makeText(context, "Student record updated!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00796B))
                ) {
                    Text("Save Changes")
                }
            },
            dismissButton = {
                TextButton(onClick = { studentToEdit = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SessionMetricCard(
    title: String,
    count: Int,
    time: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.12f),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title.uppercase(), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = color)
            Text("$count", fontSize = 18.sp, fontWeight = FontWeight.Black, color = color)
            Text(time, fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
