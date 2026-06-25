package com.example.foodapp.utils

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

object UniversalLocationEngine {

    fun isGmsAvailable(context: Context): Boolean {
        val availability = GoogleApiAvailability.getInstance()
        val resultCode = availability.isGooglePlayServicesAvailable(context)
        return resultCode == ConnectionResult.SUCCESS
    }

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(context: Context): Location? {
        return if (isGmsAvailable(context)) {
            getGmsLocation(context)
        } else {
            getNativeLocation(context)
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun getGmsLocation(context: Context): Location? = suspendCancellableCoroutine { continuation ->
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        val cancellationTokenSource = CancellationTokenSource()
        
        fusedLocationClient.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
            cancellationTokenSource.token
        )
            .addOnSuccessListener { location: Location? ->
                if (continuation.isActive) {
                    continuation.resume(location)
                }
            }
            .addOnFailureListener {
                if (continuation.isActive) {
                    continuation.resume(null)
                }
            }
            
        continuation.invokeOnCancellation {
            cancellationTokenSource.cancel()
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun getNativeLocation(context: Context): Location? = suspendCancellableCoroutine { continuation ->
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val mainHandler = Handler(Looper.getMainLooper())
        var bestLocation: Location? = null
        var locationReturned = false
        lateinit var locationListener: LocationListener

        fun finish(location: Location?) {
            if (!locationReturned && continuation.isActive) {
                locationReturned = true
                locationManager.removeUpdates(locationListener)
                mainHandler.removeCallbacksAndMessages(null)
                continuation.resume(location)
            }
        }

        locationListener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                if (!continuation.isActive || locationReturned) return

                val isBetter = bestLocation == null ||
                    location.accuracy <= (bestLocation?.accuracy ?: Float.MAX_VALUE)
                if (isBetter) {
                    bestLocation = location
                }

                if (location.provider == LocationManager.GPS_PROVIDER) {
                    finish(location)
                }
            }

            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
        }

        val hasNetwork = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        val hasGps = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)

        if (!hasNetwork && !hasGps) {
            if (continuation.isActive) continuation.resume(null)
            return@suspendCancellableCoroutine
        }

        bestLocation = listOfNotNull(
            if (hasNetwork) locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER) else null,
            if (hasGps) locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER) else null
        ).minByOrNull { it.accuracy }

        if (hasNetwork) {
            locationManager.requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER,
                0L,
                0f,
                locationListener,
                Looper.getMainLooper()
            )
        }
        
        if (hasGps) {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                0L,
                0f,
                locationListener,
                Looper.getMainLooper()
            )
        }

        mainHandler.postDelayed({
            finish(bestLocation)
        }, if (hasGps) GPS_WAIT_TIMEOUT_MS else NETWORK_ONLY_TIMEOUT_MS)

        // Cleanup on Coroutine cancellation (e.g. user navigates away before lock)
        continuation.invokeOnCancellation {
            locationManager.removeUpdates(locationListener)
            mainHandler.removeCallbacksAndMessages(null)
        }
    }

    private const val GPS_WAIT_TIMEOUT_MS = 8_000L
    private const val NETWORK_ONLY_TIMEOUT_MS = 2_000L
}
