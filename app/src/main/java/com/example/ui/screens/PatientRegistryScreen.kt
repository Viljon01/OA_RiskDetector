package com.example.ui.screens

import com.example.utils.translateAsState

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.viewmodel.MainViewModel
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import com.example.utils.CsvExporter
import kotlinx.coroutines.launch
import androidx.compose.material.icons.filled.Share


@OptIn(ExperimentalMaterial3Api::class)

@Composable
fun PatientRegistryScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onPatientSelected: (String) -> Unit
) {
    val allPatients by viewModel.allPatients.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    // Group patients by name to get a unique list of profiles, keeping the most recent entry
    val uniquePatients = remember(allPatients, searchQuery) {
        allPatients
            .groupBy { it.name.trim().lowercase() }
            .map { it.value.maxByOrNull { p -> p.timestamp }!! }
            .filter { it.name.contains(searchQuery, ignoreCase = true) }
            .sortedByDescending { it.timestamp }
    }
    
    val patientCounts = remember(allPatients) {
        allPatients.groupingBy { it.name.trim().lowercase() }.eachCount()
    }
        
    val syncedCount by viewModel.syncedCount.collectAsState()
    val totalCount by viewModel.totalCount.collectAsState()
    val pendingCount = totalCount - syncedCount
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    val csvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                val success = CsvExporter.exportToCsv(context, uri, allPatients)
                if (success) {
                    Toast.makeText(context, "Exported successfully!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Export failed.", Toast.LENGTH_SHORT).show()
                }
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
                        Text("Patient Registry", fontSize = 18.sp, fontWeight = FontWeight.SemiBold) 
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    com.example.ui.components.SyncStatusIcon(pendingCount)
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = {
                        val timestamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault()).format(java.util.Date())
                        csvLauncher.launch("osteoscreen_patients_$timestamp.csv")
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "Export to CSV")
                    }
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                placeholder = { Text("Search patient by name...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryTeal,
                    unfocusedBorderColor = BorderItem,
                    focusedContainerColor = SurfaceWhite,
                    unfocusedContainerColor = SurfaceWhite
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            com.example.ui.components.VoiceTranscriptionCard(
                title = "Voice Patient Search & Transcription",
                onTranscriptionComplete = { text ->
                    searchQuery = text.take(20)
                }
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "All Registered Patients",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = OnSurfaceText,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            if (uniquePatients.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No records found.", color = OnSurfaceVariantText)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(uniquePatients, key = { it.id }) { patient ->
                        RegistryPatientCard(
                            patient = patient,
                            visitCount = patientCounts[patient.name.trim().lowercase()] ?: 1,
                            onClick = { onPatientSelected(patient.name) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RegistryPatientCard(
    patient: com.example.data.PatientEntity,
    visitCount: Int,
    onClick: () -> Unit
) {
    val (bgColor, textColor) = when (patient.riskTier) {
        "HIGH RISK" -> HighRiskBg to HighRiskText
        "MODERATE RISK" -> ModRiskBg to ModRiskText
        "LOW RISK", "NORMAL" -> LowRiskBg to LowRiskText
        else -> SurfaceWhite to OnSurfaceVariantText
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderItem),
        onClick = onClick,
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = patient.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = OnSurfaceText
                )
                Text(
                    text = "${patient.age} yrs • ${patient.gender}",
                    fontSize = 12.sp,
                    color = OnSurfaceVariantText,
                    modifier = Modifier.padding(top = 2.dp)
                )
                if (patient.area.isNotBlank()) {
                    Text(
                        text = "📍 ${patient.area}",
                        fontSize = 12.sp,
                        color = OnSurfaceVariantText,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                Text(
                    text = "$visitCount recorded visit(s)",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = PrimaryTeal,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            Box(
                modifier = Modifier
                    .background(bgColor, RoundedCornerShape(50))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "LATEST: ${patient.riskTier}",
                    color = textColor,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}
