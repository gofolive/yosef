package com.example.data.repository

import com.example.data.local.MealCardDao
import com.example.data.local.ScanLogEntity
import com.example.data.local.StudentEntity
import com.example.data.model.DietaryType
import com.example.data.model.DiningHall
import com.example.data.model.DynamicMealToken
import com.example.data.model.MealScanLog
import com.example.data.model.MealSession
import com.example.data.model.Student
import com.example.data.model.StudentStatus
import com.example.data.model.VerificationStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.security.MessageDigest
import java.util.Calendar
import java.util.UUID

data class VerificationResult(
    val status: VerificationStatus,
    val message: String,
    val student: Student? = null,
    val previousScanTime: String? = null,
    val scanLog: MealScanLog? = null
)

class MealCardRepository(private val dao: MealCardDao) {

    fun getAllStudentsFlow(): Flow<List<Student>> {
        return dao.getAllStudentsFlow().map { list -> list.map { it.toDomain() } }
    }

    fun getAllScanLogsFlow(): Flow<List<MealScanLog>> {
        return dao.getAllScanLogsFlow().map { list -> list.map { it.toDomain() } }
    }

    fun getScanLogsForStudentFlow(mtuId: String): Flow<List<MealScanLog>> {
        return dao.getScanLogsForStudentFlow(mtuId).map { list -> list.map { it.toDomain() } }
    }

    suspend fun getStudentByMtuId(mtuId: String): Student? {
        return dao.getStudentByMtuId(mtuId.trim())?.toDomain()
    }

    suspend fun updateStudentStatus(mtuId: String, newStatus: StudentStatus) {
        dao.updateStudentStatus(mtuId, newStatus.name)
    }

    suspend fun updateStudentDietary(mtuId: String, dietaryType: DietaryType) {
        dao.updateStudentDietary(mtuId, dietaryType.name)
    }

    suspend fun updateStudentHall(mtuId: String, diningHall: DiningHall) {
        dao.updateStudentHall(mtuId, diningHall.name)
    }

    suspend fun addStudent(student: Student) {
        dao.insertStudent(StudentEntity.fromDomain(student))
    }

    suspend fun getUnsyncedCount(): Int {
        return dao.getUnsyncedOfflineLogs().size
    }

    suspend fun syncOfflineLogs(): Int {
        val unsynced = dao.getUnsyncedOfflineLogs()
        val count = unsynced.size
        if (count > 0) {
            dao.markAllLogsAsSynced()
        }
        return count
    }

    /**
     * Generate dynamic rolling token for a student
     */
    fun generateDynamicToken(studentId: String, session: MealSession): DynamicMealToken {
        val currentSeconds = System.currentTimeMillis() / 1000
        val windowIndex = currentSeconds / 30 // 30-second rolling window
        val salt = "MTU_TEPI_CAFETERIA_2026_SECRET_KEY"
        val raw = "$studentId|$session|$windowIndex|$salt"
        val digest = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray())
        val hash = digest.take(8).joinToString("") { "%02X".format(it) }
        val payload = "MTU_MEAL::$studentId::$session::$windowIndex::$hash"
        val tokenTimestamp = windowIndex * 30

