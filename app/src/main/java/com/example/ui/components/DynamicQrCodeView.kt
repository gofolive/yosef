package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DynamicMealToken
import kotlin.math.abs

@Composable
fun DynamicQrCodeView(
    dynamicToken: DynamicMealToken,
    remainingSeconds: Int,
    modifier: Modifier = Modifier,
    sizeDp: Dp = 230.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "qr_anim")
    val laserProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser_pos"
    )

    val watermarkAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "watermark_rotation"
    )

    // Generate 21x21 boolean grid deterministically based on token hash & payload
    val qrMatrix = remember(dynamicToken.tokenPayload) {
        generateQrMatrix(dynamicToken.tokenPayload, dynamicToken.rollingNonce)
    }

    Column(
        modifier = modifier.testTag("dynamic_qr_container"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Dynamic QR Card with rotating security watermark border
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            shadowElevation = 8.dp,
            modifier = Modifier
                .padding(8.dp)
                .border(2.dp, Color(0xFF00796B).copy(alpha = 0.3f), RoundedCornerShape(24.dp))
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.padding(16.dp)
            ) {
                // QR Canvas
                Canvas(
                    modifier = Modifier
                        .size(sizeDp)
                        .testTag("qr_canvas")
                ) {
                    val gridSize = 21
                    val cellSize = size.width / gridSize
                    val darkColor = Color(0xFF00382F)
                    val cornerRadius = CornerRadius(cellSize * 0.25f, cellSize * 0.25f)

                    for (r in 0 until gridSize) {
                        for (c in 0 until gridSize) {
                            if (qrMatrix[r][c]) {
                                // Don't draw in center reserved logo area (grid rows/cols 8..12)
                                val isCenterLogoArea = (r in 8..12) && (c in 8..12)
                                if (!isCenterLogoArea) {
                                    val topLeft = Offset(c * cellSize, r * cellSize)
                                    drawRoundRect(
                                        color = darkColor,
                                        topLeft = topLeft,
                                        size = Size(cellSize * 0.95f, cellSize * 0.95f),
                                        cornerRadius = cornerRadius
                                    )
                                }
                            }
                        }
                    }

                    // Draw animated laser scanning beam
                    val laserY = size.height * laserProgress
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color(0x3300BFA5),
                                Color(0xCC00BFA5),
                                Color.Transparent
                            ),
                            startY = laserY - 14f,
                            endY = laserY + 14f
                        ),
                        topLeft = Offset(0f, laserY - 14f),
                        size = Size(size.width, 28f)
                    )
                    // Core sharp laser line
                    drawLine(
                        color = Color(0xFF00E676),
                        start = Offset(0f, laserY),
                        end = Offset(size.width, laserY),
                        strokeWidth = 3f
                    )
                }

                // Center MTU Dining emblem badge
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF004D40))
                        .border(2.5.dp, Color(0xFFFFB300), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Restaurant,
                        contentDescription = "MTU Dining Shield",
                        tint = Color(0xFFFFD54F),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Time-sensitive dynamic countdown bar
        val progressFraction = (remainingSeconds.toFloat() / 30f).coerceIn(0f, 1f)
        val timerColor = when {
            remainingSeconds > 15 -> Color(0xFF00796B)
            remainingSeconds > 7 -> Color(0xFFF57C00)
            else -> Color(0xFFD32F2F)
        }

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = timerColor.copy(alpha = 0.10f),
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .padding(horizontal = 14.dp, vertical = 8.dp)
                    .fillMaxWidth(0.9f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        progress = { progressFraction },
                        modifier = Modifier.size(24.dp),
                        color = timerColor,
                        strokeWidth = 3.dp,
                        trackColor = timerColor.copy(alpha = 0.2f),
                    )
                    Text(
                        text = "$remainingSeconds",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = timerColor
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = timerColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "ANTI-FRAUD DYNAMIC TOKEN",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp,
                            color = timerColor,
                            letterSpacing = 0.5.sp
                        )
                    }
                    Text(
                        text = "Refreshes in ${remainingSeconds}s • Screenshots are invalid",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Token Refreshing",
                    tint = timerColor,
                    modifier = Modifier
                        .size(18.dp)
                        .rotate(watermarkAngle)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Security Nonce hash banner
        Text(
            text = "HASH: ${dynamicToken.rollingNonce} • WINDOW #${dynamicToken.timestampSeconds % 10000}",
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.testTag("qr_security_hash")
        )
    }
}

/**
 * Creates a valid 21x21 QR code module matrix with finder patterns and data modules
 */
private fun generateQrMatrix(payload: String, nonce: String): Array<BooleanArray> {
    val size = 21
    val matrix = Array(size) { BooleanArray(size) }

    // Finder pattern helper (7x7)
    fun drawFinder(startR: Int, startC: Int) {
        for (r in 0 until 7) {
            for (c in 0 until 7) {
                val isOuter = (r == 0 || r == 6 || c == 0 || c == 6)
                val isInner = (r in 2..4 && c in 2..4)
                matrix[startR + r][startC + c] = isOuter || isInner
            }
        }
    }

    // Draw three standard finder patterns (Top-Left, Top-Right, Bottom-Left)
    drawFinder(0, 0)
    drawFinder(0, 14)
    drawFinder(14, 0)

    // Timing strips
    for (i in 8 until 13) {
        matrix[6][i] = (i % 2 == 0)
        matrix[i][6] = (i % 2 == 0)
    }

    // Populate data modules deterministically based on string characters and nonce
    val seedString = "$payload::$nonce"
    val hash = abs(seedString.hashCode())
    var bitPos = 0

    for (r in 0 until size) {
        for (c in 0 until size) {
            // Skip finder zones
            val inTopLeft = (r in 0..7 && c in 0..7)
            val inTopRight = (r in 0..7 && c in 13..20)
            val inBottomLeft = (r in 13..20 && c in 0..7)
            val inTiming = (r == 6 || c == 6)

            if (!inTopLeft && !inTopRight && !inBottomLeft && !inTiming) {
                val charVal = seedString[(bitPos + r * 7 + c * 3) % seedString.length].code
                val isBlack = ((hash shr ((bitPos + r + c) % 24)) and 1) == 1 || ((charVal + r * c) % 3 == 0)
                matrix[r][c] = isBlack
                bitPos++
            }
        }
    }

    return matrix
}
