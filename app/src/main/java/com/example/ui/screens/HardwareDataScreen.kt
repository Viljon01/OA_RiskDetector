package com.example.ui.screens

import com.example.utils.translateAsState

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodel.MainViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)

@Composable
fun HardwareDataScreen(
    viewModel: MainViewModel,
    onGenerateReport: () -> Unit,
    onCancel: () -> Unit
) {
    val patient by viewModel.currentPatient.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    var footPressureFetched by remember { mutableStateOf(false) }
    var motionFetched by remember { mutableStateOf(false) }
    var muscleFetched by remember { mutableStateOf(false) }
    
    var isFetchingFootPressure by remember { mutableStateOf(false) }
    var isFetchingMotion by remember { mutableStateOf(false) }
    var isFetchingMuscle by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Hardware Assignment", fontWeight = FontWeight.SemiBold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White
                )
            )
        },
        bottomBar = {
            BottomAppBar(containerColor = MaterialTheme.colorScheme.background) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onCancel) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            val hardwareList = mutableListOf<String>()
                            if (footPressureFetched) hardwareList.add("Foot Pressure (Smart Insole): Peak heel pressure 240kPa, 65% weight bearing on right leg, severe pronation angle (12 deg) during stance phase.")
                            if (motionFetched) hardwareList.add("Gait & Kinematics (IMU Sensor): Right knee max flexion 85 deg (reduced), stride length asymmetry 18%, elevated impact shock 2.4G at heel strike.")
                            if (muscleFetched) hardwareList.add("sEMG Muscle Contraction: Vastus medialis (VMO) activation delayed by 45ms, peak amplitude reduced by 30% compared to hamstring co-contraction.")
                            
                            val hardwareString = if (hardwareList.isNotEmpty()) {
                                hardwareList.joinToString(" | ")
                            } else {
                                "No hardware data gathered."
                            }
                            
                            patient?.let { p ->
                                viewModel.updatePatient(p.copy(hardwareData = hardwareString))
                            }
                            onGenerateReport()
                        }
                    ) {
                        Text("Generate Report")
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Gathering Hardware Data",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                "Please connect the patient to the respective hardware devices and fetch the telemetry data.",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            HardwareCard(
                title = "Foot Pressure Analysis",
                dataSimulated = "Peak heel pressure: 240kPa\nWeight bearing: 65% on right leg\nPronation angle: 12° (Severe)",
                isFetched = footPressureFetched,
                isFetching = isFetchingFootPressure,
                onFetch = {
                    isFetchingFootPressure = true
                    coroutineScope.launch {
                        delay(500)
                        footPressureFetched = true
                        isFetchingFootPressure = false
                    }
                }
            )
            
            HardwareCard(
                title = "Motion & Gait Data",
                dataSimulated = "Right knee max flexion: 85°\nStride length asymmetry: 18%\nImpact shock: 2.4G at heel strike",
                isFetched = motionFetched,
                isFetching = isFetchingMotion,
                onFetch = {
                    isFetchingMotion = true
                    coroutineScope.launch {
                        delay(500)
                        motionFetched = true
                        isFetchingMotion = false
                    }
                }
            )
            
            HardwareCard(
                title = "Muscle Contraction Data",
                dataSimulated = "VMO activation delayed: 45ms\nPeak amplitude: -30% vs baseline\nHigh hamstring co-contraction",
                isFetched = muscleFetched,
                isFetching = isFetchingMuscle,
                onFetch = {
                    isFetchingMuscle = true
                    coroutineScope.launch {
                        delay(500)
                        muscleFetched = true
                        isFetchingMuscle = false
                    }
                }
            )
        }
    }
}

@Composable
fun HardwareCard(
    title: String,
    dataSimulated: String,
    isFetched: Boolean,
    isFetching: Boolean,
    onFetch: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    if (isFetching) {
                        Text("Fetching telemetry...", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                    } else if (isFetched) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = "Done", tint = Color(0xFF4CAF50), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Data Acquired", fontSize = 12.sp, color = Color(0xFF4CAF50))
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, contentDescription = "Pending", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Pending connection", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                
                Button(
                    onClick = onFetch,
                    enabled = !isFetched && !isFetching
                ) {
                    if (isFetching) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                    } else {
                        Text(if (isFetched) "Acquired" else "Fetch Data")
                    }
                }
            }
            if (isFetched) {
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Text(
                        text = dataSimulated,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
