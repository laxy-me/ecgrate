package com.laxy.ecgrate.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF1565C0),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF42A5F5),
    secondary = Color(0xFF0288D1),
    surface = Color(0xFFF5F5F5),
    background = Color(0xFFF5F5F5),
)

@Composable
fun EcgrateTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = LightColors, content = content)
}
