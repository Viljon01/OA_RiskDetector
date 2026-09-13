package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "patients")
data class PatientEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val age: String,
    val gender: String,
    val occupation: String,
    val phoneNumber: String = "",
    val area: String = "",
    val heightCm: String,
    val weightKg: String,
    val bmi: Double,
    val painLevel: Int = 0,
    val stiffness: String = "",
    val riskScore: Double = 0.0,
    val riskTier: String = "PENDING",
    val isSynced: Boolean = false,
    val doctorName: String = "",
    val facilityName: String = "",
    val aiSummary: String = "",
    val aiRiskScore: Double = 0.0,
    val jointAngle: Double = 0.0,
    val hardwareData: String = "",
    val quickAssessmentData: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
