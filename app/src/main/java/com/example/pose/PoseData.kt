package com.example.pose

data class KneeFrame(
    val timestamp: Long,

    val leftKneeAngle: Float,
    val rightKneeAngle: Float,

    val leftHipAngle: Float,
    val rightHipAngle: Float,

    val leftKneeX: Float,
    val rightKneeX: Float,

    val leftKneeY: Float,
    val rightKneeY: Float
)

data class KneeAssessment(
    val testName: String,

    val leftMinAngle: Float,
    val leftMaxAngle: Float,

    val rightMinAngle: Float,
    val rightMaxAngle: Float,

    val leftRom: Float,
    val rightRom: Float,

    val asymmetry: Float,

    val movementQuality: String,
    val postureStatus: String,

    val riskScore: Int,
    val recommendation: String
)
