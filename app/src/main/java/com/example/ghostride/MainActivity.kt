package com.example.ghostride

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.lifecycleScope
import com.example.ghostride.ui.theme.GhostRideTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = GhostRideDatabase.getInstance(applicationContext)

        lifecycleScope.launch {
            val existingDays = database.workingDayDao().getAllWorkingDays()
            if (existingDays.isEmpty()) {
                val defaults = listOf(
                    WorkingDay(Weekday.MONDAY, true),
                    WorkingDay(Weekday.TUESDAY, true),
                    WorkingDay(Weekday.WEDNESDAY, true),
                    WorkingDay(Weekday.THURSDAY, true),
                    WorkingDay(Weekday.FRIDAY, true),
                    WorkingDay(Weekday.SATURDAY, false),
                    WorkingDay(Weekday.SUNDAY, false)
                )
                defaults.forEach { database.workingDayDao().insertWorkingDay(it) }
            }
        }

        setContent {
            GhostRideTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        name = "Android",
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    GhostRideTheme {
        Greeting("Android")
    }
}