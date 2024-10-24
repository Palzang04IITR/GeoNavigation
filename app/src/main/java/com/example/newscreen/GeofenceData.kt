package com.example.newscreen

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

import java.io.Serializable

data class GeofenceData(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val description: String,
    val imageUrl: String,
    val radius:Double
) : Serializable
 {
    companion object {
        fun sortClockwise(locations: List<GeofenceData>, userLatitude: Double, userLongitude: Double): List<GeofenceData> {
            if (locations.isEmpty()) return emptyList()

            // Find the closest location to the user's current location
            val startLocation = locations.minByOrNull { calculateDistance(userLatitude, userLongitude, it.latitude, it.longitude) }
                ?: locations.first()
            val remainingLocations = locations.filter { it != startLocation }

            // Determine angles from the starting location
            val angles = remainingLocations.map { location ->
                var angle = atan2(
                    location.latitude - startLocation.latitude,
                    location.longitude - startLocation.longitude
                )
                if (angle < 0) angle += 2 * Math.PI // Normalize angle to 0..2*PI
                Pair(location, angle)
            }

            // Sort locations by these angles in ascending order and prepend the starting location
            return listOf(startLocation) + angles.sortedBy { it.second }.map { it.first }
        }

        fun getSequenceNames(locations: List<GeofenceData>): ArrayList<String> {
            return ArrayList(locations.map { it.name })
        }

        private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
            val earthRadius = 6371000.0 // Radius of the Earth in meters
            val dLat = Math.toRadians(lat2 - lat1)
            val dLon = Math.toRadians(lon2 - lon1)
            val a = sin(dLat / 2) * sin(dLat / 2) +
                    cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                    sin(dLon / 2) * sin(dLon / 2)
            val c = 2 * atan2(sqrt(a), sqrt(1 - a))
            return earthRadius * c
        }
    }
}

object GeofenceDataManager {
    // A map to store GeofenceData instances, using the name as the key
    private val geofenceDataMap = mutableMapOf<String, GeofenceData>()

    // Function to add geofence data to the manager
    fun addGeofenceData(geofenceData: GeofenceData) {
        geofenceDataMap[geofenceData.name] = geofenceData
    }

    fun getAllGeofenceData(): List<GeofenceData> {
        return geofenceDataMap.values.toList()
    }

    // Function to retrieve geofence data by name
    fun getGeofenceData(name: String): GeofenceData? {
        return geofenceDataMap[name]
    }

    fun getGeofenceDataByImageUrl(imageUrl: String): GeofenceData? {
        return geofenceDataMap.values.find { it.imageUrl == imageUrl }
    }

    // Optional function to remove geofence data by name
    fun removeGeofenceData(name: String) {
        geofenceDataMap.remove(name)
    }

    // Function to clear all stored geofence data
    fun clearAllGeofenceData() {
        geofenceDataMap.clear()
    }
}
