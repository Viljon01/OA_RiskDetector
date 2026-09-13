package com.example.pose

import android.graphics.PointF
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions
import com.google.mlkit.vision.pose.PoseLandmark

class KneePoseDetector {

    private val options = PoseDetectorOptions.Builder()
        .setDetectorMode(PoseDetectorOptions.STREAM_MODE)
        .build()

    private val detector = PoseDetection.getClient(options)

    fun process(
        image: InputImage,
        onResult: (Pose?) -> Unit
    ) {
        detector.process(image)
            .addOnSuccessListener { pose ->
                onResult(pose)
            }
            .addOnFailureListener {
                onResult(null)
            }
    }

    fun close() {
        detector.close()
    }
}

fun angle(
    first: PointF,
    center: PointF,
    last: PointF
): Float {

    val radians = kotlin.math.atan2(
        (last.y - center.y).toDouble(),
        (last.x - center.x).toDouble()
    ) -
            kotlin.math.atan2(
                (first.y - center.y).toDouble(),
                (first.x - center.x).toDouble()
            )

    var degrees = Math.toDegrees(radians)

    if (degrees < 0) {
        degrees += 360
    }

    if (degrees > 180) {
        degrees = 360 - degrees
    }

    return degrees.toFloat()
}
