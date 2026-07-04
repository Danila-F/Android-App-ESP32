package com.danilaf.esp32controller.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen(
    language: String,
    versionName: String,
    versionCode: Int,
    repositoryUrl: String,
    onLanguageChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uriHandler = LocalUriHandler.current

    Column(modifier = modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Settings", style = MaterialTheme.typography.headlineSmall)

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Localization", style = MaterialTheme.typography.titleMedium)
                Text("Current language: ${language.uppercase()}")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { onLanguageChange("en") }, enabled = language != "en") {
                        Text("English")
                    }
                }
                Text("Only English is available for now. More languages can be added through Android string resources later.", style = MaterialTheme.typography.bodySmall)
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("About", style = MaterialTheme.typography.titleMedium)
                Text("ESP32 Controller is a local Wi-Fi controller for ESP32 devices running the companion HTTP API firmware.")
                Text("Version: $versionName")
                Text("Version code: $versionCode")
                OutlinedButton(onClick = { uriHandler.openUri(repositoryUrl) }) {
                    Text("Open GitHub repository")
                }
            }
        }
    }
}
