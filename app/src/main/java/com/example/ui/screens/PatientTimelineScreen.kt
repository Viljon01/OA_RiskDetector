package com.example.ui.screens

import com.example.utils.translateAsState

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.example.utils.GeminiService
import kotlinx.coroutines.launch
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Warning
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput

@OptIn(ExperimentalMaterial3Api::class)

@Composable
fun PatientTimelineScreen(
    viewModel: MainViewModel,
    patientName: String,
    onBack: () -> Unit,
    onViewReport: (Int) -> Unit,
    onStartAssessment: () -> Unit
) {
    LaunchedEffect(patientName) {
        viewModel.loadPatientHistory(patientName)
    }
    


    val history by viewModel.patientHistory.collectAsState()
    val latestPatient = history.lastOrNull()
    
    val syncedCount by viewModel.syncedCount.collectAsState()
    val totalCount by viewModel.totalCount.collectAsState()
    val pendingCount = totalCount - syncedCount
    
    val coroutineScope = rememberCoroutineScope()
    var aiSummary by remember { mutableStateOf<String?>(null) }
    var isGeneratingAi by remember { mutableStateOf(false) }

    LaunchedEffect(history) {
        if (history.isNotEmpty() && aiSummary == null && !isGeneratingAi) {
            isGeneratingAi = true
            val lastThree = history.sortedByDescending { it.timestamp }.take(3).reversed()
            val latestPatient = history.lastOrNull()
            coroutineScope.launch {
                val prompt = buildString {
                    append("You are an AI medical assistant specializing in Osteoarthritis (OA). ")
                    append("Analyze this patient's profile to summarize the reason for their OA risk assessment and predict their OA risk trajectory based on the provided data. ")
                    append("Patient: ${latestPatient?.age}yo ${latestPatient?.gender}, BMI: ${latestPatient?.bmi}, Occupation: ${latestPatient?.occupation}. ")
                    append("Last assessments (oldest to newest): ")
                    lastThree.forEachIndexed { i, p ->
                        append("[Visit ${i+1}]: Pain: ${p.painLevel}/10, Stiffness: ${p.stiffness}, Risk Tier: ${p.riskTier}. ")
                    }
                    append("Provide a concise summary (max 3 short paragraphs). Keep it professional. ")
                    
                    val lang = viewModel.currentLanguage.value
                    if (lang != "EN") {
                        append("IMPORTANT: You MUST write the summary entirely in the language corresponding to language code '${lang}'.")
                    }
                }
                val responseStr = GeminiService.getAiSummary(prompt)
                try {
                    val json = org.json.JSONObject(responseStr)
                    aiSummary = json.optString("summary", responseStr)
                } catch (e: Exception) {
                    aiSummary = responseStr
                }
                isGeneratingAi = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        coil.compose.AsyncImage(
                            model = com.example.R.drawable.custom_logo,
                            contentDescription = "Logo",
                            modifier = Modifier.size(32.dp),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                            filterQuality = androidx.compose.ui.graphics.FilterQuality.High
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Secure Clinical Timeline", fontSize = 18.sp, fontWeight = FontWeight.SemiBold) 
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    com.example.ui.components.SyncStatusIcon(pendingCount)
                    Spacer(modifier = Modifier.width(16.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PrimaryTeal,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = BackgroundLight
    ) { paddingValues ->
        if (history.isEmpty() || latestPatient == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PrimaryTeal)
            }
            return@Scaffold
        }

        val sortedHistory = remember(history) { history.sortedByDescending { it.timestamp } }
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = "Secure", tint = Color(0xFFF57C00), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Confidential Patient Record (HIPAA Compliant View)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFE65100)
                        )
                    }
                }
            }

            item {
                PatientHeader(latestPatient)
            }
            item {
                if (latestPatient.riskTier == "PENDING") {
                    Button(
                        onClick = {
                            viewModel.loadPatient(latestPatient.id)
                            onStartAssessment()
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF57F17)), // Amber
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = viewModel.translateAsState("resume_pending") ?: "RESUME PENDING ASSESSMENT",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                } else {
                    Button(
                        onClick = {
                            val newRecord = latestPatient.copy(
                                id = 0,
                                painLevel = 0,
                                stiffness = "",
                                riskScore = 0.0,
                                riskTier = "PENDING",
                                isSynced = false,
                                timestamp = System.currentTimeMillis()
                            )
                            viewModel.insertPatient(newRecord) {
                                onStartAssessment()
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = viewModel.translateAsState("start_new_oa_assessment") ?: "START NEW ASSESSMENT",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            if (history.isNotEmpty()) {
                item {
                    Text(
                        "AI Risk Prediction & Trend (Last 3)",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = OnSurfaceText
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    val lastThree = remember(history) { history.sortedByDescending { it.timestamp }.take(3).reversed() }
                    
                    if (lastThree.size > 1) {
                        RiskTrendChart(lastThree)
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF3E8FF)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD8B4FE)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = "AI", tint = Color(0xFF9333EA))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("AI Analysis", fontWeight = FontWeight.Bold, color = Color(0xFF7E22CE))
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            if (aiSummary != null) {
                                Text(aiSummary!!, fontSize = 14.sp, color = OnSurfaceText, lineHeight = 20.sp)
                            } else if (isGeneratingAi) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color(0xFF9333EA), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Analyzing patient profile...", fontSize = 12.sp, color = Color(0xFF7E22CE))
                            } else {

                            }
                        }
                    }
                }
            }

            item {
                Text(
                    "Chronological Assessment History",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = OnSurfaceText
                )
            }

            // Timeline items
            
            itemsIndexed(sortedHistory, key = { _, record -> record.id }) { index, record ->
                TimelineItem(
                    record = record,
                    isFirst = index == 0,
                    isLast = index == sortedHistory.size - 1,
                    onClick = { onViewReport(record.id) }
                )
            }
        }
    }
}

