package com.pallas.launcher.platform

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.CancellationSignal
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import kotlin.coroutines.resume

object LocationHelper {

    fun hasLocationPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun isLocationEnabled(context: Context): Boolean {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            lm.isLocationEnabled
        } else {
            lm.isProviderEnabled(LocationManager.GPS_PROVIDER) || lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        }
    }

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(context: Context): Pair<Double, Double>? = withContext(Dispatchers.IO) {
        val appContext = context.applicationContext
        if (!hasLocationPermission(appContext)) return@withContext null

        val lm = appContext.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return@withContext null

        val hasFine = ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        // 1. Check last known locations across all providers
        var bestLocation: Location? = null
        val providers = try { lm.allProviders } catch (_: Exception) { lm.getProviders(true) }
        for (provider in providers) {
            val loc = try {
                lm.getLastKnownLocation(provider)
            } catch (_: SecurityException) {
                null
            } ?: continue

            if (bestLocation == null || loc.time > bestLocation.time) {
                bestLocation = loc
            }
        }

        // If last known location is fresh (< 3 minutes old), return it immediately
        val now = System.currentTimeMillis()
        if (bestLocation != null && (now - bestLocation.time) < 3 * 60 * 1000L) {
            return@withContext Pair(bestLocation.latitude, bestLocation.longitude)
        }

        // 2. Request a fresh single location update with a strict timeout
        val freshLocation = withTimeoutOrNull(4500L) {
            suspendCancellableCoroutine { continuation ->
                val cancellationSignal = CancellationSignal()
                continuation.invokeOnCancellation { cancellationSignal.cancel() }
                val allProvs = try { lm.allProviders } catch (_: Exception) { emptyList() }
                val enabledProvs = try { lm.getProviders(true) } catch (_: Exception) { emptyList() }
                val provider = when {
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                        allProvs.contains(LocationManager.FUSED_PROVIDER) &&
                        lm.isProviderEnabled(LocationManager.FUSED_PROVIDER) -> LocationManager.FUSED_PROVIDER
                    lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
                    hasFine && lm.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
                    else -> enabledProvs.firstOrNull {
                        hasFine || (it != LocationManager.GPS_PROVIDER && it != LocationManager.PASSIVE_PROVIDER)
                    } ?: LocationManager.NETWORK_PROVIDER
                }
                try {
                    lm.getCurrentLocation(provider, cancellationSignal, appContext.mainExecutor) { location ->
                        if (continuation.isActive) continuation.resume(location)
                    }
                } catch (e: Exception) {
                    if (continuation.isActive) continuation.resume(null)
                }
            }
        }

        val finalLoc = freshLocation ?: bestLocation
        finalLoc?.let { Pair(it.latitude, it.longitude) }
    }

    suspend fun reverseGeocode(context: Context, lat: Double, lon: Double): String? = withContext(Dispatchers.IO) {
        val appContext = context.applicationContext
        withTimeoutOrNull(3000L) {
            try {
                if (Geocoder.isPresent()) {
                    val geocoder = Geocoder(appContext, Locale.getDefault())
                    val addresses: List<Address>? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        suspendCancellableCoroutine { cont ->
                            try {
                                geocoder.getFromLocation(lat, lon, 1, object : Geocoder.GeocodeListener {
                                    override fun onGeocode(addresses: MutableList<Address>) {
                                        if (cont.isActive) cont.resume(addresses)
                                    }
                                    override fun onError(errorMessage: String?) {
                                        if (cont.isActive) cont.resume(null)
                                    }
                                })
                            } catch (e: Exception) {
                                if (cont.isActive) cont.resume(null)
                            }
                        }
                    } else {
                        @Suppress("DEPRECATION")
                        geocoder.getFromLocation(lat, lon, 1)
                    }

                    if (!addresses.isNullOrEmpty()) {
                        val addr = addresses[0]
                        val locality = addr.locality ?: addr.subAdminArea ?: addr.adminArea
                        val country = addr.countryName
                        if (!locality.isNullOrBlank()) {
                            return@withTimeoutOrNull if (!country.isNullOrBlank()) "$locality, $country" else locality
                        }
                    }
                }
            } catch (_: Exception) {}
            null
        }
    }
}
