package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MealCardDao {

    @Query("SELECT * FROM students ORDER BY fullName ASC")
    fun getAllStudentsFlow(): Flow<List<StudentEntity>>

    @Query("SELECT * FROM students ORDER BY fullName ASC")
    suspend fun getAllStudents(): List<StudentEntity>

    @Query("SELECT * FROM students WHERE mtuId = :mtuId LIMIT 1")
    suspend fun getStudentByMtuId(mtuId: String): StudentEntity?

    @Query("SELECT * FROM students WHERE mtuId LIKE '%' || :query || '%' OR fullName LIKE '%' || :query || '%' OR department LIKE '%' || :query || '%'")
    fun searchStudentsFlow(query: String): Flow<List<StudentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudents(students: List<StudentEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudent(student: StudentEntity): Long

    @Update
    suspend fun updateStudent(student: StudentEntity)

    @Query("UPDATE students SET status = :newStatus WHERE mtuId = :mtuId")
    suspend fun updateStudentStatus(mtuId: String, newStatus: String)

    @Query("UPDATE students SET dietaryType = :dietaryType WHERE mtuId = :mtuId")
    suspend fun updateStudentDietary(mtuId: String, dietaryType: String)

    @Query("UPDATE students SET diningHall = :hall WHERE mtuId = :mtuId")
    suspend fun updateStudentHall(mtuId: String, hall: String)

    // Scan Logs
    @Query("SELECT * FROM scan_logs ORDER BY timestampMillis DESC")
    fun getAllScanLogsFlow(): Flow<List<ScanLogEntity>>

    @Query("SELECT * FROM scan_logs WHERE mtuId = :mtuId ORDER BY timestampMillis DESC")
    fun getScanLogsForStudentFlow(mtuId: String): Flow<List<ScanLogEntity>>

    @Query("SELECT * FROM scan_logs WHERE mtuId = :mtuId AND mealSession = :mealSession AND timestampMillis >= :startOfDayMillis AND status = 'GRANTED' LIMIT 1")
    suspend fun getSuccessfulScanForSessionToday(
        mtuId: String,
        mealSession: String,
        startOfDayMillis: Long
    ): ScanLogEntity?

    @Query("SELECT * FROM scan_logs WHERE isSyncedToServer = 0")
    suspend fun getUnsyncedOfflineLogs(): List<ScanLogEntity>

    @Query("UPDATE scan_logs SET isSyncedToServer = 1 WHERE isSyncedToServer = 0")
    suspend fun markAllLogsAsSynced()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScanLog(log: ScanLogEntity): Long

    @Query("DELETE FROM scan_logs")
    suspend fun clearAllScanLogs()

    @Query("SELECT COUNT(*) FROM scan_logs WHERE status = 'GRANTED' AND timestampMillis >= :startOfDayMillis")
    suspend fun getGrantedCountToday(startOfDayMillis: Long): Int

    @Query("SELECT COUNT(*) FROM scan_logs WHERE status = 'REJECTED_PASSBACK' AND timestampMillis >= :startOfDayMillis")
    suspend fun getPassbackViolationsToday(startOfDayMillis: Long): Int
}
