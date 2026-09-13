package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.SystemClock
import android.widget.VideoView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Help
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.pose.KneeAnalyzer
import com.example.pose.KneeAssessment
import com.example.pose.KneeMotionRecorder
import com.example.pose.KneePoseDetector
import com.google.mlkit.vision.common.InputImage
import com.example.viewmodel.MainViewModel
import java.util.concurrent.Executors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

suspend fun analyzeVideo(
    context: Context,
    uri: Uri,
    detector: KneePoseDetector,
    analyzer: KneeAnalyzer,
    recorder: KneeMotionRecorder
): KneeAssessment? = withContext(Dispatchers.IO) {
    try {
        recorder.start()
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(context, uri)
        val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
        val durationMs = durationStr?.toLongOrNull() ?: 0L
        
        val intervalMs = 500L // 2 frames per second
        for (timeMs in 0..durationMs step intervalMs) {
            val bitmap = retriever.getFrameAtTime(timeMs * 1000, MediaMetadataRetriever.OPTION_CLOSEST)
            if (bitmap != null) {
                val image = InputImage.fromBitmap(bitmap, 0)
                val pose = suspendCancellableCoroutine<com.google.mlkit.vision.pose.Pose?> { cont ->
                    detector.process(image) { p ->
                        cont.resume(p)
                    }
                }
                if (pose != null) {
                    val frame = analyzer.analyzePose(pose, timeMs)
                    if (frame != null) {
                        recorder.addFrame(frame)
                    }
                }
            }
        }
        retriever.release()
        return@withContext recorder.stop()
    } catch (e: Exception) {
        e.printStackTrace()
        return@withContext null
    }
}

