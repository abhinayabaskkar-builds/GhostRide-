package com.example.ghostride.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.unit.dp
import com.example.ghostride.GhostRideDatabase
import com.example.ghostride.Weekday
import com.example.ghostride.WorkingDay
import com.example.ghostride.ui.theme.GhostRideOnPrimary
import kotlinx.coroutines.launch

@Composable
fun WorkingDaysScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val database = remember { GhostRideDatabase.getInstance(context) }
    val coroutineScope = rememberCoroutineScope()

    var workingDays by remember { mutableStateOf<List<WorkingDay>>(emptyList()) }

    LaunchedEffect(Unit) {
        workingDays = database.workingDayDao().getAllWorkingDays()
    }

    val orderedDays = listOf(
        Weekday.MONDAY, Weekday.TUESDAY, Weekday.WEDNESDAY,
        Weekday.THURSDAY, Weekday.FRIDAY, Weekday.SATURDAY, Weekday.SUNDAY
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 24.dp)
    ) {
        TextButton(
            onClick = onBack,
            modifier = Modifier.padding(bottom = 4.dp, start = (12).dp)
        ) {
            Text(
                text = "← Back to Setup",
                style = MaterialTheme.typography.labelLarge
            )
        }

        Text(
            text = "Working Days",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(top = 8.dp)
        )

        Text(
            text = "Select which days GhostRide should monitor for your commute.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            modifier = Modifier.padding(top = 6.dp, bottom = 20.dp)
        )

        orderedDays.forEachIndexed { index, day ->
            val currentEntry = workingDays.find { it.day == day }
            val isEnabled = currentEntry?.isEnabled ?: false

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = day.displayName(),
                    style = MaterialTheme.typography.bodyLarge
                )
                Switch(
                    checked = isEnabled,
                    onCheckedChange = { checked ->
                        val updated = WorkingDay(day = day, isEnabled = checked)
                        workingDays = workingDays.filter { it.day != day } + updated
                        coroutineScope.launch {
                            database.workingDayDao().insertWorkingDay(updated)
                        }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = GhostRideOnPrimary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary
                    )
                )
            }

            if (index < orderedDays.lastIndex) {
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
            }
        }

        val selectedCount = workingDays.count { it.isEnabled }
        if (selectedCount == 0) {
            Text(
                text = "Select at least one working day for tracking to activate.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }
}

private fun Weekday.displayName(): String = when (this) {
    Weekday.MONDAY -> "Monday"
    Weekday.TUESDAY -> "Tuesday"
    Weekday.WEDNESDAY -> "Wednesday"
    Weekday.THURSDAY -> "Thursday"
    Weekday.FRIDAY -> "Friday"
    Weekday.SATURDAY -> "Saturday"
    Weekday.SUNDAY -> "Sunday"
}