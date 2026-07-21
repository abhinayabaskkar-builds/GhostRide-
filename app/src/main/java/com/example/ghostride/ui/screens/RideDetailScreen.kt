package com.example.ghostride.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import com.example.ghostride.GeocodingApiClient
import com.example.ghostride.GhostRideDatabase
import com.example.ghostride.Ride
import com.example.ghostride.RideTag
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val dateFormatter = DateTimeFormatter.ofPattern("EEE, MMM d yyyy")
private val timeFormatterDetail = DateTimeFormatter.ofPattern("h:mm a")

private fun formatDate(epochMillis: Long?): String {
    if (epochMillis == null) return "--"
    return Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).format(dateFormatter)
}

private fun formatTimeDetail(epochMillis: Long?): String {
    if (epochMillis == null) return "--"
    return Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).format(timeFormatterDetail)
}

@Composable
fun RideDetailScreen(
    modifier: Modifier = Modifier,
    rideId: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val database = remember { GhostRideDatabase.getInstance(context) }
    val coroutineScope = rememberCoroutineScope()

    var ride by remember { mutableStateOf<Ride?>(null) }
    var boardingAddress by remember { mutableStateOf<String?>(null) }
    var arrivalAddress by remember { mutableStateOf<String?>(null) }
    var showTagDropdown by remember { mutableStateOf(false) }

    LaunchedEffect(rideId) {
        val loadedRide = database.rideDao().getRideById(rideId)
        ride = loadedRide

        if (loadedRide?.boardingLatitude != null && loadedRide.boardingLongitude != null) {
            boardingAddress = GeocodingApiClient.reverseGeocode(
                loadedRide.boardingLatitude,
                loadedRide.boardingLongitude
            )
        }
        if (loadedRide?.arrivalLatitude != null && loadedRide.arrivalLongitude != null) {
            arrivalAddress = GeocodingApiClient.reverseGeocode(
                loadedRide.arrivalLatitude,
                loadedRide.arrivalLongitude
            )
        }
    }

    val currentRide = ride

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 24.dp)
    ) {
        TextButton(onClick = onBack) {
            Text(
                text = "← Back to Ride History",
                style = MaterialTheme.typography.labelLarge
            )
        }

        if (currentRide == null) {
            Text(
                text = "Loading ride...",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 16.dp)
            )
            return@Column
        }

        Text(
            text = formatDate(currentRide.boardingTime),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
        )

        // Tag badge — tappable only while Unclassified, to trigger re-tagging.
        Column {
            TagBadgeTappable(
                tag = currentRide.rideTag,
                onClick = {
                    if (currentRide.rideTag == RideTag.UNCLASSIFIED) {
                        showTagDropdown = true
                    }
                }
            )
            DropdownMenu(
                expanded = showTagDropdown,
                onDismissRequest = { showTagDropdown = false }
            ) {
                listOf(RideTag.OFFICE_COMMUTE, RideTag.HOME, RideTag.OTHER).forEach { option ->
                    DropdownMenuItem(
                        text = { Text(optionLabel(option)) },
                        onClick = {
                            showTagDropdown = false
                            coroutineScope.launch {
                                val updatedRide = currentRide.copy(rideTag = option)
                                database.rideDao().updateRide(updatedRide)
                                ride = updatedRide
                            }
                        }
                    )
                }
            }
        }

        DetailRow("Driver", currentRide.driverNameSnapshot)
        DetailRow("Vehicle", currentRide.vehicleNameSnapshot)
        DetailRow("Boarding Time", formatTimeDetail(currentRide.boardingTime))
        DetailRow("Boarding Location", boardingAddress ?: "Unknown")
        DetailRow("Arrival Time", formatTimeDetail(currentRide.arrivalTime))
        DetailRow("Arrival Location", arrivalAddress ?: "Unknown")
        DetailRow(
            "Duration",
            currentRide.durationSeconds?.let { "${it / 60} min" } ?: "--"
        )
        DetailRow(
            "Distance",
            currentRide.distanceMeters?.let { "%.1f km".format(it / 1000.0) } ?: "--"
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column(modifier = Modifier.padding(top = 16.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

private fun optionLabel(tag: RideTag): String = when (tag) {
    RideTag.OFFICE_COMMUTE -> "Office Commute"
    RideTag.HOME -> "Home"
    RideTag.OTHER -> "Other"
    RideTag.UNCLASSIFIED -> "Unclassified"
}

@Composable
private fun TagBadgeTappable(tag: RideTag, onClick: () -> Unit) {
    val color = when (tag) {
        RideTag.OFFICE_COMMUTE -> com.example.ghostride.ui.theme.GhostRideGreenLight
        RideTag.HOME -> com.example.ghostride.ui.theme.GhostRideGreenDark
        RideTag.OTHER -> com.example.ghostride.ui.theme.GhostRideGray
        RideTag.UNCLASSIFIED -> com.example.ghostride.ui.theme.GhostRideGray
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = color.copy(alpha = 0.2f)
    ) {
        Text(
            text = optionLabel(tag),
            style = MaterialTheme.typography.labelMedium,
            color = color,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
        )
    }
}