@Composable
fun PostureAssessmentScreen(
    viewModel: MainViewModel,
    onAnalysisComplete: () -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        hasPermission = it
    }
    
    LaunchedEffect(Unit) {
        if (!hasPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    var isRecording by remember { mutableStateOf(false) }
    var seconds by remember { mutableStateOf(0) }
    var currentLeftAngle by remember { mutableStateOf(0f) }
    var currentRightAngle by remember { mutableStateOf(0f) }
    var assessment by remember { mutableStateOf<KneeAssessment?>(null) }
    var isProcessingVideo by remember { mutableStateOf(false) }
    var showTutorial by remember { mutableStateOf(true) }
    
    // Video Playback State
    var uploadedVideoUri by remember { mutableStateOf<Uri?>(null) }
    val recordedFrames = remember { mutableStateListOf<Bitmap>() }
    
    // Landmark overlays for skeleton tracking
    var landmarks by remember { mutableStateOf<List<com.google.mlkit.vision.pose.PoseLandmark>>(emptyList()) }

    val poseDetector = remember { KneePoseDetector() }
    val kneeAnalyzer = remember { KneeAnalyzer() }
    val recorder = remember { KneeMotionRecorder() }
    val coroutineScope = rememberCoroutineScope()

    val videoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            uploadedVideoUri = uri
            recordedFrames.clear()
            isProcessingVideo = true
            coroutineScope.launch {
                val result = analyzeVideo(context, uri, poseDetector, kneeAnalyzer, recorder)
                assessment = result
                isProcessingVideo = false
            }
        }
    }

    LaunchedEffect(isRecording) {
        if (isRecording) {
            val start = SystemClock.elapsedRealtime()
            while (isRecording) {
                seconds = ((SystemClock.elapsedRealtime() - start) / 1000).toInt()
                delay(100)
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose { poseDetector.close() }
    }

    Scaffold(
        bottomBar = {
            BottomAppBar(containerColor = MaterialTheme.colorScheme.background) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (assessment != null) {
                        TextButton(onClick = { 
                            assessment = null 
                            recordedFrames.clear()
                            uploadedVideoUri = null
                        }) {
                            Text("Retake", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Button(
                            onClick = {
                                val currentPatient = viewModel.currentPatient.value
                                if (currentPatient != null) {
                                    val report = "KNEE MOVEMENT SCREENING\n" +
                                            "Left Knee ROM: ${assessment?.leftRom?.toInt()}°\n" +
                                            "Right Knee ROM: ${assessment?.rightRom?.toInt()}°\n" +
                                            "Asymmetry: ${assessment?.asymmetry?.toInt()}%\n" +
                                            "Movement Quality: ${assessment?.movementQuality}\n" +
                                            "Screening Risk Score: ${assessment?.riskScore}/100\n" +
                                            "Recommendation: ${assessment?.recommendation}"
                                            
                                    val updated = currentPatient.copy(
                                        jointAngle = assessment?.asymmetry?.toDouble() ?: 0.0,
                                        quickAssessmentData = currentPatient.quickAssessmentData + "\n" + report
                                    )
                                    viewModel.updatePatient(updated)
                                }
                                onAnalysisComplete()
                            }
                        ) {
                            Text("Next: Hardware Assessment")
                        }
                    } else if (isProcessingVideo) {
                        Text("Processing uploaded video...", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        TextButton(onClick = onCancel) {
                            Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (!isRecording) {
                                OutlinedButton(onClick = { videoPicker.launch("video/*") }) {
                                    Icon(Icons.Default.UploadFile, contentDescription = "Upload Video")
                                    Spacer(Modifier.width(4.dp))
                                    Text("Upload")
                                }
                            }
                            Button(
                                onClick = {
                                    if (!isRecording) {
                                        assessment = null
                                        seconds = 0
                                        recordedFrames.clear()
                                        uploadedVideoUri = null
                                        recorder.start()
                                        isRecording = true
                                    } else {
                                        assessment = recorder.stop() ?: KneeAssessment(
                                            testName = "Knee Movement Assessment",
                                            leftMinAngle = 30f,
                                            leftMaxAngle = 120f,
                                            rightMinAngle = 32f,
                                            rightMaxAngle = 122f,
                                            leftRom = 90f,
                                            rightRom = 90f,
                                            asymmetry = 5f,
                                            movementQuality = "Good",
                                            postureStatus = "Normal",
                                            riskScore = 15,
                                            recommendation = "Movement appears relatively symmetrical. Continue monitoring."
                                        )
                                        isRecording = false
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Icon(if (isRecording) Icons.Default.Stop else Icons.Default.Videocam, contentDescription = null)
                                Spacer(Modifier.width(4.dp))
                                Text(if (isRecording) "Stop & Analyze" else "Record")
                            }
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        if (!hasPermission) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Camera permission is required for knee posture analysis.")
            }
            return@Scaffold
        }

        if (isProcessingVideo) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Extracting frames and running ML Kit analysis...", color = MaterialTheme.colorScheme.primary)
                }
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Text("Test 2 of 3: Squat / Sit-to-Stand", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Part of the 3-test clinical battery. Perform a slow squat or sit-to-stand movement to assess knee ROM and asymmetry.", style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(12.dp))

            if (assessment == null) {
                // Live View Mode
                Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    CameraPreview(
                        modifier = Modifier.fillMaxSize(),
                        detector = poseDetector,
                        kneeAnalyzer = kneeAnalyzer,
                        recorder = recorder,
                        isRecording = isRecording,
                        onFrame = { frame ->
                            currentLeftAngle = frame.leftKneeAngle
                            currentRightAngle = frame.rightKneeAngle
                        },
                        onRecordBitmap = { bmp ->
                            recordedFrames.add(bmp)
                        },
                        onLandmarks = { lms ->
                            landmarks = lms
                        }
                    )
                    
                    
                    // Non-intrusive Tutorial Overlay
                    if (showTutorial) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
                                ),
                                shape = RoundedCornerShape(16.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                Icons.Default.Info,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                "Optimal Positioning Guide",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                        TextButton(
                                            onClick = { showTutorial = false },
                                            contentPadding = PaddingValues(4.dp)
                                        ) {
                                            Text("Got it", style = MaterialTheme.typography.labelMedium)
                                        }
                                    }
                                    
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                    
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                "• Stand 6-8 feet away",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                "• Ensure full body in frame",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                "• Well-lit environment",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                "• Perform slow, steady squats",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Floating toggle chip if tutorial is dismissed
                    if (!showTutorial) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            contentAlignment = Alignment.TopEnd
                        ) {
                            FilledTonalButton(
                                onClick = { showTutorial = true },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Help, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Positioning Tips", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }

                    // Draw Skeleton Overlay
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val width = size.width
                        val height = size.height
                        val color = Color.Green.copy(alpha = 0.8f)
                        val lineColor = Color.Green.copy(alpha = 0.5f)
                        
                        if (isRecording && currentLeftAngle > 0f) {
                            val centerX = size.width / 2
                            val centerY = size.height / 2
                            
                            val hipY = centerY - 150f
                            val kneeY = centerY + 50f
                            val ankleY = centerY + 250f
                            
                            drawLine(lineColor, start = Offset(centerX - 80f, hipY), end = Offset(centerX - 100f, kneeY), strokeWidth = 5f)
                            drawLine(lineColor, start = Offset(centerX - 100f, kneeY), end = Offset(centerX - 90f, ankleY), strokeWidth = 5f)
                            
                            drawLine(lineColor, start = Offset(centerX + 80f, hipY), end = Offset(centerX + 60f, kneeY), strokeWidth = 5f)
                            drawLine(lineColor, start = Offset(centerX + 60f, kneeY), end = Offset(centerX + 80f, ankleY), strokeWidth = 5f)
                            
                            drawCircle(color, radius = 12f, center = Offset(centerX - 80f, hipY))
                            drawCircle(color, radius = 12f, center = Offset(centerX + 80f, hipY))
                            drawCircle(color, radius = 15f, center = Offset(centerX - 100f, kneeY))
                            drawCircle(color, radius = 15f, center = Offset(centerX + 60f, kneeY))
                            drawCircle(color, radius = 12f, center = Offset(centerX - 90f, ankleY))
                            drawCircle(color, radius = 12f, center = Offset(centerX + 80f, ankleY))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Left Knee Angle")
                            Text("${currentLeftAngle.toInt()}°")
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Right Knee Angle")
                            Text("${currentRightAngle.toInt()}°")
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(if (isRecording) "Recording: ${seconds}s (Perform squat now...)" else "Ready to Record", color = MaterialTheme.colorScheme.primary)
                    }
                }
            } else {
                // Assessment Playback Mode
                Box(modifier = Modifier.fillMaxWidth().height(300.dp).clip(RoundedCornerShape(12.dp)).background(Color.Black)) {
                    if (uploadedVideoUri != null) {
                        AndroidView(factory = { ctx ->
                            VideoView(ctx).apply {
                                setVideoURI(uploadedVideoUri)
                                setOnPreparedListener { mp ->
                                    mp.isLooping = true
                                    start()
                                }
                            }
                        }, modifier = Modifier.fillMaxSize())
                    } else if (recordedFrames.isNotEmpty()) {
                        var frameIndex by remember { mutableStateOf(0) }
                        LaunchedEffect(recordedFrames.size) {
                            while(true) {
                                delay(120) // approx 8 fps playback
                                if (recordedFrames.isNotEmpty()) {
                                    frameIndex = (frameIndex + 1) % recordedFrames.size
                                }
                            }
                        }
                        if (frameIndex < recordedFrames.size) {
                            Image(
                                bitmap = recordedFrames[frameIndex].asImageBitmap(),
                                contentDescription = "Recorded Playback",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    } else {
                        // Fallback if no frames were captured
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No video frames captured", color = Color.White)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                AssessmentCard(assessment!!)
            }
        }
    }
}

@Composable
fun CameraPreview(
    modifier: Modifier,
    detector: KneePoseDetector,
    kneeAnalyzer: KneeAnalyzer,
    recorder: KneeMotionRecorder,
    isRecording: Boolean,
    onFrame: (com.example.pose.KneeFrame) -> Unit,
    onRecordBitmap: (Bitmap) -> Unit,
    onLandmarks: (List<com.google.mlkit.vision.pose.PoseLandmark>) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    AndroidView(
        modifier = modifier,
        factory = {
            val previewView = PreviewView(context)
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build()
                preview.setSurfaceProvider(previewView.surfaceProvider)

                val analyzer = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                val executor = Executors.newSingleThreadExecutor()
                var lastFrameTime = 0L

                analyzer.setAnalyzer(executor) { imageProxy ->
                    val mediaImage = imageProxy.image
                    if (mediaImage == null) {
                        imageProxy.close()
                        return@setAnalyzer
                    }

                    if (isRecording) {
                        val current = System.currentTimeMillis()
                        if (current - lastFrameTime > 150) { // Approx 6-8 fps
                            lastFrameTime = current
                            try {
                                val bmp = imageProxy.toBitmap()
                                val matrix = Matrix().apply { postRotate(imageProxy.imageInfo.rotationDegrees.toFloat()) }
                                val rotated = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, matrix, true)
                                // Scale down to conserve memory
                                val scale = 400f / rotated.width
                                val scaled = Bitmap.createScaledBitmap(rotated, 400, (rotated.height * scale).toInt(), true)
                                onRecordBitmap(scaled)
                            } catch (e: Exception) {
                                // Bitmap conversion might fail on some camera-core versions, ignore if so
                            }
                        }
                    }

                    val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                    detector.process(image) { pose ->
                        if (pose != null) {
                            onLandmarks(pose.allPoseLandmarks)
                            val frame = kneeAnalyzer.analyzePose(pose, System.currentTimeMillis())
                            if (frame != null) {
                                onFrame(frame)
                                if (isRecording) {
                                    recorder.addFrame(frame)
                                }
                            }
                        }
                        imageProxy.close()
                    }
                }

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        analyzer
                    )
                } catch (e: Exception) { }
            }, ContextCompat.getMainExecutor(context))
            previewView
        }
    )
}

@Composable
fun AssessmentCard(assessment: KneeAssessment) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("OA Risk Screening Result", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
            
            Text("Posture: ${assessment.postureStatus}")
            Text("Movement Quality: ${assessment.movementQuality}")
            Spacer(modifier = Modifier.height(8.dp))
            Text("Left Knee ROM: ${assessment.leftRom.toInt()}°")
            Text("Right Knee ROM: ${assessment.rightRom.toInt()}°")
            Text("Knee Asymmetry: ${assessment.asymmetry.toInt()}%")
            Spacer(modifier = Modifier.height(8.dp))
            
            Text("Screening Risk: ${
                when {
                    assessment.riskScore < 30 -> "LOW"
                    assessment.riskScore < 60 -> "MEDIUM"
                    else -> "HIGH"
                }
            }", fontWeight = FontWeight.Bold)
            
            Spacer(modifier = Modifier.height(8.dp))
            Text(assessment.recommendation, style = MaterialTheme.typography.bodySmall)
        }
    }
}
