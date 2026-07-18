package com.example.ghostride.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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

private fun SetupStatus.icon(): String = when (this) {
    SetupStatus.COMPLETE -> "✅"
    SetupStatus.PARTIAL -> "🟡"
    SetupStatus.NOT_STARTED -> "⚪"
}

private fun SetupStatus.label(): String = when (this) {
    SetupStatus.COMPLETE -> "Complete"
    SetupStatus.PARTIAL -> "Partial"
    SetupStatus.NOT_STARTED -> "Not Started"
}

data class SetupCardInfo(
    val title: String,
    val status: SetupStatus
)

@Composable
fun SetupHomeScreen(
    modifier: Modifier = Modifier
) {
    // Placeholder statuses for now — real status wiring comes as each form
    // (Working Days, Drivers & Vehicles, Profile, Location) gets built in later steps.
    val cards = listOf(
        SetupCardInfo("Profile", SetupStatus.NOT_STARTED),
        SetupCardInfo("Drivers & Vehicles", SetupStatus.NOT_STARTED),
        SetupCardInfo("Home & Office Location", SetupStatus.NOT_STARTED),
        SetupCardInfo("Working Days", SetupStatus.NOT_STARTED)
    )

    val isFullySetUp = cards.all { it.status == SetupStatus.COMPLETE }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "Setup",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        if (!isFullySetUp) {
            Text(
                text = "Complete all sections below to start automatic tracking.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SetupCard(cards[0], modifier = Modifier.weight(1f))
            SetupCard(cards[1], modifier = Modifier.weight(1f))
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
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
    Card(
        modifier = modifier.aspectRatio(1f),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = info.status.icon(),
                style = MaterialTheme.typography.headlineSmall
            )
            Column {
                Text(
                    text = info.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = info.status.label(),
                    style = MaterialTheme.typography.bodySmall,
                    color = info.status.color()
                )
            }
        }
    }
}