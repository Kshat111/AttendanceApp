package com.example.myapplication.util

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

data class LocationResult(
    val latitude: Double?,
    val longitude: Double?
)

object LocationHelper {

    private const val TAG = "LocationHelper"

    fun hasLocationPermission(context: Context): Boolean {
        val fineGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        return fineGranted || coarseGranted
    }

    suspend fun getCurrentLocation(context: Context): LocationResult {
        if (!hasLocationPermission(context)) {
            Log.d(TAG, "Location permission not granted. Returning null coordinates.")
            return LocationResult(null, null)
        }

        return try {
            val location = withTimeoutOrNull(4000L) {
                fetchLocation(context)
            }

            if (location != null) {
                Log.d(TAG, "Location obtained: lat=${location.latitude}, lng=${location.longitude}")
                LocationResult(location.latitude, location.longitude)
            } else {
                Log.w(TAG, "Location request timed out or returned null.")
                LocationResult(null, null)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching location", e)
            LocationResult(null, null)
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun fetchLocation(context: Context): Location? = suspendCancellableCoroutine { continuation ->
        val client: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
        val cts = CancellationTokenSource()

        try {
            client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cts.token)
                .addOnSuccessListener { location ->
                    if (location != null) {
                        if (continuation.isActive) continuation.resume(location)
                    } else {
                        client.lastLocation
                            .addOnSuccessListener { lastLoc ->
                                if (continuation.isActive) continuation.resume(lastLoc)
                            }
                            .addOnFailureListener {
                                if (continuation.isActive) continuation.resume(null)
                            }
                    }
                }
                .addOnFailureListener {
                    if (continuation.isActive) continuation.resume(null)
                }
        } catch (_: SecurityException) {
            if (continuation.isActive) continuation.resume(null)
        }
    }
}
