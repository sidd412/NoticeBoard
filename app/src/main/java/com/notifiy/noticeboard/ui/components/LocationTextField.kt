package com.notifiy.noticeboard.ui.components

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.notifiy.noticeboard.utils.LocationManager
import com.notifiy.noticeboard.utils.hasLocationPermission
import androidx.activity.ComponentActivity

@Composable
fun LocationTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Organization Location",
    placeholder: String = "Enter city, state",
    isError: Boolean = false,
    onLocationRequested: () -> Unit = {}
) {
    val context = LocalContext.current
    var isLoadingLocation by remember { mutableStateOf(false) }
    
    val locationManager = remember { LocationManager(context) }
    
    val handleGetCurrentLocation = {
        // Debug log  
        android.util.Log.d("LocationTextField", "Current location button clicked")
        
        if (context.hasLocationPermission()) {
            android.util.Log.d("LocationTextField", "Permission already granted, getting location")
            isLoadingLocation = true
        } else {
            android.util.Log.d("LocationTextField", "No permission, calling callback")
            // Call the callback to handle permission request at Activity level
            onLocationRequested()
        }
    }
    
    // Handle location fetching when permission is granted
    LaunchedEffect(isLoadingLocation) {
        if (isLoadingLocation && context.hasLocationPermission()) {
            android.util.Log.d("LocationTextField", "Starting location fetch")
            try {
                val locationData = locationManager.getCurrentLocation()
                android.util.Log.d("LocationTextField", "Location data: $locationData")
                locationData?.let { data ->
                    onValueChange(data.fullAddress)
                    // Location fills automatically, no need for success toast
                } ?: run {
                    android.util.Log.d("LocationTextField", "No location data returned")
                    Toast.makeText(context, "Could not detect location. Please enter manually.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                android.util.Log.e("LocationTextField", "Error getting location", e)
                Toast.makeText(context, "Error getting location: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                isLoadingLocation = false
            }
        }
    }
    
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        leadingIcon = {
            Icon(Icons.Default.LocationOn, contentDescription = "Location Icon")
        },
        trailingIcon = {
            IconButton(
                onClick = handleGetCurrentLocation,
                enabled = !isLoadingLocation
            ) {
                Icon(
                    imageVector = Icons.Default.MyLocation,
                    contentDescription = "Get Current Location",
                    modifier = Modifier.padding(end = 4.dp)
                )
            }
        },
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        isError = isError,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
    )
}

@Preview(showBackground = true)
@Composable
fun LocationTextFieldPreview() {
    LocationTextField(
        value = "",
        onValueChange = {}
    )
}