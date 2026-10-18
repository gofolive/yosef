package com.example.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DiningHall
import com.example.data.model.VerificationStatus
import com.example.ui.components.AntiPassbackAlertModal
import com.example.ui.viewmodel.MealCardViewModel

@Composable
fun GatekeeperScannerScreen(
    viewModel: MealCardViewModel,
    modifier: Modifier = Modifier
) {
    val gatekeeperHall by viewModel.gatekeeperHall.collectAsState()
    val activeSession by viewModel.activeSession.collectAsState()
    val isOfflineMode by viewModel.isOfflineMode.collectAsState()
    val manualIdInput by viewModel.manualIdInput.collectAsState()
    val verificationResult by viewModel.verificationResult.collectAsState()
    val scanLogs by viewModel.scanLogsList.collectAsState()

    var showManualInput by remember { mutableStateOf(false) }

    // Shift Statistics
    val shiftLogs = scanLogs.filter { it.mealSession == activeSession }
    val totalScanned = shiftLogs.size
    val grantedCount = shiftLogs.count { it.status == VerificationStatus.GRANTED }
    val passbackViolations = shiftLogs.count { it.status == VerificationStatus.REJECTED_PASSBACK }
    val ineligibles = shiftLogs.count { !it.status.isApproved && it.status != VerificationStatus.REJECTED_PASSBACK }

    // Viewfinder laser animation
    val infiniteTransition = rememberInfiniteTransition(label = "scanner_laser")
    val laserYRatio by infiniteTransition.animateFloat(
        initialValue = 0.05f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser_y"
    )

    // Verification Result Modal Dialog
    AntiPassbackAlertModal(
        result = verificationResult,
        onDismiss = { viewModel.dismissVerificationModal() }
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Spacer(modifier = Modifier.height(10.dp))

            // Gatekeeper Station & Dining Hall Selection
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "VERIFICATION STATION",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${gatekeeperHall.code} • Turnstile Gate 1",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF004D40)
                        )
                    }

                    // Hall switch chips
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        DiningHall.values().forEach { hall ->
                            FilterChip(
                                selected = gatekeeperHall == hall,
                                onClick = { viewModel.setGatekeeperHall(hall) },
                                label = {
                                    Text(
                                        text = hall.code,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF00796B),
                                    selectedLabelColor = Color.White
                                ),
                                modifier = Modifier.testTag("hall_chip_${hall.code.lowercase()}")
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Interactive Viewfinder & Scanner Simulation Box
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0E1715)),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("scanner_viewfinder_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Viewfinder Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF00E676))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "LIVE ANTI-PASSBACK SCANNER",
                                color = Color(0xFF80CBC4),
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                letterSpacing = 0.5.sp
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isOfflineMode) Color(0xFFD84315) else Color(0xFF004D40)
                        ) {
                            Text(
                                text = if (isOfflineMode) "OFFLINE CACHE ACTIVE" else "ONLINE SERVER",
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Scanner Viewport Box with animated laser
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(240.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFF142421))
                            .border(2.dp, Color(0xFF00BFA5).copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            // Target corner reticles
                            val strokeW = 4f
                            val cornerLen = 32f
                            val color = Color(0xFF00E676)

                            // Top-left corner
                            drawLine(color, Offset(20f, 20f), Offset(20f + cornerLen, 20f), strokeW)
                            drawLine(color, Offset(20f, 20f), Offset(20f, 20f + cornerLen), strokeW)

                            // Top-right corner
                            drawLine(color, Offset(size.width - 20f, 20f), Offset(size.width - 20f - cornerLen, 20f), strokeW)
                            drawLine(color, Offset(size.width - 20f, 20f), Offset(size.width - 20f, 20f + cornerLen), strokeW)

                            // Bottom-left corner
                            drawLine(color, Offset(20f, size.height - 20f), Offset(20f + cornerLen, size.height - 20f), strokeW)
                            drawLine(color, Offset(20f, size.height - 20f), Offset(20f, size.height - 20f - cornerLen), strokeW)

                            // Bottom-right corner
                            drawLine(color, Offset(size.width - 20f, size.height - 20f), Offset(size.width - 20f - cornerLen, size.height - 20f), strokeW)
                            drawLine(color, Offset(size.width - 20f, size.height - 20f), Offset(size.width - 20f, size.height - 20f - cornerLen), strokeW)

                            // Animated Laser Sweep
                            val currentLaserY = size.height * laserYRatio
                            drawRect(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color(0x3300E676),
                                        Color(0xCC00E676),
                                        Color.Transparent
                                    ),
                                    startY = currentLaserY - 18f,
                                    endY = currentLaserY + 18f
                                ),
                                topLeft = Offset(0f, currentLaserY - 18f),
                                size = Size(size.width, 36f)
                            )
                            drawLine(
                                color = Color(0xFF00E676),
                                start = Offset(16f, currentLaserY),
                                end = Offset(size.width - 16f, currentLaserY),
                                strokeWidth = 3f
                            )
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = "Scanner",
                                tint = Color(0xFF80CBC4).copy(alpha = 0.7f),
                                modifier = Modifier.size(52.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Align Student QR or NFC Card",
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Anti-passback verifies single-meal usage",
                                color = Color(0xFF80CBC4),
                                fontSize = 10.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Instant Test Scan Action Buttons
                    Text(
                        text = "TAP TEST STUDENTS TO VERIFY ANTI-PASSBACK RULES:",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFD54F),
                        letterSpacing = 0.5.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Button(
                                onClick = { viewModel.processScan("MTU/14022/13") },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00796B)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("scan_dejene_button")
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Scan Dejene", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Text("MTU/14022/13 (IS)", fontSize = 9.sp, color = Color.White.copy(alpha = 0.8f))
                                }
                            }

                            Button(
                                onClick = { viewModel.processScan("MTU/14022/13") },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("rescan_passback_button")
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("⚡ Rescan Dejene", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Text("Test Anti-Passback", fontSize = 9.sp, color = Color.White.copy(alpha = 0.8f))
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            OutlinedButton(
                                onClick = { viewModel.processScan("MTU/13890/13") },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF80CBC4)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Scan Meron (Fasting)", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                    Text("Orthodox Fasting", fontSize = 9.sp)
                                }
                            }

                            OutlinedButton(
                                onClick = { viewModel.processScan("MTU/12099/12") },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF8A80)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Scan Dawit (Suspended)", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                    Text("Ineligible Card", fontSize = 9.sp)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Manual ID verification Toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showManualInput = !showManualInput }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Keyboard,
                            contentDescription = null,
                            tint = Color(0xFF80CBC4),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (showManualInput) "Hide Manual ID Keypad" else "Enter Student MTU ID Manually",
                            fontSize = 12.sp,
                            color = Color(0xFF80CBC4),
                            fontWeight = FontWeight.Medium
                        )
                    }

                    if (showManualInput) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = manualIdInput,
                                onValueChange = { viewModel.setManualIdInput(it) },
                                placeholder = { Text("e.g. MTU/14022/13", fontSize = 12.sp, color = Color.Gray) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = {
                                    if (manualIdInput.isNotBlank()) {
                                        viewModel.processScan(manualIdInput)
                                    }
                                }),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("manual_id_input")
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    if (manualIdInput.isNotBlank()) {
                                        viewModel.processScan(manualIdInput)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .height(52.dp)
                                    .testTag("verify_manual_id_button")
                            ) {
                                Text("VERIFY", color = Color(0xFF00382F), fontWeight = FontWeight.Black)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Shift Real-time Headcounts & Security Stats
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Current Shift Live Stats (${activeSession.sessionName})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Total Verified
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("SERVED", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                Text("$grantedCount", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color(0xFF004D40))
                                Text("Students", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        // Anti-Passback Blocked
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFFFEBEE),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("PASSBACK", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC62828))
                                Text("$passbackViolations", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color(0xFFC62828))
                                Text("Blocked", fontSize = 10.sp, color = Color(0xFFB71C1C))
                            }
                        }

                        // Ineligibles
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFFFF3E0),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("INELIGIBLE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE65100))
                                Text("$ineligibles", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color(0xFFE65100))
                                Text("Denied", fontSize = 10.sp, color = Color(0xFFBF360C))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
        }

        // Live Gatekeeper Verification Stream
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Entrance Audit Logs",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    text = "${scanLogs.size} Total Scans",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
        }

        items(scanLogs.take(15)) { log ->
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
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
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(
                                    if (log.status.isApproved) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                                )
                        ) {
                            Icon(
                                imageVector = if (log.status.isApproved) Icons.Default.CheckCircle else Icons.Default.Error,
                                contentDescription = null,
                                tint = if (log.status.isApproved) Color(0xFF2E7D32) else Color(0xFFC62828),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Text(
                                text = log.studentName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "${log.mtuId} • ${log.mealSession.sessionName}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (!log.status.isApproved && log.rejectionReason != null) {
                                Text(
                                    text = log.rejectionReason,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFFC62828)
                                )
                            }
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = log.formattedTime(),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                        if (log.isOfflineCached) {
                            Text(
                                text = "OFFLINE CACHED",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFE65100)
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}
