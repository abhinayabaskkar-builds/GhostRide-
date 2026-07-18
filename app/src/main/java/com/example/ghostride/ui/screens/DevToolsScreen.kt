package com.example.ghostride.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun DevToolsScreen(
    modifier: Modifier = Modifier,
    onSimulateConnect: () -> Unit,
    onSimulateDisconnect: () -> Unit,
    onCheckSpeed: () -> Unit,
    onStartActivityUpdates: () -> Unit,
    onCreateTestActiveRide: () -> Unit,
    onRequestBatteryExemption: () -> Unit,
    onRunSilentFailureCheck: () -> Unit
) {
    Column(modifier = modifier.verticalScroll(rememberScrollState())) {
        Text(text = "GhostRide — Developer Tools")
        Button(onClick = onSimulateConnect) {
            Text("Simulate Vehicle Connect")
        }
        Button(onClick = onSimulateDisconnect) {
            Text("Simulate Vehicle Disconnect")
        }
        Button(onClick = onCheckSpeed) {
            Text("Check GPS Speed")
        }
        Button(onClick = onStartActivityUpdates) {
            Text("Start Activity Recognition")
        }
        Button(onClick = onCreateTestActiveRide) {
            Text("Create Test Active Ride")
        }
        Button(onClick = onRequestBatteryExemption) {
            Text("Request Battery Optimization Exemption")
        }
        Button(onClick = onRunSilentFailureCheck) {
            Text("Run Silent Failure Check Now")
        }
    }
}