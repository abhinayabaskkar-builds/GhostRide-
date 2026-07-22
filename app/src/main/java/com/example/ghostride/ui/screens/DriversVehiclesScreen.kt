package com.example.ghostride.ui.screens

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.ghostride.Driver
import com.example.ghostride.GhostRideDatabase
import com.example.ghostride.Vehicle
import kotlinx.coroutines.launch

private data class DriverWithVehicle(
    val driver: Driver,
    val vehicle: Vehicle?
)

private fun hasBluetoothConnectPermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context, Manifest.permission.BLUETOOTH_CONNECT
    ) == PackageManager.PERMISSION_GRANTED
}

private fun getPairedDevices(context: Context): List<BluetoothDevice> {
    if (!hasBluetoothConnectPermission(context)) return emptyList()
    val bluetoothManager = context.getSystemService(BluetoothManager::class.java)
    val adapter = bluetoothManager?.adapter ?: return emptyList()
    return try {
        adapter.bondedDevices?.toList() ?: emptyList()
    } catch (e: SecurityException) {
        emptyList()
    }
}

@Composable
fun DriversVehiclesScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val database = remember { GhostRideDatabase.getInstance(context) }
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    var driversWithVehicles by remember { mutableStateOf<List<DriverWithVehicle>>(emptyList()) }
    var showAddForm by remember { mutableStateOf(false) }
    var editingDriverId by remember { mutableStateOf<String?>(null) }
    var entryPendingDelete by remember { mutableStateOf<DriverWithVehicle?>(null) }

    var driverName by remember { mutableStateOf("") }
    var vehicleName by remember { mutableStateOf("") }
    var selectedDevice by remember { mutableStateOf<BluetoothDevice?>(null) }
    var manualMacAddress by remember { mutableStateOf("") }
    var pairedDevices by remember { mutableStateOf<List<BluetoothDevice>>(emptyList()) }
    var permissionMissing by remember { mutableStateOf(false) }

    suspend fun refreshDrivers() {
        val drivers = database.driverDao().getAllDrivers()
        val vehicles = database.vehicleDao().getAllVehicles()
        driversWithVehicles = drivers.map { driver ->
            DriverWithVehicle(driver, vehicles.find { it.driverId == driver.id })
        }
    }

    fun refreshPairedDevices() {
        permissionMissing = !hasBluetoothConnectPermission(context)
        pairedDevices = getPairedDevices(context)
    }

    LaunchedEffect(Unit) {
        refreshDrivers()
    }

    LaunchedEffect(showAddForm, editingDriverId) {
        if (showAddForm || editingDriverId != null) {
            refreshPairedDevices()
        }
    }

    fun resetForm() {
        driverName = ""
        vehicleName = ""
        selectedDevice = null
        manualMacAddress = ""
        showAddForm = false
        editingDriverId = null
        focusManager.clearFocus()
    }

    fun openAddForm() {
        resetForm()
        showAddForm = true
    }

    fun openEditForm(entry: DriverWithVehicle) {
        resetForm()
        editingDriverId = entry.driver.id
        driverName = entry.driver.name
        vehicleName = entry.vehicle?.name ?: ""
        manualMacAddress = entry.vehicle?.bluetoothMac ?: ""
    }

    fun saveForm(existingDriver: Driver?, existingVehicle: Vehicle?) {
        val macAddress = selectedDevice?.address ?: manualMacAddress.trim()
        if (driverName.isBlank() || vehicleName.isBlank() || macAddress.isBlank()) return

        coroutineScope.launch {
            if (existingDriver == null) {
                val newDriver = Driver(name = driverName.trim())
                database.driverDao().insertDriver(newDriver)
                val newVehicle = Vehicle(
                    name = vehicleName.trim(),
                    bluetoothMac = macAddress,
                    driverId = newDriver.id
                )
                database.vehicleDao().insertVehicle(newVehicle)
            } else {
                val updatedDriver = existingDriver.copy(name = driverName.trim())
                database.driverDao().updateDriver(updatedDriver)

                if (existingVehicle != null) {
                    database.vehicleDao().updateVehicle(
                        existingVehicle.copy(name = vehicleName.trim(), bluetoothMac = macAddress)
                    )
                } else {
                    database.vehicleDao().insertVehicle(
                        Vehicle(name = vehicleName.trim(), bluetoothMac = macAddress, driverId = updatedDriver.id)
                    )
                }
            }
            refreshDrivers()
            resetForm()
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 24.dp)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { focusManager.clearFocus() }
    ) {
        TextButton(onClick = onBack) {
            Text(
                text = "← Back to Setup",
                style = MaterialTheme.typography.labelLarge
            )
        }

        Text(
            text = "Drivers & Vehicles",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(top = 8.dp)
        )

        Text(
            text = "Add up to 2 drivers, each linked to one vehicle.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            modifier = Modifier.padding(top = 6.dp, bottom = 20.dp)
        )

        driversWithVehicles.forEach { entry ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = entry.driver.name,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = entry.vehicle?.name ?: "No vehicle linked",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.padding(top = 2.dp)
                    )
                    if (entry.vehicle != null) {
                        Text(
                            text = entry.vehicle.bluetoothMac,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { openEditForm(entry) }) {
                            Text("Edit")
                        }
                        TextButton(onClick = { entryPendingDelete = entry }) {
                            Text("Delete", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            // Inline edit form — appears directly under the card being edited,
            // instead of a separate block elsewhere on the screen.
            if (editingDriverId == entry.driver.id) {
                DriverForm(
                    formTitle = "Edit Driver",
                    driverName = driverName,
                    onDriverNameChange = { driverName = it },
                    vehicleName = vehicleName,
                    onVehicleNameChange = { vehicleName = it },
                    manualMacAddress = manualMacAddress,
                    onManualMacChange = { manualMacAddress = it },
                    selectedDevice = selectedDevice,
                    onDeviceSelected = { selectedDevice = it },
                    pairedDevices = pairedDevices,
                    permissionMissing = permissionMissing,
                    onRetry = { refreshPairedDevices() },
                    onCancel = { resetForm() },
                    onSave = { saveForm(entry.driver, entry.vehicle) },
                    saveLabel = "Save Changes",
                    focusManager = focusManager
                )
            }
        }

        if (showAddForm) {
            DriverForm(
                formTitle = "New Driver",
                driverName = driverName,
                onDriverNameChange = { driverName = it },
                vehicleName = vehicleName,
                onVehicleNameChange = { vehicleName = it },
                manualMacAddress = manualMacAddress,
                onManualMacChange = { manualMacAddress = it },
                selectedDevice = selectedDevice,
                onDeviceSelected = { selectedDevice = it },
                pairedDevices = pairedDevices,
                permissionMissing = permissionMissing,
                onRetry = { refreshPairedDevices() },
                onCancel = { resetForm() },
                onSave = { saveForm(null, null) },
                saveLabel = "Save Driver",
                focusManager = focusManager
            )
        } else if (editingDriverId == null) {
            Button(
                onClick = { openAddForm() },
                enabled = driversWithVehicles.size < 2,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    if (driversWithVehicles.size < 2) "+ Add Driver"
                    else "Maximum 2 drivers reached"
                )
            }
        }

        if (driversWithVehicles.isEmpty()) {
            Text(
                text = "At least 1 driver with a paired vehicle is required to complete setup.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }

    val entryToDelete = entryPendingDelete
    if (entryToDelete != null) {
        AlertDialog(
            onDismissRequest = { entryPendingDelete = null },
            title = { Text("Delete ${entryToDelete.driver.name}?") },
            text = {
                Text(
                    "This will remove ${entryToDelete.driver.name}" +
                            (entryToDelete.vehicle?.let { " and ${it.name}" } ?: "") +
                            ". This can't be undone. Past rides already recorded won't be affected."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    coroutineScope.launch {
                        database.driverDao().deleteDriver(entryToDelete.driver)
                        entryToDelete.vehicle?.let { database.vehicleDao().deleteVehicle(it) }
                        refreshDrivers()
                        entryPendingDelete = null
                    }
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { entryPendingDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun DriverForm(
    formTitle: String,
    driverName: String,
    onDriverNameChange: (String) -> Unit,
    vehicleName: String,
    onVehicleNameChange: (String) -> Unit,
    manualMacAddress: String,
    onManualMacChange: (String) -> Unit,
    selectedDevice: BluetoothDevice?,
    onDeviceSelected: (BluetoothDevice) -> Unit,
    pairedDevices: List<BluetoothDevice>,
    permissionMissing: Boolean,
    onRetry: () -> Unit,
    onCancel: () -> Unit,
    onSave: () -> Unit,
    saveLabel: String,
    focusManager: androidx.compose.ui.focus.FocusManager
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = formTitle,
                style = MaterialTheme.typography.titleMedium
            )

            OutlinedTextField(
                value = driverName,
                onValueChange = onDriverNameChange,
                label = { Text("Driver name") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
            )

            Text(
                text = "Select vehicle's paired Bluetooth device",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 20.dp, bottom = 4.dp)
            )

            when {
                permissionMissing -> {
                    Text(
                        text = "Bluetooth permission not granted. Enable it in phone settings, then retry.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
                pairedDevices.isEmpty() -> {
                    Text(
                        text = "No paired devices found. Pair the vehicle's Bluetooth in your phone's Bluetooth settings first, then retry.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
                else -> {
                    pairedDevices.forEach { device ->
                        val deviceName = try {
                            device.name ?: device.address
                        } catch (e: SecurityException) {
                            device.address
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onDeviceSelected(device) }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedDevice?.address == device.address,
                                onClick = { onDeviceSelected(device) }
                            )
                            Column {
                                Text(
                                    text = deviceName,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = device.address,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                }
            }

            TextButton(
                onClick = onRetry,
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Text("Retry", style = MaterialTheme.typography.labelLarge)
            }

            OutlinedTextField(
                value = manualMacAddress,
                onValueChange = onManualMacChange,
                label = { Text("Or enter MAC address manually (testing only)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
            )

            OutlinedTextField(
                value = vehicleName,
                onValueChange = onVehicleNameChange,
                label = { Text("Vehicle name") },
                enabled = selectedDevice != null || manualMacAddress.isNotBlank(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onCancel) {
                    Text("Cancel")
                }
                Button(
                    onClick = onSave,
                    enabled = driverName.isNotBlank() && vehicleName.isNotBlank() &&
                            (selectedDevice != null || manualMacAddress.isNotBlank()),
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text(saveLabel)
                }
            }
        }
    }
}