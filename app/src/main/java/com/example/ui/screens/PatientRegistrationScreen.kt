package com.example.ui.screens

import com.example.utils.translateAsState

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.PatientEntity
import com.example.viewmodel.MainViewModel
import kotlin.math.roundToInt


@Composable
fun PatientRegistrationScreen(
    viewModel: MainViewModel,
    onProceed: () -> Unit,
    onCancel: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("Male") }
    var occupation by remember { mutableStateOf("Farmer") }
    var heightCm by remember { mutableStateOf("") }
    var weightKg by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var area by remember { mutableStateOf("") }
    
    val bmi = remember(heightCm, weightKg) {
        val h = heightCm.toDoubleOrNull()
        val w = weightKg.toDoubleOrNull()
        if (h != null && w != null && h > 0) {
            val hMeters = h / 100.0
            val b = w / (hMeters * hMeters)
            (b * 10.0).roundToInt() / 10.0
        } else {
            0.0
        }
    }

    Scaffold(
        bottomBar = {
            BottomAppBar(
                containerColor = MaterialTheme.colorScheme.background
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onCancel) {
                        Text(viewModel.translateAsState("cancel"), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Button(
                        onClick = {
                            if (name.isNotBlank() && age.isNotBlank()) {
                                val patient = PatientEntity(
                                    name = name,
                                    age = age,
                                    gender = gender,
                                    occupation = occupation,
                                    phoneNumber = phoneNumber,
                                    area = area,
                                    heightCm = heightCm,
                                    weightKg = weightKg,
                                    bmi = bmi,
                                    riskTier = "PENDING"
                                )
                                viewModel.insertPatient(patient) {
                                    onProceed()
                                }
                            }
                        },
                        enabled = name.isNotBlank() && age.isNotBlank() && heightCm.isNotBlank() && weightKg.isNotBlank()
                    ) {
                        Text(viewModel.translateAsState("proceed_to_screening"))
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
                    viewModel.translateAsState("patient_registration"),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(viewModel.translateAsState("full_name")) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            OutlinedTextField(
                value = age,
                onValueChange = { age = it },
                label = { Text(viewModel.translateAsState("age")) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )
            
            OutlinedTextField(
                value = phoneNumber,
                onValueChange = { phoneNumber = it },
                label = { Text(viewModel.translateAsState("phone_number")) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true
            )
            
            OutlinedTextField(
                value = area,
                onValueChange = { area = it },
                label = { Text(viewModel.translateAsState("area_location")) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Text(viewModel.translateAsState("gender"), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(viewModel.translateAsState("male"), viewModel.translateAsState("female"), viewModel.translateAsState("other")).forEach { g ->
                    FilterChip(
                        selected = gender == g,
                        onClick = { gender = g },
                        label = { Text(g) }
                    )
                }
            }

            Text(viewModel.translateAsState("occupation"), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(viewModel.translateAsState("farmer"), viewModel.translateAsState("laborer"), viewModel.translateAsState("housewife"), viewModel.translateAsState("other")).forEach { occ ->
                    FilterChip(
                        selected = occupation == occ,
                        onClick = { occupation = occ },
                        label = { Text(occ) }
                    )
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = heightCm,
                    onValueChange = { heightCm = it },
                    label = { Text(viewModel.translateAsState("height_cm")) },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                OutlinedTextField(
                    value = weightKg,
                    onValueChange = { weightKg = it },
                    label = { Text(viewModel.translateAsState("weight_kg")) },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            }

            if (bmi > 0) {
                Text(
                    text = "${viewModel.translateAsState("calculated_bmi")}: $bmi",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
