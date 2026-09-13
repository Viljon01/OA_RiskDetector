package com.example.pose

class KneeMotionRecorder {

    private val frames = mutableListOf<KneeFrame>()

    private var recording = false

    fun start() {
        frames.clear()
        recording = true
    }

    fun addFrame(frame: KneeFrame) {

        if (!recording) {
            return
        }

        frames.add(frame)
    }

    fun stop(): KneeAssessment? {

        recording = false

        if (frames.size < 10) {
            return null
        }

        return calculateAssessment()
    }

    fun getFrames(): List<KneeFrame> {
        return frames.toList()
    }

    private fun calculateAssessment(): KneeAssessment {

        val leftAngles =
            frames.map { it.leftKneeAngle }

        val rightAngles =
            frames.map { it.rightKneeAngle }

        val leftMin = leftAngles.minOrNull() ?: 0f
        val leftMax = leftAngles.maxOrNull() ?: 0f

        val rightMin = rightAngles.minOrNull() ?: 0f
        val rightMax = rightAngles.maxOrNull() ?: 0f

        val leftRom =
            leftMax - leftMin

        val rightRom =
            rightMax - rightMin

        val asymmetry =
            if (maxOf(leftRom, rightRom) == 0f) {
                0f
            } else {
                kotlin.math.abs(leftRom - rightRom) /
                        maxOf(leftRom, rightRom) * 100f
            }

        val movementQuality =
            when {
                asymmetry < 10 -> "Good"
                asymmetry < 20 -> "Moderate"
                else -> "Asymmetric"
            }

        val postureStatus =
            when {
                asymmetry < 10 -> "Normal"
                asymmetry < 20 -> "Needs Attention"
                else -> "Abnormal"
            }

        var riskScore = 0

        if (leftRom < 40) {
            riskScore += 20
        }

        if (rightRom < 40) {
            riskScore += 20
        }

        if (asymmetry > 15) {
            riskScore += 20
        }

        if (asymmetry > 25) {
            riskScore += 20
        }

        if (leftMin < 30 || rightMin < 30) {
            riskScore += 10
        }

        if (riskScore > 100) {
            riskScore = 100
        }

        val recommendation =
            when {
                riskScore < 30 ->
                    "Movement appears relatively symmetrical. Continue monitoring."

                riskScore < 60 ->
                    "Some movement asymmetry or reduced range detected. Consider further clinical assessment."

                else ->
                    "Significant movement abnormality detected. Clinical evaluation is recommended."
            }

        return KneeAssessment(
            testName = "Knee Movement Assessment",

            leftMinAngle = leftMin,
            leftMaxAngle = leftMax,

            rightMinAngle = rightMin,
            rightMaxAngle = rightMax,

            leftRom = leftRom,
            rightRom = rightRom,

            asymmetry = asymmetry,

            movementQuality = movementQuality,
            postureStatus = postureStatus,

            riskScore = riskScore,
            recommendation = recommendation
        )
    }
}
