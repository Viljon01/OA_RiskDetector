with open("app/src/main/java/com/example/ui/screens/OrganizationProfileScreen.kt", "r") as f:
    code = f.read()

target = """                item {
                    com.example.ui.components.LanguageSelectionSection(viewModel)
                }"""

replacement = """                item {
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
                }"""

if target in code:
    code = code.replace(target, replacement)
    with open("app/src/main/java/com/example/ui/screens/OrganizationProfileScreen.kt", "w") as f:
        f.write(code)
    print("OrganizationProfileScreen patched with VoiceTranscriptionCard")
else:
    print("Target not found")
