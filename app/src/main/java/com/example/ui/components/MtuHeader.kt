package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Badge
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MealSession

enum class AppRole(val title: String, val icon: String) {
    STUDENT("Student Card", "🎓"),
    GATEKEEPER("Scanner Gate", "🛡️"),
    ADMIN("Admin Dashboard", "📊")
}

@Composable
fun MtuHeader(
    currentRole: AppRole,
    onRoleSelected: (AppRole) -> Unit,
    activeSession: MealSession,
    isOfflineMode: Boolean,
    onToggleOfflineMode: () -> Unit,
    unsyncedCount: Int,
    onTriggerSync: () -> Unit
) {
    Surface(
        color = Color(0xFF004D40),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            // Top Row: MTU Branding and Network Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF00796B))
                    ) {
                        Icon(
                            imageVector = Icons.Default.School,
                            contentDescription = "MTU Logo",
                            tint = Color(0xFFFFD54F),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = "MIZAN-TEPI UNIVERSITY",
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp,
                            color = Color(0xFFFFD54F),
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "Tepi Campus • Student Dining Services",
                            fontWeight = FontWeight.Medium,
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }

                // Network Offline/Online Simulation Toggle Button
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isOfflineMode) Color(0xFFD84315) else Color(0xFF00796B),
                    modifier = Modifier
                        .clickable { onToggleOfflineMode() }
                        .testTag("network_mode_toggle")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isOfflineMode) Icons.Default.CloudOff else Icons.Default.CloudDone,
                            contentDescription = if (isOfflineMode) "Offline" else "Online",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = if (isOfflineMode) "OFFLINE" else "ONLINE",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Sub-row: Active Meal Session Pill & Offline Sync notice if any
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Active session indicator
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White.copy(alpha = 0.12f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF00E676))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "ACTIVE MEAL: ${activeSession.sessionName.uppercase()} (${activeSession.timeWindowString()})",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // If offline and have unsynced records
                if (unsyncedCount > 0) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFFFB300),
                        modifier = Modifier.clickable { onTriggerSync() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = "Sync",
                                tint = Color(0xFF263238),
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "$unsyncedCount Pending Sync",
                                color = Color(0xFF263238),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Role selection Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AppRole.values().forEach { role ->
                    val isSelected = currentRole == role
                    FilterChip(
                        selected = isSelected,
                        onClick = { onRoleSelected(role) },
                        label = {
                            Text(
                                text = "${role.icon} ${role.title}",
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = Color.White.copy(alpha = 0.15f),
                            labelColor = Color.White.copy(alpha = 0.85f),
                            selectedContainerColor = Color(0xFFFFD54F),
                            selectedLabelColor = Color(0xFF004D40)
                        ),
                        border = null,
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp)
                            .testTag("role_chip_${role.name.lowercase()}")
                    )
                }
            }
        }
    }
}
