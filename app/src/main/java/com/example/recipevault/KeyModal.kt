package com.example.recipevault

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties

@Composable
fun ApiKeyEntryModal(
    showDialog: Boolean,
    onDismissRequest: () -> Unit,
    onApiKeySaved: (String) -> Unit // Callback when API key is successfully saved
) {
    val context = LocalContext.current
    var tempApiKey by remember { mutableStateOf(PrefsManager.getApiKey(context) ?: "") }
    var errorText by remember { mutableStateOf<String?>(null) }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = {
                // Reset tempApiKey if user dismisses without saving,
                // or keep it if you want it to persist across dialog openings
                // tempApiKey = PrefsManager.getApiKey(context) ?: "" // Optional: Reset on dismiss
                errorText = null // Clear error on dismiss
                onDismissRequest()
            },
            title = { Text("Enter API Key") },
            text = {
                Column {
                    OutlinedTextField(
                        value = tempApiKey,
                        onValueChange = {
                            tempApiKey = it
                            if (errorText != null) errorText = null // Clear error when user types
                        },
                        label = { Text("API Key") },
                        singleLine = true,
                        isError = errorText != null,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (errorText != null) {
                        Text(
                            text = errorText!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (tempApiKey.isNotBlank()) {
                            PrefsManager.saveApiKey(context, tempApiKey)
                            Log.d("ApiKeyModal", "API Key saved: $tempApiKey")
                            onApiKeySaved(tempApiKey) // Notify caller
                            errorText = null
                            onDismissRequest() // Close the dialog
                        } else {
                            errorText = "API Key cannot be empty"
                        }
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        // tempApiKey = PrefsManager.getApiKey(context) ?: "" // Optional: Reset on dismiss
                        errorText = null
                        onDismissRequest()
                    }
                ) {
                    Text("Cancel")
                }
            },
            properties = DialogProperties(dismissOnClickOutside = false) // Prevent closing by clicking outside
        )
    }
}

// --- Example Usage ---
@Composable
fun SettingsScreen() { // Or any screen where you want to trigger this
    val context = LocalContext.current
    var showApiKeyDialog by remember { mutableStateOf(false) }
    // To display the saved API key (optional)
    var currentApiKey by remember { mutableStateOf(PrefsManager.getApiKey(context)) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Current API Key: ${currentApiKey ?: "Not set"}")
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { showApiKeyDialog = true }) {
            Text("Set API Key")
        }
    }

    ApiKeyEntryModal(
        showDialog = showApiKeyDialog,
        onDismissRequest = { showApiKeyDialog = false },
        onApiKeySaved = { savedKey ->
            currentApiKey = savedKey // Update the displayed key
            // You might want to trigger other actions here, like re-fetching data
            showApiKeyDialog = false
            Log.d("SettingsScreen", "API Key dialog confirmed and dismissed.")
        }
    )
}