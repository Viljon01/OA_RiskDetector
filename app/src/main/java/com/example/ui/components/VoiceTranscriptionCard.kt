package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun VoiceTranscriptionCard(
    title: String = "Voice Transcription & Dictation",
    onTranscriptionComplete: (String) -> Unit
) {
    var isRecording by remember { mutableStateOf(false) }
    var transcribedText by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                        contentDescription = "Mic",
                        tint = if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isRecording) "Listening & Transcribing..." else title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = {
                        if (!isRecording) {
                            isRecording = true
                            transcribedText = ""
                            coroutineScope.launch {
                                delay(3000)
                                isRecording = false
                                val sampleTranscriptions = listOf(
                                    "Patient reports bilateral knee stiffness and moderate joint pain during flexion.",
                                    "Follow-up required in 2 weeks with low-impact physical therapy regimen.",
                                    "Recommended daily warm compresses and prescribed OTC analgesics."
                                )
                                transcribedText = sampleTranscriptions.random()
                                onTranscriptionComplete(transcribedText)
                            }
                        } else {
                            isRecording = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(if (isRecording) "Stop" else "Record Voice")
                }
            }

            AnimatedVisibility(visible = isRecording || transcribedText.isNotBlank()) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(8.dp))
                    if (isRecording) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Audio stream active... Transcribing speech to clinical text.", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                    } else if (transcribedText.isNotBlank()) {
                        Text("Transcribed Note:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(transcribedText, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }
    }
}
