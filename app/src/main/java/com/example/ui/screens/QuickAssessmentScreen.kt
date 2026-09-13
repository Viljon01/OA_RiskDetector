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
import androidx.compose.ui.platform.LocalContext
import android.app.Activity
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import com.example.viewmodel.MainViewModel

data class AssessmentQuestion(
    val id: Int,
    val textKey: String,
    val optionsKeys: List<String>
)

@OptIn(ExperimentalMaterial3Api::class)

@Composable
fun QuickAssessmentScreen(
    viewModel: MainViewModel,
    onProceed: () -> Unit,
    onCancel: () -> Unit
) {
    val history by viewModel.patientHistory.collectAsState()
        val questions = listOf(
        AssessmentQuestion(1, "q1_text", listOf("opt_yes", "opt_no")),
        AssessmentQuestion(2, "q2_text", listOf("opt_knee", "opt_hip", "opt_ankle", "opt_other")),
        AssessmentQuestion(3, "q3_text", emptyList()), // 0-10 scale
        AssessmentQuestion(4, "q4_text", listOf("opt_never", "opt_sometimes", "opt_often")),
        AssessmentQuestion(5, "q5_text", listOf("opt_no", "opt_mild", "opt_moderate", "opt_severe")),
        AssessmentQuestion(6, "q6_text", listOf("opt_no", "opt_sometimes", "opt_yes")),
        AssessmentQuestion(7, "q7_text", listOf("opt_never", "opt_sometimes", "opt_often")),
        AssessmentQuestion(8, "q8_text", listOf("opt_no", "opt_yes")),
        AssessmentQuestion(9, "q9_text", listOf("opt_never", "opt_sometimes", "opt_often")),
        AssessmentQuestion(10, "q10_text", listOf("opt_no", "opt_sometimes", "opt_often")),
        AssessmentQuestion(11, "q11_text", listOf("opt_no", "opt_slightly", "opt_very")),
        AssessmentQuestion(12, "q12_text", listOf("opt_no", "opt_sometimes", "opt_yes")),
        AssessmentQuestion(13, "q13_text", listOf("opt_yes", "opt_no")),
        AssessmentQuestion(14, "q14_text", listOf("opt_yes", "opt_no", "opt_not_sure")),
        AssessmentQuestion(15, "q15_text", listOf("opt_no", "opt_sometimes"))
    )

    val currentPatient by viewModel.currentPatient.collectAsState()
    val answers = remember { mutableStateMapOf<Int, String>() }
    var painScale by remember { mutableStateOf(0f) }

    LaunchedEffect(currentPatient) {
        currentPatient?.quickAssessmentData?.let { data ->
            if (data.isNotEmpty()) {
                try {
                    val json = org.json.JSONObject(data)
                    questions.forEach { q ->
                        if (q.id != 3 && json.has(q.id.toString())) {
                            answers[q.id] = json.getString(q.id.toString())
                        }
                    }
                    if (json.has("painScale")) {
                        painScale = json.getDouble("painScale").toFloat()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    val allAnswered = questions.all { q ->
        if (q.id == 3) true else answers.containsKey(q.id)
    }

    fun saveProgress() {
        val answersJson = org.json.JSONObject()
        answers.forEach { (k, v) -> answersJson.put(k.toString(), v) }
        answersJson.put("painScale", painScale.toDouble())
        currentPatient?.let { p ->
            viewModel.updatePatient(p.copy(
                quickAssessmentData = answersJson.toString(),
                painLevel = painScale.toInt()
            ))
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            saveProgress()
        }
    }

    Scaffold(
        bottomBar = {
            BottomAppBar(containerColor = MaterialTheme.colorScheme.background) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = {
                        saveProgress()
                        onCancel()
                    }) {
                        Text(viewModel.translateAsState("cancel"), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Button(
                        onClick = {
                            saveProgress()
                            onProceed()
                        },
                        enabled = allAnswered
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
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
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
                        viewModel.translateAsState("assessment_title"),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                
                // Language Selector
                var expanded by remember { mutableStateOf(false) }
                val currentLanguage by viewModel.currentLanguage.collectAsState()
                Box {
                    IconButton(onClick = { expanded = true }) {
                        Icon(imageVector = androidx.compose.material.icons.Icons.Default.Language, contentDescription = "Change Language")
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        com.example.utils.Language.values().forEach { lang ->
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        RadioButton(
                                            selected = (currentLanguage == lang.code),
                                            onClick = null
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("${lang.nativeName} (${lang.code})")
                                    }
                                },
                                onClick = {
                                    viewModel.setLanguage(lang.code)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
            
            questions.forEach { q ->
                Column {
                    Text(viewModel.translateAsState(q.textKey), style = MaterialTheme.typography.bodyMedium)
                    if (q.id == 3) {
                        // Pain Scale Slider
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text("0", modifier = Modifier.padding(end = 8.dp))
                            Slider(
                                value = painScale,
                                onValueChange = { painScale = it },
                                valueRange = 0f..10f,
                                steps = 9,
                                modifier = Modifier.weight(1f)
                            )
                            Text("10", modifier = Modifier.padding(start = 8.dp))
                        }
                        Text("Selected: ${painScale.toInt()}", style = MaterialTheme.typography.labelSmall)
                    } else {
                        // Options
                        @OptIn(ExperimentalLayoutApi::class)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(top = 8.dp).fillMaxWidth()
                        ) {
                            q.optionsKeys.forEach { optKey ->
                                FilterChip(
                                    selected = answers[q.id] == optKey,
                                    onClick = { answers[q.id] = optKey },
                                    label = { Text(viewModel.translateAsState(optKey)) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
