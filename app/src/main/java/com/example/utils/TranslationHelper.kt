package com.example.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.viewmodel.MainViewModel

@Composable
fun MainViewModel.translateAsState(key: String): String {
    val lang by this.currentLanguage.collectAsState()
    return this.translate(key)
}
