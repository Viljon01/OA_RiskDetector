package com.example.pose

import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseLandmark

class KneeAnalyzer {

    fun analyzePose(
        pose: Pose,
        timestamp: Long
    ): KneeFrame? {

        val leftHip =
            pose.getPoseLandmark(PoseLandmark.LEFT_HIP)?.position

        val leftKnee =
            pose.getPoseLandmark(PoseLandmark.LEFT_KNEE)?.position

        val leftAnkle =
            pose.getPoseLandmark(PoseLandmark.LEFT_ANKLE)?.position

        val rightHip =
            pose.getPoseLandmark(PoseLandmark.RIGHT_HIP)?.position

        val rightKnee =
            pose.getPoseLandmark(PoseLandmark.RIGHT_KNEE)?.position

        val rightAnkle =
            pose.getPoseLandmark(PoseLandmark.RIGHT_ANKLE)?.position

        if (
            leftHip == null ||
            leftKnee == null ||
            leftAnkle == null ||
            rightHip == null ||
            rightKnee == null ||
            rightAnkle == null
        ) {
            return null
        }

        val leftKneeAngle =
            angle(leftHip, leftKnee, leftAnkle)

        val rightKneeAngle =
            angle(rightHip, rightKnee, rightAnkle)

        val leftHipAngle =
            angle(
                pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)?.position
                    ?: return null,
                leftHip,
                leftKnee
            )

        val rightHipAngle =
            angle(
                pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)?.position
                    ?: return null,
                rightHip,
                rightKnee
            )

        return KneeFrame(
            timestamp = timestamp,

            leftKneeAngle = leftKneeAngle,
            rightKneeAngle = rightKneeAngle,

            leftHipAngle = leftHipAngle,
            rightHipAngle = rightHipAngle,

            leftKneeX = leftKnee.x,
            rightKneeX = rightKnee.x,

            leftKneeY = leftKnee.y,
            rightKneeY = rightKnee.y
        )
    }
}
