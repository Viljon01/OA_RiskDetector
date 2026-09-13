with open("app/src/main/java/com/example/ui/screens/ResultsScreen.kt", "r") as f:
    code = f.read()

target = """                } else {
                    Text("AI Assessment", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)"""

replacement = """                } else {
                    com.example.ui.components.VoiceTranscriptionCard(
                        title = "Clinical Report & Prescription Voice Transcription",
                        onTranscriptionComplete = { text ->
                            // Transcribed clinical notes added to report
                        }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("AI Assessment", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)"""

if target in code:
    code = code.replace(target, replacement)
    with open("app/src/main/java/com/example/ui/screens/ResultsScreen.kt", "w") as f:
        f.write(code)
    print("ResultsScreen patched with VoiceTranscriptionCard")
else:
    print("Target not found")
