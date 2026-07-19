package com.example.ghostride.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.ghostride.GhostRideDatabase
import com.example.ghostride.Profile
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val database = remember { GhostRideDatabase.getInstance(context) }
    val coroutineScope = rememberCoroutineScope()

    var name by remember { mutableStateOf("") }
    var lastSavedName by remember { mutableStateOf<String?>(null) }
    var showSavedMessage by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val existingProfile = database.profileDao().getProfile()
        if (existingProfile != null) {
            name = existingProfile.name
            lastSavedName = existingProfile.name
        }
    }

    LaunchedEffect(showSavedMessage) {
        if (showSavedMessage) {
            delay(4000)
            showSavedMessage = false
        }
    }

    val hasUnsavedChange = name.isNotBlank() && name != lastSavedName

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 24.dp)
    ) {
        TextButton(onClick = onBack) {
            Text(
                text = "← Back to Setup",
                style = MaterialTheme.typography.labelLarge
            )
        }

        Text(
            text = "Profile",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(top = 8.dp)
        )

        Text(
            text = "This is your personal profile for GhostRide.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            modifier = Modifier.padding(top = 6.dp, bottom = 20.dp)
        )

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Your name") },
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {
                coroutineScope.launch {
                    database.profileDao().saveProfile(Profile(name = name.trim()))
                    lastSavedName = name.trim()
                    showSavedMessage = true
                }
            },
            enabled = hasUnsavedChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
        ) {
            Text("Save")
        }

        if (showSavedMessage) {
            Text(
                text = "Saved.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 12.dp)
            )
        }
    }
}