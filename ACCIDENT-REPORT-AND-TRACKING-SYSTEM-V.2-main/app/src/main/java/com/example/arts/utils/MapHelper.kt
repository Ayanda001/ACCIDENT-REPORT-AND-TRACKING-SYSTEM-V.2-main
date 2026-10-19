package com.example.arts.utils

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import androidx.core.content.ContextCompat
import org.maplibre.android.location.LocationComponentActivationOptions
import org.maplibre.android.location.modes.CameraMode
import org.maplibre.android.location.modes.RenderMode
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.PropertyFactory.circleColor
import org.maplibre.android.style.layers.PropertyFactory.circleRadius
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeColor
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeWidth
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.Point

/**
 * Shared map setup for the report and incident screens.
 *
 * Maps are drawn by MapLibre using real OpenStreetMap street tiles, with a red
 * pin for the incident and MapLibre's blue dot for the phone's live location.
 */
object MapHelper {

    // Sandton, Johannesburg: where the map opens until the phone's location is known.
    const val DEFAULT_LAT = -26.107
    const val DEFAULT_LNG = 28.056

    private const val PIN_SOURCE = "arts-pin-source"
    private const val PIN_LAYER = "arts-pin-layer"
    private const val ALERT_RED = "#C81E3A"

    // A MapLibre style that shows the OpenStreetMap raster tiles.
    private const val OSM_STYLE_URL = "https://tiles.openfreemap.org/styles/bright"

    fun osmStyle(): Style.Builder = Style.Builder().fromUri(OSM_STYLE_URL)

    fun hasLocationPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    /** Draws, or moves, the red incident pin. Safe against duplicate layers/sources. */
    fun setPin(style: Style, lat: Double, lng: Double) {
        val feature = Feature.fromGeometry(Point.fromLngLat(lng, lat))
        
        val existingSource = style.getSourceAs<GeoJsonSource>(PIN_SOURCE)
        if (existingSource != null) {
            existingSource.setGeoJson(feature)
        } else {
            style.addSource(GeoJsonSource(PIN_SOURCE, feature))
        }

        if (style.getLayer(PIN_LAYER) == null) {
            style.addLayer(
                CircleLayer(PIN_LAYER, PIN_SOURCE).withProperties(
                    circleRadius(10f),
                    circleColor(Color.parseColor(ALERT_RED)),
                    circleStrokeColor(Color.WHITE),
                    circleStrokeWidth(3f)
                )
            )
        }
    }

    /**
     * Shows the phone's live location as a blue dot that follows it as it moves.
     * Does nothing, and returns false, if location permission has not been granted.
     */
    @SuppressLint("MissingPermission")
    fun enableLiveLocation(context: Context, map: MapLibreMap, style: Style): Boolean {
        if (!hasLocationPermission(context)) return false
        val component = map.locationComponent
        if (!component.isLocationComponentActivated) {
            component.activateLocationComponent(
                LocationComponentActivationOptions.builder(context, style).useDefaultLocationEngine(true).build()
            )
        }
        component.isLocationComponentEnabled = true
        component.cameraMode = CameraMode.NONE
        component.renderMode = RenderMode.NORMAL
        return true
    }
}
