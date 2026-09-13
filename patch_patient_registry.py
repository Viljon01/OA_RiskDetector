with open("app/src/main/java/com/example/ui/screens/PatientRegistryScreen.kt", "r") as f:
    code = f.read()

target = """            OutlinedTextField(
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
            )"""

replacement = target + """
            Spacer(modifier = Modifier.height(8.dp))
            com.example.ui.components.VoiceTranscriptionCard(
                title = "Voice Patient Search & Transcription",
                onTranscriptionComplete = { text ->
                    searchQuery = text.take(20)
                }
            )
            Spacer(modifier = Modifier.height(16.dp))"""

if target in code:
    code = code.replace(target, replacement)
    with open("app/src/main/java/com/example/ui/screens/PatientRegistryScreen.kt", "w") as f:
        f.write(code)
    print("PatientRegistryScreen patched with VoiceTranscriptionCard")
else:
    print("Target not found")
