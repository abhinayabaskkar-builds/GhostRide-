package com.example.ghostride.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.ghostride.GhostRideDatabase
import com.example.ghostride.Location
import com.example.ghostride.LocationType
import com.example.ghostride.PlaceResult
import com.example.ghostride.PlacesApiClient
import kotlinx.coroutines.launch

@Composable
fun LocationScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val database = remember { GhostRideDatabase.getInstance(context) }

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("Home", "Office")

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
            text = "Home & Office Location",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(top = 8.dp)
        )

        Text(
            text = "Search for each address. Both save independently.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            modifier = Modifier.padding(top = 6.dp, bottom = 16.dp)
        )

        TabRow(selectedTabIndex = selectedTab) {
            tabTitles.forEachIndexed { index, tabTitle ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(tabTitle) }
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(top = 16.dp)
        ) {
            when (selectedTab) {
                0 -> LocationSection(locationType = LocationType.HOME, database = database)
                1 -> LocationSection(locationType = LocationType.OFFICE, database = database)
            }
        }
    }
}

@Composable
private fun LocationSection(
    locationType: LocationType,
    database: GhostRideDatabase,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()

    var savedLocation by remember(locationType) { mutableStateOf<Location?>(null) }
    var showSearchUi by remember(locationType) { mutableStateOf(false) }
    var searchQuery by remember(locationType) { mutableStateOf("") }
    var searchResults by remember(locationType) { mutableStateOf<List<PlaceResult>>(emptyList()) }
    var isSearching by remember(locationType) { mutableStateOf(false) }
    var hasSearched by remember(locationType) { mutableStateOf(false) }

    LaunchedEffect(locationType) {
        val existing = database.locationDao().getLocation(locationType)
        savedLocation = existing
        showSearchUi = existing == null
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (savedLocation != null) {
                Text(
                    text = savedLocation!!.displayAddress,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                AsyncImage(
                    model = PlacesApiClient.staticMapUrl(
                        savedLocation!!.latitude,
                        savedLocation!!.longitude
                    ),
                    contentDescription = "Map preview",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                )

                if (!showSearchUi) {
                    TextButton(
                        onClick = { showSearchUi = true },
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Text("Change address", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }

            if (showSearchUi) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Search address") },
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        isSearching = true
                        hasSearched = true
                        coroutineScope.launch {
                            searchResults = PlacesApiClient.searchPlaces(searchQuery)
                            isSearching = false
                        }
                    },
                    enabled = searchQuery.isNotBlank() && !isSearching,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    Text(if (isSearching) "Searching..." else "Search")
                }

                if (savedLocation != null) {
                    TextButton(
                        onClick = {
                            showSearchUi = false
                            searchQuery = ""
                            searchResults = emptyList()
                            hasSearched = false
                        }
                    ) {
                        Text("Cancel", style = MaterialTheme.typography.labelLarge)
                    }
                }

                if (hasSearched && !isSearching && searchResults.isEmpty()) {
                    Text(
                        text = "No results found. Try a different search.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                searchResults.forEach { result ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.background
                        ),
                        onClick = {
                            coroutineScope.launch {
                                val newLocation = Location(
                                    type = locationType,
                                    displayAddress = result.address.ifBlank { result.name },
                                    latitude = result.latitude,
                                    longitude = result.longitude,
                                    geofenceRadiusMeters = 200
                                )
                                database.locationDao().saveLocation(newLocation)
                                savedLocation = newLocation
                                searchResults = emptyList()
                                searchQuery = ""
                                hasSearched = false
                                showSearchUi = false
                            }
                        }
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = result.name.ifBlank { "Unnamed location" },
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = result.address,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }
        }
    }
}