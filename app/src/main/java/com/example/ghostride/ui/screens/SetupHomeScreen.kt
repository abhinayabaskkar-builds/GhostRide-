package com.example.ghostride.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.ghostride.GhostRideDatabase
import com.example.ghostride.ui.theme.GhostRideAmber
import com.example.ghostride.ui.theme.GhostRideGray
import com.example.ghostride.ui.theme.GhostRideGreenLight

enum class SetupStatus {
    COMPLETE, PARTIAL, NOT_STARTED
}

private fun SetupStatus.color(): Color = when (this) {
    SetupStatus.COMPLETE -> GhostRideGreenLight
    SetupStatus.PARTIAL -> GhostRideAmber
    SetupStatus.NOT_STARTED -> GhostRideGray
}

private fun SetupStatus.label(): String = when (this) {
    SetupStatus.COMPLETE -> "Complete"
    SetupStatus.PARTIAL -> "Partial"
    SetupStatus.NOT_STARTED -> "Not Started"
}

data class SetupCardInfo(
    val title: String,
    val status: SetupStatus,
    val onClick: (() -> Unit)? = null
)

@Composable
fun SetupHomeScreen(
    modifier: Modifier = Modifier,
    onNavigateToProfile: () -> Unit = {},
    onNavigateToWorkingDays: () -> Unit = {},
    onNavigateToDriversVehicles: () -> Unit = {}
) {
    val context = LocalContext.current
    val database = remember { GhostRideDatabase.getInstance(context) }

    var profileStatus by remember { mutableStateOf(SetupStatus.NOT_STARTED) }
    var workingDaysStatus by remember { mutableStateOf(SetupStatus.NOT_STARTED) }
    var driversVehiclesStatus by remember { mutableStateOf(SetupStatus.NOT_STARTED) }

    LaunchedEffect(Unit) {
        val profile = database.profileDao().getProfile()
        profileStatus = if (profile != null && profile.name.isNotBlank()) {
            SetupStatus.COMPLETE
        } else {
            SetupStatus.NOT_STARTED
        }

        val days = database.workingDayDao().getAllWorkingDays()
        val anyEnabled = days.any { it.isEnabled }
        workingDaysStatus = if (anyEnabled) SetupStatus.COMPLETE else SetupStatus.NOT_STARTED

        val drivers = database.driverDao().getAllDrivers()
        val vehicles = database.vehicleDao().getAllVehicles()
        val hasCompleteDriverVehiclePair = drivers.any { driver ->
            vehicles.any { it.driverId == driver.id }
        }
        driversVehiclesStatus = if (hasCompleteDriverVehiclePair) SetupStatus.COMPLETE else SetupStatus.NOT_STARTED
    }

    val cards = listOf(
        SetupCardInfo("Profile", profileStatus, onClick = onNavigateToProfile),
        SetupCardInfo("Drivers & Vehicles", driversVehiclesStatus, onClick = onNavigateToDriversVehicles),
        SetupCardInfo("Home & Office Location", SetupStatus.NOT_STARTED),
        SetupCardInfo("Working Days", workingDaysStatus, onClick = onNavigateToWorkingDays)
    )

    val isFullySetUp = cards.all { it.status == SetupStatus.COMPLETE }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 24.dp)
    ) {
        Text(
            text = "Setup",
            style = MaterialTheme.typography.headlineMedium
        )

        if (!isFullySetUp) {
            Text(
                text = "Complete all sections below to start automatic tracking.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 6.dp, bottom = 24.dp)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            SetupCard(cards[0], modifier = Modifier.weight(1f))
            SetupCard(cards[1], modifier = Modifier.weight(1f))
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            SetupCard(cards[2], modifier = Modifier.weight(1f))
            SetupCard(cards[3], modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun SetupCard(
    info: SetupCardInfo,
    modifier: Modifier = Modifier
) {
    val cardModifier = modifier.aspectRatio(1f)
    val shape = RoundedCornerShape(20.dp)
    val colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    val elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)

    if (info.onClick != null) {
        Card(
            onClick = info.onClick,
            modifier = cardModifier,
            shape = shape,
            colors = colors,
            elevation = elevation
        ) {
            SetupCardBody(info)
        }
    } else {
        Card(
            modifier = cardModifier,
            shape = shape,
            colors = colors,
            elevation = elevation
        ) {
            SetupCardBody(info)
        }
    }
}

@Composable
private fun SetupCardBody(info: SetupCardInfo) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(18.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        StatusBadge(status = info.status)
        Column {
            Text(
                text = info.title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 14.dp)
            )
            Text(
                text = info.status.label(),
                style = MaterialTheme.typography.bodySmall,
                color = info.status.color(),
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

@Composable
private fun StatusBadge(status: SetupStatus) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .background(color = status.color().copy(alpha = 0.18f), shape = CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (status == SetupStatus.COMPLETE) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = "Complete",
                tint = status.color(),
                modifier = Modifier.size(16.dp)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(color = status.color(), shape = CircleShape)
            )
        }
    }
}