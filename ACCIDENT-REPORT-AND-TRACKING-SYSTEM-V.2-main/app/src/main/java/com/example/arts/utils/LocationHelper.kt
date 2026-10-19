package com.example.arts.utils

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import androidx.core.os.CancellationSignal
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/**
 * Finds where the phone is right now, for the panic button.
 *
 * Uses a recent known position if there is one, otherwise asks the phone for a
 * fresh fix, and as a last resort falls back to an older position rather than
 * failing, because in an emergency an approximate location is better than none.
 */
object LocationHelper {

    @SuppressLint("MissingPermission")
    suspend fun currentLocation(context: Context, maxAgeMs: Long = 120_000, timeoutMs: Long = 12_000): Location? {
        if (!MapHelper.hasLocationPermission(context)) return null
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            .filter { runCatching { manager.isProviderEnabled(it) }.getOrDefault(false) }
        if (providers.isEmpty()) return null

        fun knownFixes(): List<Location> =
            providers.mapNotNull { runCatching { manager.getLastKnownLocation(it) }.getOrNull() }

        // 1. A fix from the last couple of minutes is accurate enough.
        val recent = knownFixes()
            .filter { System.currentTimeMillis() - it.time <= maxAgeMs }
            .maxByOrNull { it.time }
        if (recent != null) return recent

        // 2. Otherwise ask the phone for a fresh one.
        val fresh = withTimeoutOrNull(timeoutMs) {
            suspendCancellableCoroutine<Location?> { continuation ->
                val signal = CancellationSignal()
                continuation.invokeOnCancellation { signal.cancel() }
                LocationManagerCompat.getCurrentLocation(
                    manager, providers.first(), signal, ContextCompat.getMainExecutor(context)
                ) { location ->
                    if (continuation.isActive) continuation.resume(location)
                }
            }
        }
        if (fresh != null) return fresh

        // 3. Fall back to any older fix.
        return knownFixes().maxByOrNull { it.time }
    }
}
