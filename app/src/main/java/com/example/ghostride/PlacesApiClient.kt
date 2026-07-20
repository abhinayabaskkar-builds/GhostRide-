package com.example.ghostride

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

data class PlaceResult(
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double
)

object PlacesApiClient {

    private val client = OkHttpClient()
    private const val SEARCH_URL = "https://places.googleapis.com/v1/places:searchText"

    suspend fun searchPlaces(query: String): List<PlaceResult> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()

        val requestBodyJson = JSONObject().apply {
            put("textQuery", query)
        }

        val body = requestBodyJson.toString()
            .toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url(SEARCH_URL)
            .post(body)
            .addHeader("Content-Type", "application/json")
            .addHeader("X-Goog-Api-Key", BuildConfig.MAPS_API_KEY)
            .addHeader(
                "X-Goog-FieldMask",
                "places.displayName,places.formattedAddress,places.location"
            )
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()

                val responseBody = response.body?.string() ?: return@withContext emptyList()
                val json = JSONObject(responseBody)
                val places = json.optJSONArray("places") ?: return@withContext emptyList()

                (0 until places.length()).mapNotNull { index ->
                    val place = places.optJSONObject(index) ?: return@mapNotNull null
                    val displayName = place.optJSONObject("displayName")?.optString("text") ?: ""
                    val address = place.optString("formattedAddress", "")
                    val location = place.optJSONObject("location") ?: return@mapNotNull null
                    val lat = location.optDouble("latitude")
                    val lng = location.optDouble("longitude")

                    PlaceResult(
                        name = displayName,
                        address = address,
                        latitude = lat,
                        longitude = lng
                    )
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun staticMapUrl(latitude: Double, longitude: Double): String {
        return "https://maps.googleapis.com/maps/api/staticmap" +
                "?center=$latitude,$longitude" +
                "&zoom=15&size=400x200&markers=color:red%7C$latitude,$longitude" +
                "&key=${BuildConfig.MAPS_API_KEY}"
    }
}