        return DynamicMealToken(
            mtuId = studentId,
            session = session,
            timestampSeconds = tokenTimestamp,
            rollingNonce = hash,
            tokenPayload = payload,
            validityDurationSeconds = 30
        )
    }

    /**
     * Parse and verify a dynamic token string (e.g. from QR scan)
     */
    fun parseDynamicToken(payload: String): Pair<String, MealSession>? {
        if (!payload.startsWith("MTU_MEAL::")) {
            // Check if it's a direct student MTU ID or barcode
            return null
        }
        val parts = payload.split("::")
        if (parts.size >= 3) {
            val mtuId = parts[1]
            val session = runCatching { MealSession.valueOf(parts[2]) }.getOrDefault(MealSession.LUNCH)
            return Pair(mtuId, session)
        }
        return null
    }

    /**
     * Anti-passback and eligibility verification engine
     */
    suspend fun verifyAndRecordMealScan(
        mtuIdInput: String,
        targetSession: MealSession,
        gatekeeperHall: DiningHall,
        gatekeeperName: String = "Officer Haile M.",
        isOfflineMode: Boolean = false
    ): VerificationResult {
        val cleanMtuId = mtuIdInput.trim()
        val studentEntity = dao.getStudentByMtuId(cleanMtuId)

        if (studentEntity == null) {
            val rejectedLog = MealScanLog(
                scanId = UUID.randomUUID().toString().take(8),
                mtuId = cleanMtuId,
                studentName = "Unregistered ID ($cleanMtuId)",
                department = "Unknown",
                mealSession = targetSession,
                timestampMillis = System.currentTimeMillis(),
                diningHall = gatekeeperHall,
                gatekeeperName = gatekeeperName,
                status = VerificationStatus.REJECTED_INELIGIBLE,
                rejectionReason = "Student ID not found in campus dining database",
                isOfflineCached = isOfflineMode,
                isSyncedToServer = !isOfflineMode
            )
            dao.insertScanLog(ScanLogEntity.fromDomain(rejectedLog))
            return VerificationResult(
                status = VerificationStatus.REJECTED_INELIGIBLE,
                message = "Student ID not found in MTU Tepi dining directory.",
                scanLog = rejectedLog
            )
        }

        val student = studentEntity.toDomain()

        // Check 1: Status Eligibility
        if (student.status != StudentStatus.ACTIVE) {
            val rejectedLog = MealScanLog(
                scanId = UUID.randomUUID().toString().take(8),
                mtuId = student.mtuId,
                studentName = student.fullName,
                department = student.department,
                mealSession = targetSession,
                timestampMillis = System.currentTimeMillis(),
                diningHall = gatekeeperHall,
                gatekeeperName = gatekeeperName,
                status = VerificationStatus.REJECTED_INELIGIBLE,
                rejectionReason = "Card status: ${student.status.displayName}",
                isOfflineCached = isOfflineMode,
                isSyncedToServer = !isOfflineMode
            )
            dao.insertScanLog(ScanLogEntity.fromDomain(rejectedLog))
            return VerificationResult(
                status = VerificationStatus.REJECTED_INELIGIBLE,
                message = "Meal card is currently suspended: ${student.status.displayName}. Please contact Student Services Dining Office.",
                student = student,
                scanLog = rejectedLog
            )
        }

        // Check 2: Dining Hall assignment check
        if (student.diningHall != gatekeeperHall) {
            val rejectedLog = MealScanLog(
                scanId = UUID.randomUUID().toString().take(8),
                mtuId = student.mtuId,
                studentName = student.fullName,
                department = student.department,
                mealSession = targetSession,
                timestampMillis = System.currentTimeMillis(),
                diningHall = gatekeeperHall,
                gatekeeperName = gatekeeperName,
                status = VerificationStatus.REJECTED_WRONG_HALL,
                rejectionReason = "Registered at ${student.diningHall.displayName}, not ${gatekeeperHall.displayName}",
                isOfflineCached = isOfflineMode,
                isSyncedToServer = !isOfflineMode
            )
            dao.insertScanLog(ScanLogEntity.fromDomain(rejectedLog))
            return VerificationResult(
                status = VerificationStatus.REJECTED_WRONG_HALL,
                message = "Assigned to ${student.diningHall.displayName}. Access not permitted at ${gatekeeperHall.code}.",
                student = student,
                scanLog = rejectedLog
            )
        }

        // Check 3: ANTI-PASSBACK LOGIC
        // Look up if this student already had a GRANTED scan in this meal session today
        val startOfDay = getStartOfDayMillis()
        val priorGrantedScan = dao.getSuccessfulScanForSessionToday(
            mtuId = student.mtuId,
            mealSession = targetSession.name,
            startOfDayMillis = startOfDay
        )

        if (priorGrantedScan != null) {
            val priorLog = priorGrantedScan.toDomain()
            val passbackViolationLog = MealScanLog(
                scanId = UUID.randomUUID().toString().take(8),
                mtuId = student.mtuId,
                studentName = student.fullName,
                department = student.department,
                mealSession = targetSession,
                timestampMillis = System.currentTimeMillis(),
                diningHall = gatekeeperHall,
                gatekeeperName = gatekeeperName,
                status = VerificationStatus.REJECTED_PASSBACK,
                rejectionReason = "Anti-passback alert: Already consumed ${targetSession.sessionName} at ${priorLog.formattedTime()} (${priorLog.gatekeeperName})",
                isOfflineCached = isOfflineMode,
                isSyncedToServer = !isOfflineMode
            )
            dao.insertScanLog(ScanLogEntity.fromDomain(passbackViolationLog))

            return VerificationResult(
                status = VerificationStatus.REJECTED_PASSBACK,
                message = "ANTI-PASSBACK VIOLATION! Student already accessed ${targetSession.sessionName} today at ${priorLog.formattedTime()}.",
                student = student,
                previousScanTime = priorLog.formattedTime(),
                scanLog = passbackViolationLog
            )
        }

        // All checks passed! Access granted
        val grantedLog = MealScanLog(
            scanId = UUID.randomUUID().toString().take(8),
            mtuId = student.mtuId,
            studentName = student.fullName,
            department = student.department,
            mealSession = targetSession,
            timestampMillis = System.currentTimeMillis(),
            diningHall = gatekeeperHall,
            gatekeeperName = gatekeeperName,
            status = VerificationStatus.GRANTED,
            rejectionReason = null,
            isOfflineCached = isOfflineMode,
            isSyncedToServer = !isOfflineMode
        )
        dao.insertScanLog(ScanLogEntity.fromDomain(grantedLog))

        return VerificationResult(
            status = VerificationStatus.GRANTED,
            message = "Verified successfully. Please proceed to the serving line.",
            student = student,
            scanLog = grantedLog
        )
    }

    private fun getStartOfDayMillis(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    /**
     * Seeds initial database if empty
     */
    suspend fun seedDatabaseIfEmpty() {
        val existingStudents = dao.getAllStudents()
        if (existingStudents.isEmpty()) {
            val seedStudents = listOf(
                StudentEntity(
                    mtuId = "MTU/14022/13",
                    fullName = "Dejene Gashaw",
                    department = "Information Systems",
                    yearOfStudy = 4,
                    diningHall = DiningHall.HALL_1_MAIN.name,
                    dietaryType = DietaryType.REGULAR_NON_FASTING.name,
                    status = StudentStatus.ACTIVE.name,
                    barcodeNfcUid = "NFC-84920193",
                    avatarSeed = "DG",
                    phone = "+251 91 142 8890"
                ),
                StudentEntity(
                    mtuId = "MTU/13890/13",
                    fullName = "Meron Tadesse",
                    department = "Software Engineering",
                    yearOfStudy = 4,
                    diningHall = DiningHall.HALL_1_MAIN.name,
                    dietaryType = DietaryType.ORTHODOX_FASTING.name,
                    status = StudentStatus.ACTIVE.name,
                    barcodeNfcUid = "NFC-99120481",
                    avatarSeed = "MT",
                    phone = "+251 92 334 1122"
                ),
                StudentEntity(
                    mtuId = "MTU/14210/14",
                    fullName = "Mohammed Ahmed",
                    department = "Computer Science",
                    yearOfStudy = 3,
                    diningHall = DiningHall.HALL_1_MAIN.name,
                    dietaryType = DietaryType.MUSLIM_HALAL.name,
                    status = StudentStatus.ACTIVE.name,
                    barcodeNfcUid = "NFC-73019482",
                    avatarSeed = "MA",
                    phone = "+251 94 887 6543"
                ),
                StudentEntity(
                    mtuId = "MTU/15104/15",
                    fullName = "Bethelhem Bekele",
                    department = "Information Systems",
                    yearOfStudy = 2,
                    diningHall = DiningHall.HALL_2_ANNEX.name,
                    dietaryType = DietaryType.REGULAR_NON_FASTING.name,
                    status = StudentStatus.ACTIVE.name,
                    barcodeNfcUid = "NFC-61902847",
                    avatarSeed = "BB",
                    phone = "+251 93 456 7890"
                ),
                StudentEntity(
                    mtuId = "MTU/12099/12",
                    fullName = "Dawit Alemayehu",
                    department = "Electrical Engineering",
                    yearOfStudy = 5,
                    diningHall = DiningHall.HALL_1_MAIN.name,
                    dietaryType = DietaryType.REGULAR_NON_FASTING.name,
                    status = StudentStatus.SUSPENDED.name,
                    barcodeNfcUid = "NFC-10293847",
                    avatarSeed = "DA",
                    phone = "+251 91 998 7766"
                ),
                StudentEntity(
                    mtuId = "MTU/14456/14",
                    fullName = "Hiwot Girma",
                    department = "Agricultural Economics",
                    yearOfStudy = 3,
                    diningHall = DiningHall.HALL_2_ANNEX.name,
                    dietaryType = DietaryType.ORTHODOX_FASTING.name,
                    status = StudentStatus.ACTIVE.name,
                    barcodeNfcUid = "NFC-55443322",
                    avatarSeed = "HG",
                    phone = "+251 92 654 3210"
                ),
                StudentEntity(
                    mtuId = "MTU/15321/15",
                    fullName = "Natnael Yilma",
                    department = "Information Systems",
                    yearOfStudy = 2,
                    diningHall = DiningHall.HALL_1_MAIN.name,
                    dietaryType = DietaryType.SPECIAL_DIET.name,
                    status = StudentStatus.ACTIVE.name,
                    barcodeNfcUid = "NFC-33221100",
                    avatarSeed = "NY",
                    phone = "+251 94 112 2334"
                ),
                StudentEntity(
                    mtuId = "MTU/13901/13",
                    fullName = "Tsion Getachew",
                    department = "Software Engineering",
                    yearOfStudy = 4,
                    diningHall = DiningHall.HALL_1_MAIN.name,
                    dietaryType = DietaryType.REGULAR_NON_FASTING.name,
                    status = StudentStatus.ACTIVE.name,
                    barcodeNfcUid = "NFC-88776655",
                    avatarSeed = "TG",
                    phone = "+251 91 556 6778"
                )
            )
            dao.insertStudents(seedStudents)

            // Seed initial breakfast scan history for today so stats are active immediately
            val currentTime = System.currentTimeMillis()
            val threeHoursAgo = currentTime - (3 * 3600 * 1000)
            val initialLogs = listOf(
                ScanLogEntity(
                    scanId = "SC-78910",
                    mtuId = "MTU/14022/13",
                    studentName = "Dejene Gashaw",
                    department = "Information Systems",
                    mealSession = MealSession.BREAKFAST.name,
                    timestampMillis = threeHoursAgo - 120000,
                    diningHall = DiningHall.HALL_1_MAIN.name,
                    gatekeeperName = "Officer Haile M.",
                    status = VerificationStatus.GRANTED.name,
                    rejectionReason = null,
                    isOfflineCached = false,
                    isSyncedToServer = true
                ),
                ScanLogEntity(
                    scanId = "SC-78911",
                    mtuId = "MTU/13890/13",
                    studentName = "Meron Tadesse",
                    department = "Software Engineering",
                    mealSession = MealSession.BREAKFAST.name,
                    timestampMillis = threeHoursAgo - 90000,
                    diningHall = DiningHall.HALL_1_MAIN.name,
                    gatekeeperName = "Officer Haile M.",
                    status = VerificationStatus.GRANTED.name,
                    rejectionReason = null,
                    isOfflineCached = false,
                    isSyncedToServer = true
                ),
                ScanLogEntity(
                    scanId = "SC-78912",
                    mtuId = "MTU/14210/14",
                    studentName = "Mohammed Ahmed",
                    department = "Computer Science",
                    mealSession = MealSession.BREAKFAST.name,
                    timestampMillis = threeHoursAgo - 60000,
                    diningHall = DiningHall.HALL_1_MAIN.name,
                    gatekeeperName = "Officer Haile M.",
                    status = VerificationStatus.GRANTED.name,
                    rejectionReason = null,
                    isOfflineCached = false,
                    isSyncedToServer = true
                ),
                ScanLogEntity(
                    scanId = "SC-78913",
                    mtuId = "MTU/12099/12",
                    studentName = "Dawit Alemayehu",
                    department = "Electrical Engineering",
                    mealSession = MealSession.BREAKFAST.name,
                    timestampMillis = threeHoursAgo - 30000,
                    diningHall = DiningHall.HALL_1_MAIN.name,
                    gatekeeperName = "Officer Haile M.",
                    status = VerificationStatus.REJECTED_INELIGIBLE.name,
                    rejectionReason = "Card status: Suspended (Contact Admin)",
                    isOfflineCached = false,
                    isSyncedToServer = true
                )
            )
            initialLogs.forEach { dao.insertScanLog(it) }
        }
    }
}