@Composable
fun PatientHeader(patient: com.example.data.PatientEntity) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = PrimaryTealLight),
        border = androidx.compose.foundation.BorderStroke(1.dp, SyncBorder),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(patient.name, fontSize = 24.sp, fontWeight = FontWeight.Black, color = PrimaryTealDark)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Age / Gender", fontSize = 12.sp, color = OnSurfaceVariantText)
                    Text("${patient.age} / ${patient.gender}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = OnSurfaceText)
                }
                Column {
                    Text("BMI", fontSize = 12.sp, color = OnSurfaceVariantText)
                    Text(String.format("%.1f", patient.bmi), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = OnSurfaceText)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Primary Physician", fontSize = 12.sp, color = OnSurfaceVariantText)
                    Text(patient.doctorName.takeIf { it.isNotBlank() } ?: "Unknown", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = OnSurfaceText)
                }
            }
            if (patient.phoneNumber.isNotBlank() || patient.area.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (patient.phoneNumber.isNotBlank()) {
                        Column {
                            Text("Phone Number", fontSize = 12.sp, color = OnSurfaceVariantText)
                            Text(patient.phoneNumber, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = OnSurfaceText)
                        }
                    }
                    if (patient.area.isNotBlank()) {
                        Column(horizontalAlignment = if (patient.phoneNumber.isNotBlank()) Alignment.End else Alignment.Start) {
                            Text("Area", fontSize = 12.sp, color = OnSurfaceVariantText)
                            Text(patient.area, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = OnSurfaceText)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RiskTrendChart(history: List<com.example.data.PatientEntity>) {
    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    
    val formatter = remember { java.text.SimpleDateFormat("MMM dd", java.util.Locale.getDefault()) }
    val textPaint = remember { 
        android.graphics.Paint().apply {
            color = android.graphics.Color.DKGRAY
            textSize = 32f
            textAlign = android.graphics.Paint.Align.CENTER
        }
    }
    val scorePaint = remember {
        android.graphics.Paint().apply {
            color = android.graphics.Color.BLACK
            textSize = 36f
            isFakeBoldText = true
            textAlign = android.graphics.Paint.Align.CENTER
        }
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(250.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderItem),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxSize()
        ) {
            Canvas(modifier = Modifier.fillMaxSize().pointerInput(Unit) {
                detectTapGestures(onTap = { offset ->
                    val sorted = history.sortedBy { it.timestamp }
                    val stepX = size.width / (sorted.size - 1)
                    val x = offset.x
                    val index = (x / stepX).toInt().coerceIn(0, sorted.size - 1)
                    selectedIndex = if (selectedIndex == index) null else index
                })
            }) {
                val sorted = history.sortedBy { it.timestamp }
                val maxScore = 100f
                val minScore = 0f
                
                val width = size.width
                val height = size.height - 60f - 60f
                val stepX = if (sorted.size > 1) width / (sorted.size - 1) else width
                
                val path = Path()
                
                
                sorted.forEachIndexed { index, record ->
                    val score = record.riskScore.toFloat().coerceIn(minScore, maxScore)
                    val y = height - ((score / maxScore) * height)
                    val x = index * stepX
                    
                    if (index == 0) {
                        path.moveTo(x, y)
                    } else {
                        path.lineTo(x, y)
                    }
                    
                    val dotColor = when (record.riskTier) {
                        "HIGH RISK" -> HighRiskText
                        "MODERATE RISK" -> ModRiskText
                        else -> LowRiskText
                    }
                    
                    drawCircle(
                        color = dotColor,
                        radius = 6.dp.toPx(),
                        center = Offset(x, y)
                    )
                    
                    // Draw Month/Date Label
                    val dateStr = formatter.format(java.util.Date(record.timestamp))
                    val canvasContext = drawContext.canvas.nativeCanvas
                    canvasContext.drawText(dateStr, x, height + 40f, textPaint)
                    
                    // Draw Risk Score value label above the dot
                    canvasContext.drawText(score.toInt().toString(), x, y - 24f, scorePaint)
                }
                
                drawPath(
                    path = path,
                    color = PrimaryTeal,
                    style = Stroke(width = 3.dp.toPx())
                )
            }
            
            // Draw tooltip if selectedIndex != null
            if (selectedIndex != null) {
                val record = history[selectedIndex!!]
                Card(
                    modifier = Modifier.align(Alignment.TopCenter),
                    colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.8f))
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text("Risk Score: ${record.riskScore.toInt()}", color = Color.White, fontSize = 12.sp)
                        Text("Joint Alignment: ${record.aiRiskScore.toInt()}", color = Color.White, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun TimelineItem(
    record: com.example.data.PatientEntity,
    isFirst: Boolean,
    isLast: Boolean,
    onClick: () -> Unit
) {
    val formatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    val dateStr = formatter.format(Date(record.timestamp))

    val (bgColor, textColor) = when (record.riskTier) {
        "HIGH RISK" -> HighRiskBg to HighRiskText
        "MODERATE RISK" -> ModRiskBg to ModRiskText
        "LOW RISK", "NORMAL" -> LowRiskBg to LowRiskText
        else -> SurfaceWhite to OnSurfaceVariantText
    }

    Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        // Timeline graphic
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(32.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .background(PrimaryTeal, CircleShape)
                    .border(4.dp, PrimaryTealLight, CircleShape)
            )
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .weight(1f)
                        .background(BorderLight)
                )
            }
        }
        
        Spacer(modifier = Modifier.width(8.dp))
        
        // Card content
        Card(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = if (!isLast) 24.dp else 0.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderItem),
            onClick = onClick,
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(dateStr, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = OnSurfaceText)
                    
                    Box(
                        modifier = Modifier
                            .background(bgColor, RoundedCornerShape(50))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = record.riskTier,
                            color = textColor,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Attending Doctor", fontSize = 10.sp, color = OnSurfaceVariantText)
                        Text(record.doctorName.takeIf { it.isNotBlank() } ?: "Unknown", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = OnSurfaceText)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Risk Score", fontSize = 10.sp, color = OnSurfaceVariantText)
                        Text("${record.riskScore.toInt()}/100", fontSize = 14.sp, fontWeight = FontWeight.Black, color = PrimaryTeal)
                    }
                }
            }
        }
    }
}
