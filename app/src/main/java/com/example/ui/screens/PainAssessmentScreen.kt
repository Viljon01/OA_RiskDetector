package com.example.ui.screens

import com.example.utils.translateAsState

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.viewmodel.MainViewModel


@Composable
fun PainAssessmentScreen(
    viewModel: MainViewModel,
    onGenerateReport: () -> Unit,
    onCancel: () -> Unit
) {
    val patient by viewModel.currentPatient.collectAsState()
    
    var painLevel by remember { mutableStateOf(patient?.painLevel?.toFloat() ?: 0f) }
    var stiffness by remember { mutableStateOf(patient?.stiffness ?: "None") }

    val history by viewModel.patientHistory.collectAsState()
        Scaffold(
        bottomBar = {
            BottomAppBar(containerColor = MaterialTheme.colorScheme.background) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onCancel) {
                        Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Button(
                        onClick = {
                            patient?.let {
                                // Dummy risk score calculation
                                val score = (painLevel * 10) + (if (stiffness == "> 30 mins") 20 else 0)
                                val tier = when {
                                    score > 70 -> "HIGH RISK"
                                    score > 40 -> "MODERATE RISK"
                                    else -> "LOW RISK"
                                }
                                val updated = it.copy(
                                    painLevel = painLevel.toInt(),
                                    stiffness = stiffness,
                                    riskScore = score.toDouble(),
                                    riskTier = tier
                                )
                                viewModel.updatePatient(updated)
                                onGenerateReport()
                            }
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
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                coil.compose.AsyncImage(
                    model = com.example.R.drawable.custom_logo,
                    contentDescription = "Logo",
                    modifier = Modifier.size(32.dp),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                            filterQuality = androidx.compose.ui.graphics.FilterQuality.High
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "Pain & Symptom Assessment",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            Column {
                Text("Current Pain Level (0-10)", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.height(8.dp))
                Slider(
                    value = painLevel,
                    onValueChange = { painLevel = it },
                    valueRange = 0f..10f,
                    steps = 9
                )
                Text(
                    text = "Score: ${painLevel.toInt()}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Column {
                Text("Morning Joint Stiffness", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("None", "< 30 mins", "> 30 mins").forEach { opt ->
                        FilterChip(
                            selected = stiffness == opt,
                            onClick = { stiffness = opt },
                            label = { Text(opt) }
                        )
                    }
                }
            }
            
            // Note: Checkboxes for Anatomical Distribution omitted for brevity in prototype, 
            // but structure is clear.
        }
    }
}
