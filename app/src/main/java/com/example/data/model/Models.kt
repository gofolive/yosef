package com.example.data.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class DietaryType(val displayName: String, val badgeColor: Long) {
    REGULAR_NON_FASTING("Regular (Non-Fasting)", 0xFF00796B),
    ORTHODOX_FASTING("Orthodox Fasting", 0xFF6A1B9A),
    MUSLIM_HALAL("Muslim (Halal)", 0xFF2E7D32),
    SPECIAL_DIET("Special Dietary", 0xFFD84315)
}

enum class DiningHall(val code: String, val displayName: String, val capacity: Int) {
    HALL_1_MAIN("DH-01", "Main Student Dining Hall 1", 1800),
    HALL_2_ANNEX("DH-02", "Technology Campus Dining Hall 2", 1200)
}

enum class StudentStatus(val displayName: String) {
    ACTIVE("Active / Eligible"),
    SUSPENDED("Suspended (Contact Admin)"),
    ON_LEAVE("Academic Leave"),
    GRADUATED("Alumni / Graduated")
}

enum class MealSession(
    val sessionName: String,
    val defaultStartHour: Int,
    val defaultStartMin: Int,
    val defaultEndHour: Int,
    val defaultEndMin: Int,
    val portionTarget: Int
) {
    BREAKFAST("Breakfast", 6, 30, 9, 0, 2200),
    LUNCH("Lunch", 11, 45, 14, 30, 2600),
    DINNER("Dinner", 18, 0, 21, 0, 2400);

    fun timeWindowString(): String {
        return String.format(
            Locale.US,
            "%02d:%02d - %02d:%02d",
            defaultStartHour,
            defaultStartMin,
            defaultEndHour,
            defaultEndMin
        )
    }
}

enum class VerificationStatus(val displayName: String, val isApproved: Boolean) {
    GRANTED("ACCESS GRANTED", true),
    REJECTED_PASSBACK("ANTI-PASSBACK VIOLATION", false),
    REJECTED_INELIGIBLE("CARD INELIGIBLE / SUSPENDED", false),
    REJECTED_WRONG_HALL("INCORRECT DINING HALL", false),
    REJECTED_TOKEN_EXPIRED("DYNAMIC TOKEN EXPIRED", false),
    REJECTED_OUTSIDE_WINDOW("CAFETERIA CLOSED FOR THIS MEAL", false)
}

data class Student(
    val id: Long = 0,
    val mtuId: String,
    val fullName: String,
    val department: String,
    val yearOfStudy: Int,
    val diningHall: DiningHall,
    val dietaryType: DietaryType,
    val status: StudentStatus,
    val barcodeNfcUid: String,
    val avatarSeed: String,
    val phone: String = "+251 91 234 5678"
)

data class DynamicMealToken(
    val mtuId: String,
    val session: MealSession,
    val timestampSeconds: Long,
    val rollingNonce: String,
    val tokenPayload: String,
    val validityDurationSeconds: Int = 30
) {
    fun isExpired(currentTimeSeconds: Long = System.currentTimeMillis() / 1000): Boolean {
        return (currentTimeSeconds - timestampSeconds) > validityDurationSeconds
    }

    fun remainingSeconds(currentTimeSeconds: Long = System.currentTimeMillis() / 1000): Int {
        val rem = validityDurationSeconds - (currentTimeSeconds - timestampSeconds)
        return if (rem > 0) rem.toInt() else 0
    }
}

data class MealScanLog(
    val id: Long = 0,
    val scanId: String,
    val mtuId: String,
    val studentName: String,
    val department: String,
    val mealSession: MealSession,
    val timestampMillis: Long,
    val diningHall: DiningHall,
    val gatekeeperName: String,
    val status: VerificationStatus,
    val rejectionReason: String? = null,
    val isOfflineCached: Boolean = false,
    val isSyncedToServer: Boolean = true
) {
    fun formattedTime(): String {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.US)
        return sdf.format(Date(timestampMillis))
    }

    fun formattedDate(): String {
        val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.US)
        return sdf.format(Date(timestampMillis))
    }
}
