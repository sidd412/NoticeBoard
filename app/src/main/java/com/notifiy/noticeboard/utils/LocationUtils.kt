package com.notifiy.noticeboard.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import androidx.core.content.ContextCompat.checkSelfPermission
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.tasks.await
import java.io.IOException
import java.util.Locale

data class LocationData(
    val city: String,
    val state: String,
    val pincode: String,
    val fullAddress: String
)

class LocationManager(private val context: Context) {
    private val fusedLocationClient: FusedLocationProviderClient = 
        LocationServices.getFusedLocationProviderClient(context)
    
    private val geocoder = Geocoder(context, Locale.getDefault())
    
    suspend fun getCurrentLocation(): LocationData? {
        return try {
            // Check permissions
            if (checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return null
            }
            
            // Get current location
            val location = fusedLocationClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null).await()
            
            // Reverse geocoding to get address
            val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
            
            if (addresses?.isNotEmpty() == true) {
                val address = addresses[0]
                LocationData(
                    city = address.locality ?: address.subLocality ?: "Unknown City",
                    state = address.adminArea ?: "Unknown State",
                    pincode = address.postalCode ?: "Unknown",
                    fullAddress = formatAddress(address)
                )
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    private fun formatAddress(address: Address): String {
        val parts = mutableListOf<String>()
        
        // Add city/sub-locality
        val city = address.locality ?: address.subLocality
        if (!city.isNullOrBlank()) {
            parts.add(city)
        }
        
        // Add state
        if (!address.adminArea.isNullOrBlank()) {
            parts.add(address.adminArea!!)
        }
        
        return parts.joinToString(", ")
    }
}


// Extension function to check location permissions
fun Context.hasLocationPermission(): Boolean {
    return checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
           checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
}

