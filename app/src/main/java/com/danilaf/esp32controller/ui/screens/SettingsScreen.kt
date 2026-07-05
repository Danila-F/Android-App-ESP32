package com.danilaf.esp32controller.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
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

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Text("Settings", style = MaterialTheme.typography.headlineSmall) }

        item {
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
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("App updates", style = MaterialTheme.typography.titleMedium)
                    Text("Installed version: $versionName")
                    Text("Version code: $versionCode")
                    Text("Update channel: signed GitHub Actions APK")
                    Text("New APK artifacts can be installed over the existing app when they are signed with the same CI keystore and have a higher version code.", style = MaterialTheme.typography.bodySmall)
                    Text("Saved devices and settings are stored in app data and are preserved during normal APK updates.", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("About", style = MaterialTheme.typography.titleMedium)
                    Text("ESP32 Controller is a local Wi-Fi controller for ESP32 devices running the companion HTTP API firmware.")
                    OutlinedButton(onClick = { uriHandler.openUri(repositoryUrl) }) {
                        Text("Open GitHub repository")
                    }
                }
            }
        }
    }
}
