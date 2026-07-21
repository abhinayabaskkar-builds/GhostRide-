package com.example.ghostride

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

object GeocodingApiClient {

    private val client = OkHttpClient()
    private const val GEOCODE_URL = "https://maps.googleapis.com/maps/api/geocode/json"

    /**
     * Takes a raw GPS coordinate and returns a short, human-readable address
     * (e.g. "Bonifacio High Street, Taguig"), or null if it can't be determined
     * (no internet, bad coordinates, API error, etc.).
     */
    suspend fun reverseGeocode(latitude: Double, longitude: Double): String? =
        withContext(Dispatchers.IO) {
            val url = "$GEOCODE_URL?latlng=$latitude,$longitude&key=${BuildConfig.MAPS_API_KEY}"

            val request = Request.Builder()
                .url(url)
                .get()
                .build()

            try {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@withContext null

                    val responseBody = response.body?.string() ?: return@withContext null
                    val json = JSONObject(responseBody)

                    if (json.optString("status") != "OK") return@withContext null

                    val results = json.optJSONArray("results") ?: return@withContext null
                    if (results.length() == 0) return@withContext null

                    val firstResult = results.optJSONObject(0) ?: return@withContext null
                    val formattedAddress = firstResult.optString("formatted_address", "")
                    if (formattedAddress.isBlank()) return@withContext null

                    shortenAddress(formattedAddress)
                }
            } catch (e: Exception) {
                null
            }
        }

    /**
     * Keeps only the first two comma-separated parts of a full address,
     * so it fits cleanly on a small card (e.g. "Bonifacio High Street, Taguig"
     * instead of "Bonifacio High Street, Taguig, Metro Manila, Philippines").
     */
    private fun shortenAddress(formattedAddress: String): String {
        val parts = formattedAddress.split(",").map { it.trim() }
        return if (parts.size >= 2) {
            "${parts[0]}, ${parts[1]}"
        } else {
            parts.first()
        }
    }
}