package com.example.ui.components

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import com.example.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageSelectionSection(viewModel: MainViewModel? = null) {
    val currentLang by viewModel?.currentLanguage?.collectAsState() ?: remember { mutableStateOf("EN") }
    val languages = listOf(
        "English" to "en",
        "Hindi" to "hi",
        "Bengali" to "bn",
        "Assamese" to "as",
        "Mizo" to "lus"
    )

    Column(modifier = Modifier.padding(16.dp)) {
        Text("App Language / Quick Switch", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Text("Select preferred language for UI, reports, and AI transcriptions.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(12.dp))
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            languages.forEach { (name, code) ->
                val isSelected = currentLang.equals(code, ignoreCase = true) || 
                                 AppCompatDelegate.getApplicationLocales().toLanguageTags().contains(code)
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags(code)
                        AppCompatDelegate.setApplicationLocales(appLocale)
                        viewModel?.setLanguage(code.uppercase())
                    },
                    label = { Text(name) }
                )
            }
        }
    }
}
