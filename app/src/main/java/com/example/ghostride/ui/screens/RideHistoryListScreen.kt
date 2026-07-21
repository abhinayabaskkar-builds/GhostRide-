package com.example.ghostride.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.ghostride.GeocodingApiClient
import com.example.ghostride.GhostRideDatabase
import com.example.ghostride.Ride
import com.example.ghostride.RideStatus
import com.example.ghostride.RideTag
import com.example.ghostride.ui.theme.GhostRideGray
import com.example.ghostride.ui.theme.GhostRideGreenDark
import com.example.ghostride.ui.theme.GhostRideGreenLight
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val timeFormatter = DateTimeFormatter.ofPattern("h:mm a")

private fun RideTag.color(): Color = when (this) {
    RideTag.OFFICE_COMMUTE -> GhostRideGreenLight
    RideTag.HOME -> GhostRideGreenDark
    RideTag.OTHER -> GhostRideGray
    RideTag.UNCLASSIFIED -> GhostRideGray
}

private fun RideTag.label(): String = when (this) {
    RideTag.OFFICE_COMMUTE -> "Office Commute"
    RideTag.HOME -> "Home"
    RideTag.OTHER -> "Other"
    RideTag.UNCLASSIFIED -> "Unclassified"
}

/** Groups a ride into "Today", "Yesterday", "This Week", or a month like "July 2026". */
private fun groupLabel(boardingTime: Long): String {
    val zone = ZoneId.systemDefault()
    val rideDate = Instant.ofEpochMilli(boardingTime).atZone(zone).toLocalDate()
    val today = LocalDate.now(zone)

    return when {
        rideDate == today -> "Today"
        rideDate == today.minusDays(1) -> "Yesterday"
        rideDate.isAfter(today.minusDays(7)) -> "This Week"
        else -> {
            val month = rideDate.month.getDisplayName(TextStyle.FULL, Locale.getDefault())
            "$month ${rideDate.year}"
        }
    }
}

private fun formatTime(epochMillis: Long?): String {
    if (epochMillis == null) return "--"
    val zone = ZoneId.systemDefault()
    return Instant.ofEpochMilli(epochMillis).atZone(zone).format(timeFormatter)
}

private fun formatDuration(durationSeconds: Long?): String {
    if (durationSeconds == null) return "--"
    val minutes = durationSeconds / 60
    return "$minutes min"
}

private fun formatDistance(distanceMeters: Double?): String {
    if (distanceMeters == null) return "--"
    val km = distanceMeters / 1000.0
    return "%.1f km".format(km)
}

@Composable
fun RideHistoryListScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    onRideClick: (String) -> Unit
) {
    val context = LocalContext.current
    val database = remember { GhostRideDatabase.getInstance(context) }

    var rides by remember { mutableStateOf<List<Ride>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        val allRides = database.rideDao().getAllRides()
        rides = allRides.filter { it.rideStatus == RideStatus.COMPLETED }
        isLoading = false
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 24.dp)
    ) {
        TextButton(onClick = onBack) {
            Text(
                text = "← Back to Setup",
                style = MaterialTheme.typography.labelLarge
            )
        }

        Text(
            text = "Ride History",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
        )

        when {
            isLoading -> {
                // Nothing to show yet; avoids flashing an empty-state message
                // for a split second while the database loads.
            }
            rides.isEmpty() -> {
                Text(
                    text = "No rides recorded yet. Once GhostRide detects a commute, it'll show up here.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
            else -> {
                val grouped = rides.groupBy { groupLabel(it.boardingTime) }

                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    grouped.forEach { (label, ridesInGroup) ->
                        item {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                            )
                        }
                        items(ridesInGroup) { ride ->
                            RideCard(ride, onClick = { onRideClick(ride.id) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RideCard(ride: Ride, onClick: () -> Unit) {
    val needsAddress = ride.rideTag == RideTag.OTHER || ride.rideTag == RideTag.UNCLASSIFIED

    var boardingAddress by remember(ride.id) { mutableStateOf<String?>(null) }
    var arrivalAddress by remember(ride.id) { mutableStateOf<String?>(null) }

    LaunchedEffect(ride.id) {
        if (needsAddress) {
            if (ride.boardingLatitude != null && ride.boardingLongitude != null) {
                boardingAddress = GeocodingApiClient.reverseGeocode(
                    ride.boardingLatitude,
                    ride.boardingLongitude
                )
            }
            if (ride.arrivalLatitude != null && ride.arrivalLongitude != null) {
                arrivalAddress = GeocodingApiClient.reverseGeocode(
                    ride.arrivalLatitude,
                    ride.arrivalLongitude
                )
            }
        }
    }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            TagBadge(ride.rideTag)

            Text(
                text = "${ride.driverNameSnapshot} · ${ride.vehicleNameSnapshot}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 10.dp)
            )

            if (needsAddress && (boardingAddress != null || arrivalAddress != null)) {
                Text(
                    text = "${boardingAddress ?: "Unknown"} → ${arrivalAddress ?: "Unknown"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    modifier = Modifier.padding(top = 6.dp)
                )
            }

            Text(
                text = "${formatTime(ride.boardingTime)} – ${formatTime(ride.arrivalTime)} " +
                        "(${formatDuration(ride.durationSeconds)}) · ${formatDistance(ride.distanceMeters)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}

@Composable
private fun TagBadge(tag: RideTag) {
    Surface(
        shape = RoundedCornerShape(50),
        color = tag.color().copy(alpha = 0.2f)
    ) {
        Text(
            text = tag.label(),
            style = MaterialTheme.typography.labelMedium,
            color = tag.color(),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
        )
    }
}