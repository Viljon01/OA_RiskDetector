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
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import android.app.Activity
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)

@Composable
fun OrganizationProfileScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val allPatients by viewModel.allPatients.collectAsState()
    val facilityName by viewModel.currentFacilityName.collectAsState()
    val doctorName by viewModel.currentDoctorName.collectAsState()
    val context = LocalContext.current

    // Aggregate data
    val patientsByDoctor = remember(allPatients) { allPatients.groupBy { it.doctorName.takeIf { name -> name.isNotBlank() } ?: "Unknown Doctor" } }
    
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
                        Text("Organizational Profile", fontSize = 18.sp, fontWeight = FontWeight.SemiBold) 
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = PrimaryTealLight),
                border = androidx.compose.foundation.BorderStroke(1.dp, SyncBorder),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text("Facility Overview", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = OnSurfaceText)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(facilityName, fontSize = 20.sp, fontWeight = FontWeight.Black, color = PrimaryTealDark)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Total Patients Screened", fontSize = 12.sp, color = OnSurfaceVariantText)
                            Text("${allPatients.size}", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = OnSurfaceText)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Active Doctors", fontSize = 12.sp, color = OnSurfaceVariantText)
                            Text("${patientsByDoctor.keys.size}", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = OnSurfaceText)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                "Doctor Histories & Patient Loads",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = OnSurfaceText,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            val doctorEntries = remember(patientsByDoctor) { patientsByDoctor.entries.toList() }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    com.example.ui.components.LanguageSelectionSection(viewModel)
                }
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    com.example.ui.components.VoiceTranscriptionCard(
                        title = "Facility Voice Dictation & Transcription",
                        onTranscriptionComplete = { text ->
                            // Facility notes updated via transcription
                        }
                    )
                }
                
                items(doctorEntries, key = { it.key }) { (docName, docPatients) ->
                    DoctorStatsCard(
                        doctorName = docName,
                        patients = docPatients,
                        isCurrentUser = docName == doctorName
                    )
                }
            }
        }
    }
}

@Composable
fun DoctorStatsCard(doctorName: String, patients: List<com.example.data.PatientEntity>, isCurrentUser: Boolean) {
    val highRiskCount = patients.count { it.riskTier == "HIGH RISK" }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isCurrentUser) PrimaryTeal else BorderItem),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(if (isCurrentUser) PrimaryTeal else BorderLight, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = if (isCurrentUser) Color.White else OnSurfaceVariantText)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(doctorName, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = OnSurfaceText)
                        if (isCurrentUser) {
                            Text("Current User", fontSize = 10.sp, color = PrimaryTeal, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text("${patients.size}", fontSize = 20.sp, fontWeight = FontWeight.Black, color = OnSurfaceText)
                    Text("Patients", fontSize = 10.sp, color = OnSurfaceVariantText)
                }
            }
            
            if (highRiskCount > 0) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(HighRiskBg, RoundedCornerShape(8.dp))
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Identified High Risk Cases", fontSize = 12.sp, color = HighRiskText)
                    Text("$highRiskCount", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = HighRiskText)
                }
            }
        }
    }
}
