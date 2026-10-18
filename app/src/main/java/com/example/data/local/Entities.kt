package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.DietaryType
import com.example.data.model.DiningHall
import com.example.data.model.MealScanLog
import com.example.data.model.MealSession
import com.example.data.model.Student
import com.example.data.model.StudentStatus
import com.example.data.model.VerificationStatus

@Entity(tableName = "students")
data class StudentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val mtuId: String,
    val fullName: String,
    val department: String,
    val yearOfStudy: Int,
    val diningHall: String,
    val dietaryType: String,
    val status: String,
    val barcodeNfcUid: String,
    val avatarSeed: String,
    val phone: String
) {
    fun toDomain(): Student {
        return Student(
            id = id,
            mtuId = mtuId,
            fullName = fullName,
            department = department,
            yearOfStudy = yearOfStudy,
            diningHall = runCatching { DiningHall.valueOf(diningHall) }.getOrDefault(DiningHall.HALL_1_MAIN),
            dietaryType = runCatching { DietaryType.valueOf(dietaryType) }.getOrDefault(DietaryType.REGULAR_NON_FASTING),
            status = runCatching { StudentStatus.valueOf(status) }.getOrDefault(StudentStatus.ACTIVE),
            barcodeNfcUid = barcodeNfcUid,
            avatarSeed = avatarSeed,
            phone = phone
        )
    }

    companion object {
        fun fromDomain(student: Student): StudentEntity {
            return StudentEntity(
                id = student.id,
                mtuId = student.mtuId,
                fullName = student.fullName,
                department = student.department,
                yearOfStudy = student.yearOfStudy,
                diningHall = student.diningHall.name,
                dietaryType = student.dietaryType.name,
                status = student.status.name,
                barcodeNfcUid = student.barcodeNfcUid,
                avatarSeed = student.avatarSeed,
                phone = student.phone
            )
        }
    }
}

@Entity(tableName = "scan_logs")
data class ScanLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val scanId: String,
    val mtuId: String,
    val studentName: String,
    val department: String,
    val mealSession: String,
    val timestampMillis: Long,
    val diningHall: String,
    val gatekeeperName: String,
    val status: String,
    val rejectionReason: String?,
    val isOfflineCached: Boolean,
    val isSyncedToServer: Boolean
) {
    fun toDomain(): MealScanLog {
        return MealScanLog(
            id = id,
            scanId = scanId,
            mtuId = mtuId,
            studentName = studentName,
            department = department,
            mealSession = runCatching { MealSession.valueOf(mealSession) }.getOrDefault(MealSession.LUNCH),
            timestampMillis = timestampMillis,
            diningHall = runCatching { DiningHall.valueOf(diningHall) }.getOrDefault(DiningHall.HALL_1_MAIN),
            gatekeeperName = gatekeeperName,
            status = runCatching { VerificationStatus.valueOf(status) }.getOrDefault(VerificationStatus.GRANTED),
            rejectionReason = rejectionReason,
            isOfflineCached = isOfflineCached,
            isSyncedToServer = isSyncedToServer
        )
    }

    companion object {
        fun fromDomain(log: MealScanLog): ScanLogEntity {
            return ScanLogEntity(
                id = log.id,
                scanId = log.scanId,
                mtuId = log.mtuId,
                studentName = log.studentName,
                department = log.department,
                mealSession = log.mealSession.name,
                timestampMillis = log.timestampMillis,
                diningHall = log.diningHall.name,
                gatekeeperName = log.gatekeeperName,
                status = log.status.name,
                rejectionReason = log.rejectionReason,
                isOfflineCached = log.isOfflineCached,
                isSyncedToServer = log.isSyncedToServer
            )
        }
    }
}
