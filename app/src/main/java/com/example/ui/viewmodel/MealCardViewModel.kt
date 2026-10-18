package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.MtuMealDatabase
import com.example.data.model.DietaryType
import com.example.data.model.DiningHall
import com.example.data.model.DynamicMealToken
import com.example.data.model.MealScanLog
import com.example.data.model.MealSession
import com.example.data.model.Student
import com.example.data.model.StudentStatus
import com.example.data.model.VerificationStatus
import com.example.data.repository.MealCardRepository
import com.example.data.repository.VerificationResult
import com.example.ui.components.AppRole
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MealCardViewModel(application: Application) : AndroidViewModel(application) {

    private val database = MtuMealDatabase.getInstance(application)
    private val repository = MealCardRepository(database.mealCardDao())

    // App Navigation Role
    private val _currentRole = MutableStateFlow(AppRole.STUDENT)
    val currentRole: StateFlow<AppRole> = _currentRole.asStateFlow()

    // Offline Campus Downtime Simulation
    private val _isOfflineMode = MutableStateFlow(false)
    val isOfflineMode: StateFlow<Boolean> = _isOfflineMode.asStateFlow()

    private val _unsyncedCount = MutableStateFlow(0)
    val unsyncedCount: StateFlow<Int> = _unsyncedCount.asStateFlow()

    // Active Meal Session (defaults to LUNCH)
    private val _activeSession = MutableStateFlow(MealSession.LUNCH)
    val activeSession: StateFlow<MealSession> = _activeSession.asStateFlow()

    // Gatekeeper Hall Station
    private val _gatekeeperHall = MutableStateFlow(DiningHall.HALL_1_MAIN)
    val gatekeeperHall: StateFlow<DiningHall> = _gatekeeperHall.asStateFlow()

    // Students list from DB
    val studentsList: StateFlow<List<Student>> = repository.getAllStudentsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active Selected Student for Student Meal Card View
    private val _selectedStudent = MutableStateFlow<Student?>(null)
    val selectedStudent: StateFlow<Student?> = _selectedStudent.asStateFlow()

    // Dynamic QR Token & Countdown
    private val _dynamicToken = MutableStateFlow<DynamicMealToken?>(null)
    val dynamicToken: StateFlow<DynamicMealToken?> = _dynamicToken.asStateFlow()

    private val _tokenRemainingSeconds = MutableStateFlow(30)
    val tokenRemainingSeconds: StateFlow<Int> = _tokenRemainingSeconds.asStateFlow()

    // Verification Scanner State
    private val _verificationResult = MutableStateFlow<VerificationResult?>(null)
    val verificationResult: StateFlow<VerificationResult?> = _verificationResult.asStateFlow()

    private val _manualIdInput = MutableStateFlow("")
    val manualIdInput: StateFlow<String> = _manualIdInput.asStateFlow()

    // Scan Logs Flow
    val scanLogsList: StateFlow<List<MealScanLog>> = repository.getAllScanLogsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Admin Filters
    private val _studentSearchQuery = MutableStateFlow("")
    val studentSearchQuery: StateFlow<String> = _studentSearchQuery.asStateFlow()

    private val _selectedDietaryFilter = MutableStateFlow<DietaryType?>(null)
    val selectedDietaryFilter: StateFlow<DietaryType?> = _selectedDietaryFilter.asStateFlow()

    private val _selectedHallFilter = MutableStateFlow<DiningHall?>(null)
    val selectedHallFilter: StateFlow<DiningHall?> = _selectedHallFilter.asStateFlow()

    private val _selectedStatusFilter = MutableStateFlow<StudentStatus?>(null)
    val selectedStatusFilter: StateFlow<StudentStatus?> = _selectedStatusFilter.asStateFlow()

    // Filtered Students for Admin Directory
    val filteredStudents: StateFlow<List<Student>> = combine(
        studentsList,
        _studentSearchQuery,
        _selectedDietaryFilter,
        _selectedHallFilter,
        _selectedStatusFilter
    ) { students, query, dietary, hall, status ->
        students.filter { student ->
            val matchesQuery = query.isBlank() ||
                student.fullName.contains(query, ignoreCase = true) ||
                student.mtuId.contains(query, ignoreCase = true) ||
                student.department.contains(query, ignoreCase = true)

            val matchesDietary = dietary == null || student.dietaryType == dietary
            val matchesHall = hall == null || student.diningHall == hall
            val matchesStatus = status == null || student.status == status

            matchesQuery && matchesDietary && matchesHall && matchesStatus
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            repository.seedDatabaseIfEmpty()
            updateUnsyncedCount()

            // Initialize default student to Dejene Gashaw
            val defaultStudent = repository.getStudentByMtuId("MTU/14022/13")
            _selectedStudent.value = defaultStudent
            generateCurrentToken()
        }

        // Start dynamic token timer loop
        startTokenTimerLoop()
    }

    private fun startTokenTimerLoop() {
        viewModelScope.launch {
            while (true) {
                delay(1000)
                val current = _tokenRemainingSeconds.value
                if (current <= 1) {
                    generateCurrentToken()
                    _tokenRemainingSeconds.value = 30
                } else {
                    _tokenRemainingSeconds.value = current - 1
                }
            }
        }
    }

    private fun generateCurrentToken() {
        val student = _selectedStudent.value ?: return
        val token = repository.generateDynamicToken(student.mtuId, _activeSession.value)
        _dynamicToken.value = token
    }

    fun selectRole(role: AppRole) {
        _currentRole.value = role
    }

    fun selectStudent(student: Student) {
        _selectedStudent.value = student
        _tokenRemainingSeconds.value = 30
        generateCurrentToken()
    }

    fun setActiveSession(session: MealSession) {
        _activeSession.value = session
        generateCurrentToken()
    }

    fun setGatekeeperHall(hall: DiningHall) {
        _gatekeeperHall.value = hall
    }

    fun toggleOfflineMode() {
        _isOfflineMode.value = !_isOfflineMode.value
        updateUnsyncedCount()
    }

    fun setManualIdInput(input: String) {
        _manualIdInput.value = input
    }

    fun dismissVerificationModal() {
        _verificationResult.value = null
    }

    fun updateStudentSearchQuery(query: String) {
        _studentSearchQuery.value = query
    }

    fun setDietaryFilter(dietary: DietaryType?) {
        _selectedDietaryFilter.value = dietary
    }

    fun setHallFilter(hall: DiningHall?) {
        _selectedHallFilter.value = hall
    }

    fun setStatusFilter(status: StudentStatus?) {
        _selectedStatusFilter.value = status
    }

    /**
     * Executes verification when scanning a QR code, tapping a test student, or entering ID
     */
    fun processScan(rawMtuIdOrPayload: String) {
        viewModelScope.launch {
            val parsedMtuId = if (rawMtuIdOrPayload.startsWith("MTU_MEAL::")) {
                repository.parseDynamicToken(rawMtuIdOrPayload)?.first ?: rawMtuIdOrPayload
            } else {
                rawMtuIdOrPayload
            }

            val result = repository.verifyAndRecordMealScan(
                mtuIdInput = parsedMtuId,
                targetSession = _activeSession.value,
                gatekeeperHall = _gatekeeperHall.value,
                gatekeeperName = "Officer Haile M.",
                isOfflineMode = _isOfflineMode.value
            )

            _verificationResult.value = result
            triggerHapticFeedback(result.status.isApproved)
            updateUnsyncedCount()
        }
    }

    /**
     * Synchronize cached scans to central database
     */
    fun syncOfflineRecords() {
        viewModelScope.launch {
            repository.syncOfflineLogs()
            updateUnsyncedCount()
        }
    }

    private fun updateUnsyncedCount() {
        viewModelScope.launch {
            _unsyncedCount.value = repository.getUnsyncedCount()
        }
    }

    fun toggleStudentStatus(student: Student) {
        viewModelScope.launch {
            val newStatus = if (student.status == StudentStatus.ACTIVE) {
                StudentStatus.SUSPENDED
            } else {
                StudentStatus.ACTIVE
            }
            repository.updateStudentStatus(student.mtuId, newStatus)
            if (_selectedStudent.value?.mtuId == student.mtuId) {
                _selectedStudent.value = student.copy(status = newStatus)
            }
        }
    }

    fun updateStudentDietary(student: Student, dietary: DietaryType) {
        viewModelScope.launch {
            repository.updateStudentDietary(student.mtuId, dietary)
            if (_selectedStudent.value?.mtuId == student.mtuId) {
                _selectedStudent.value = student.copy(dietaryType = dietary)
            }
        }
    }

    fun updateStudentHall(student: Student, hall: DiningHall) {
        viewModelScope.launch {
            repository.updateStudentHall(student.mtuId, hall)
            if (_selectedStudent.value?.mtuId == student.mtuId) {
                _selectedStudent.value = student.copy(diningHall = hall)
            }
        }
    }

    fun registerNewStudent(
        mtuId: String,
        fullName: String,
        department: String,
        year: Int,
        hall: DiningHall,
        dietary: DietaryType
    ) {
        viewModelScope.launch {
            val initials = fullName.split(" ").filter { it.isNotBlank() }
                .take(2).map { it.first().uppercaseChar() }.joinToString("")
            val newStudent = Student(
                mtuId = mtuId.trim(),
                fullName = fullName.trim(),
                department = department.trim(),
                yearOfStudy = year,
                diningHall = hall,
                dietaryType = dietary,
                status = StudentStatus.ACTIVE,
                barcodeNfcUid = "NFC-" + (10000000..99999999).random(),
                avatarSeed = if (initials.isNotEmpty()) initials else "ST"
            )
            repository.addStudent(newStudent)
        }
    }

    private fun triggerHapticFeedback(isApproved: Boolean) {
        try {
            val context = getApplication<Application>()
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            if (vibrator != null && vibrator.hasVibrator()) {
                if (isApproved) {
                    // Quick double-pulse for approved
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator.vibrate(
                            VibrationEffect.createWaveform(longArrayOf(0, 70, 60, 70), -1)
                        )
                    } else {
                        @Suppress("DEPRECATION")
                        vibrator.vibrate(100)
                    }
                } else {
                    // Long warning buzz for rejected / anti-passback
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator.vibrate(
                            VibrationEffect.createOneShot(450, VibrationEffect.DEFAULT_AMPLITUDE)
                        )
                    } else {
                        @Suppress("DEPRECATION")
                        vibrator.vibrate(450)
                    }
                }
            }
        } catch (_: Exception) {}
    }

    fun generateDailyReportSummary(): String {
        val logs = scanLogsList.value
        val granted = logs.filter { it.status == VerificationStatus.GRANTED }
        val passbackBlocked = logs.filter { it.status == VerificationStatus.REJECTED_PASSBACK }
        val suspendedBlocked = logs.filter { it.status == VerificationStatus.REJECTED_INELIGIBLE }
        val wrongHallBlocked = logs.filter { it.status == VerificationStatus.REJECTED_WRONG_HALL }

        val bCount = granted.count { it.mealSession == MealSession.BREAKFAST }
        val lCount = granted.count { it.mealSession == MealSession.LUNCH }
        val dCount = granted.count { it.mealSession == MealSession.DINNER }

        val todayDate = SimpleDateFormat("EEEE, MMMM dd, yyyy", Locale.US).format(Date())

        return """
==================================================
MIZAN-TEPI UNIVERSITY - TEPI CAMPUS
STUDENT CAFETERIA DAILY CONSUMPTION REPORT
Date: $todayDate
Generated by: Cafeteria Verification System (OOSAD)
==================================================

1. MEAL CONSUMPTION SUMMARY
--------------------------------------------------
- Total Meals Served: ${granted.size}
  * Breakfast (06:30 - 09:00): $bCount students
  * Lunch     (11:45 - 14:30): $lCount students
  * Dinner    (18:00 - 21:00): $dCount students

2. SECURITY & FRAUD PREVENTION (ANTI-PASSBACK)
--------------------------------------------------
- Anti-Passback Duplicates Prevented: ${passbackBlocked.size} attempts
- Suspended/Ineligible Cards Blocked: ${suspendedBlocked.size} attempts
- Incorrect Hall Re-routes:           ${wrongHallBlocked.size} attempts
- Total Access Anomalies Blocked:    ${passbackBlocked.size + suspendedBlocked.size + wrongHallBlocked.size}

3. CAMPUS NETWORK RESILIENCE
--------------------------------------------------
- Local Offline Cached Scans: ${logs.count { it.isOfflineCached }}
- Pending Central Sync:       ${unsyncedCount.value}
- Status: Fully synchronized & operational.
==================================================
        """.trimIndent()
    }
}
