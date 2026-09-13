with open("app/src/main/java/com/example/ui/components/VoiceTranscriptionCard.kt", "r") as f:
    code = f.read()

code = "import androidx.compose.foundation.BorderStroke\n" + code

with open("app/src/main/java/com/example/ui/components/VoiceTranscriptionCard.kt", "w") as f:
    f.write(code)
print("BorderStroke imported")
