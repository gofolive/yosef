package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.VerificationStatus
import com.example.data.repository.VerificationResult

@Composable
fun AntiPassbackAlertModal(
    result: VerificationResult?,
    onDismiss: () -> Unit
) {
    if (result == null) return

    val isApproved = result.status.isApproved
    val statusColor = when (result.status) {
        VerificationStatus.GRANTED -> Color(0xFF2E7D32)
        VerificationStatus.REJECTED_PASSBACK -> Color(0xFFC62828)
        VerificationStatus.REJECTED_INELIGIBLE -> Color(0xFFD84315)
        VerificationStatus.REJECTED_WRONG_HALL -> Color(0xFFEF6C00)
        else -> Color(0xFFC62828)
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .testTag("verification_result_modal")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Large Status Icon Banner
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(statusColor.copy(alpha = 0.14f))
                        .border(3.dp, statusColor, CircleShape)
                ) {
                    Icon(
                        imageVector = when (result.status) {
                            VerificationStatus.GRANTED -> Icons.Default.CheckCircle
                            VerificationStatus.REJECTED_PASSBACK -> Icons.Default.Block
                            VerificationStatus.REJECTED_WRONG_HALL -> Icons.Default.MeetingRoom
                            else -> Icons.Default.Warning
                        },
                        contentDescription = result.status.displayName,
                        tint = statusColor,
                        modifier = Modifier.size(52.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Title
                Text(
                    text = result.status.displayName,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = statusColor,
                    textAlign = TextAlign.Center,
                    letterSpacing = 0.5.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = result.message,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Spacer(modifier = Modifier.height(18.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(16.dp))

                // Student Identity Card
                if (result.student != null) {
                    val student = result.student
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Avatar circle
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF004D40))
                                ) {
                                    Text(
                                        text = student.avatarSeed,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 17.sp
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = student.fullName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${student.department} • Year ${student.yearOfStudy}",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "ID: ${student.mtuId}",
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp,
                                        color = Color(0xFF00796B)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                            Spacer(modifier = Modifier.height(8.dp))

                            // Details badges
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = "Dietary Profile",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = student.dietaryType.displayName,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(student.dietaryType.badgeColor)
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "Assigned Hall",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = student.diningHall.code,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }

                // If Anti-Passback Violation: Highlight Previous Scan Alert
                if (result.status == VerificationStatus.REJECTED_PASSBACK && result.previousScanTime != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFFFEBEE),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF9A9A)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = null,
                                tint = Color(0xFFC62828),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Prior Scan Recorded at ${result.previousScanTime}. Only one meal is permitted per window.",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFFB71C1C)
                            )
                        }
                    }
                }

                // If scanLog is present and offline
                if (result.scanLog?.isOfflineCached == true) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "⚡ Scanned in Local Offline Cache (Will sync when online)",
                        fontSize = 10.sp,
                        color = Color(0xFFE65100),
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action Dismiss Button
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("dismiss_verification_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = statusColor
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        text = if (isApproved) "Proceed to Serving Line" else "Acknowledge & Close",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}
