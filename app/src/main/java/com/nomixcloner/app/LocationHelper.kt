package com.nomixcloner.app

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.Build
import androidx.core.content.ContextCompat.getMainExecutor
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Granularity
import com.google.android.gms.location.LastLocationRequest
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.Task
import java.util.function.Consumer
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class LocationHelper {

    @SuppressLint("MissingPermission")
    suspend fun requestNativeCurrentLocation(
        locationManager: LocationManager,
        context: Context
    ): String = suspendCoroutine { continuation ->
        val consumer =
            Consumer<Location> { location -> continuation.resume("Native current: lat ${location.latitude}, long ${location.longitude}\n") }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            locationManager.getCurrentLocation(
                LocationManager.NETWORK_PROVIDER,
                null,
                getMainExecutor(context),
                consumer
            )
        } else {
            continuation.resume("Native current: Unavailable\n")
        }
    }

    @SuppressLint("MissingPermission")
    fun requestNativeLastLocation(
        locationManager: LocationManager,
    ): String {
        val location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
        return if (location != null) {
            "Native last: lat ${location.latitude}, long ${location.longitude}\n"
        } else {
            "Native last: Unavailable\n"
        }
    }

    @SuppressLint("MissingPermission")
    suspend fun requestLastLocation(
        fusedLocationClient: FusedLocationProviderClient
    ): String = suspendCoroutine { continuation ->
        val lastLocation: Task<Location> = fusedLocationClient.lastLocation
        lastLocation.addOnCompleteListener { task ->
            if (task.isSuccessful && task.result != null) {
                val location: Location = task.result!!
                continuation.resume("Last: lat ${location.latitude}, long ${location.longitude}\n")
            } else {
                continuation.resume("Last: Unknown\n")
            }
        }
    }

    @SuppressLint("MissingPermission")
    suspend fun requestLastLocationBound(
        fusedLocationClient: FusedLocationProviderClient
    ): String = suspendCoroutine { continuation ->
        val lastLocationBound: Task<Location> = fusedLocationClient.getLastLocation(
            LastLocationRequest.Builder()
                .setGranularity(Granularity.GRANULARITY_COARSE)
                .setMaxUpdateAgeMillis(1000)
                .build()
        )
        lastLocationBound.addOnCompleteListener { task ->
            if (task.isSuccessful && task.result != null) {
                val location: Location = task.result!!
                continuation.resume("Last bound: lat ${location.latitude}, long ${location.longitude}\n")
            } else {
                continuation.resume("Last bound: Unknown\n")
            }
        }
    }

    @SuppressLint("MissingPermission")
    suspend fun requestCurrentLocation(
        fusedLocationClient: FusedLocationProviderClient
    ): String = suspendCoroutine { continuation ->
        val currentLocation: Task<Location> =
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
        currentLocation.addOnCompleteListener { task ->
            if (task.isSuccessful && task.result != null) {
                val location: Location = task.result!!
                continuation.resume("Current: lat ${location.latitude}, long ${location.longitude}\n")
            } else {
                continuation.resume("Current: Unknown\n")
            }
        }
    }

    @SuppressLint("MissingPermission")
    suspend fun requestCurrentLocationBound(
        fusedLocationClient: FusedLocationProviderClient
    ): String = suspendCoroutine { continuation ->
        val currentLocationBound: Task<Location> = fusedLocationClient.getCurrentLocation(
            CurrentLocationRequest.Builder()
                .setGranularity(Granularity.GRANULARITY_FINE)
                .setMaxUpdateAgeMillis(1000)
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                .build(),
            null
        )
        currentLocationBound.addOnCompleteListener { task ->
            if (task.isSuccessful && task.result != null) {
                val location: Location = task.result!!
                continuation.resume("Current bound: lat ${location.latitude}, long ${location.longitude}\n")
            } else {
                continuation.resume("Current bound: Unknown\n")
            }
        }
    }
}