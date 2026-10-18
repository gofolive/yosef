package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MealScanLog
import com.example.data.model.MealSession
import com.example.data.model.Student
import com.example.data.model.StudentStatus
import com.example.data.model.VerificationStatus
import com.example.ui.components.DynamicQrCodeView
import com.example.ui.viewmodel.MealCardViewModel

@Composable
fun StudentMealCardScreen(
    viewModel: MealCardViewModel,
    modifier: Modifier = Modifier
) {
    val selectedStudent by viewModel.selectedStudent.collectAsState()
    val dynamicToken by viewModel.dynamicToken.collectAsState()
    val remainingSeconds by viewModel.tokenRemainingSeconds.collectAsState()
    val allStudents by viewModel.studentsList.collectAsState()
    val activeSession by viewModel.activeSession.collectAsState()
    val allLogs by viewModel.scanLogsList.collectAsState()

    val studentLogs = selectedStudent?.let { s ->
        allLogs.filter { it.mtuId == s.mtuId }.take(5)
    } ?: emptyList()

    val student = selectedStudent

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Spacer(modifier = Modifier.height(12.dp))

            // Student profile demo switcher bar
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "SELECT STUDENT IDENTITY (DEMO SWITCHER):",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(allStudents) { s ->
                        val isCurrent = s.mtuId == student?.mtuId
                        FilterChip(
                            selected = isCurrent,
                            onClick = { viewModel.selectStudent(s) },
                            label = {
                                Text(
                                    text = "${s.fullName.split(" ").first()} (${s.mtuId.takeLast(5)})",
                                    fontSize = 11.sp,
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF00796B),
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.testTag("switch_student_${s.mtuId.replace("/", "_")}")
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
        }

        if (student == null) {
            item {
                Text(text = "Loading student credential...", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            item {
                // Official University Digital ID & Meal Card
                ElevatedCard(
                    shape = RoundedCornerShape(26.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("student_meal_card")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Card Header with Gold Ribbon
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(Color(0xFF004D40), Color(0xFF00796B))
                                    )
                                )
                                .padding(vertical = 10.dp, horizontal = 14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Restaurant,
                                        contentDescription = null,
                                        tint = Color(0xFFFFD54F),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "MTU DIGITAL MEAL PASS",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 12.sp,
                                        color = Color.White,
                                        letterSpacing = 1.sp
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (student.status == StudentStatus.ACTIVE) Color(0xFF00E676) else Color(0xFFFF5252)
                                ) {
                                    Text(
                                        text = student.status.name,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFF003314),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Student Info Strip
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Avatar Badge
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF004D40))
                                    .border(2.dp, Color(0xFFFFB300), CircleShape)
                            ) {
                                Text(
                                    text = student.avatarSeed,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = student.fullName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${student.department} • 4th Year",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "ID: ${student.mtuId}",
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color(0xFF00796B)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Dietary and Dining Hall Badges
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(student.dietaryType.badgeColor).copy(alpha = 0.12f),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text(
                                        text = "DIET PROFILE",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(student.dietaryType.badgeColor)
                                    )
                                    Text(
                                        text = student.dietaryType.displayName,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text(
                                        text = "ASSIGNED HALL",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = student.diningHall.displayName.split(" ").take(3).joinToString(" "),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Dynamic QR Code Component
                        if (dynamicToken != null) {
                            DynamicQrCodeView(
                                dynamicToken = dynamicToken!!,
                                remainingSeconds = remainingSeconds
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Security anti-passback info
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFECEFF1),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = null,
                                    tint = Color(0xFF00796B),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Anti-Passback Protected: One entry per meal window. QR code regenerates every 30 seconds to block screenshots.",
                                    fontSize = 10.sp,
                                    color = Color(0xFF37474F),
                                    lineHeight = 14.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Daily Meal Sessions Schedule & Status
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Today's Meal Access Windows",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        MealSession.values().forEach { session ->
                            val isCurrentActive = session == activeSession
                            val scanForThisSession = studentLogs.find { it.mealSession == session && it.status == VerificationStatus.GRANTED }
                            val isConsumed = scanForThisSession != null

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isCurrentActive) Color(0xFFE0F2F1) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable { viewModel.setActiveSession(session) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = session.sessionName,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = if (isCurrentActive) Color(0xFF004D40) else MaterialTheme.colorScheme.onSurface
                                            )
                                            if (isCurrentActive) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = Color(0xFF00796B)
                                                ) {
                                                    Text(
                                                        text = "NOW ACTIVE",
                                                        fontSize = 8.sp,
                                                        color = Color.White,
                                                        fontWeight = FontWeight.Black,
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }
                                        Text(
                                            text = session.timeWindowString(),
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = when {
                                            isConsumed -> Color(0xFF2E7D32)
                                            isCurrentActive -> Color(0xFFFFB300)
                                            else -> Color(0xFF78909C)
                                        }
                                    ) {
                                        Text(
                                            text = when {
                                                isConsumed -> "CONSUMED (${scanForThisSession.formattedTime()})"
                                                isCurrentActive -> "READY TO SCAN"
                                                else -> "SCHEDULED"
                                            },
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Student Recent Dining Logs
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Your Dining Pass History",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        if (studentLogs.isEmpty()) {
                            Text(
                                text = "No dining scans logged yet today for this student.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        } else {
                            studentLogs.forEach { log ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = if (log.status.isApproved) Icons.Default.CheckCircle else Icons.Default.Error,
                                            contentDescription = null,
                                            tint = if (log.status.isApproved) Color(0xFF2E7D32) else Color(0xFFC62828),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = "${log.mealSession.sessionName} • ${log.diningHall.code}",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                text = log.status.displayName,
                                                fontSize = 10.sp,
                                                color = if (log.status.isApproved) Color(0xFF2E7D32) else Color(0xFFC62828)
                                            )
                                        }
                                    }

                                    Text(
                                        text = log.formattedTime(),
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
