package com.example.arts.fragments

import android.Manifest
import android.annotation.SuppressLint
import android.view.MotionEvent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.arts.R
import com.example.arts.adapters.RecentIncidentsAdapter
import com.example.arts.data.ReportDraft
import com.example.arts.utils.GeocodeHelper
import com.example.arts.utils.LocationHelper
import com.example.arts.utils.PrefsHelper
import com.example.arts.utils.toast
import com.example.arts.utils.MapHelper
import com.example.arts.viewmodels.IncidentViewModel
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.Point
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private val incidentViewModel: IncidentViewModel by viewModels()
    private var homeMapView: MapView? = null
    private var homeMap: MapLibreMap? = null

    private lateinit var tvUsername: TextView
    private lateinit var tvSafetyScore: TextView
    private lateinit var rvRecentIncidents: RecyclerView
    private lateinit var adapter: RecentIncidentsAdapter

    private val locationPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) sendPanicReport() else toast("Location permission is needed to send your position")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // MapLibre must be initialised before the MapView in this layout is created.
        MapLibre.getInstance(requireContext())
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvUsername = view.findViewById(R.id.tv_username)
        tvSafetyScore = view.findViewById(R.id.tv_safety_score)
        rvRecentIncidents = view.findViewById(R.id.rv_recent_incidents)

        // Set username from prefs
        val name = PrefsHelper.getUserName(requireContext()).split(" ").first()
        tvUsername.text = name

        // Setup RecyclerView
        adapter = RecentIncidentsAdapter(emptyList())
        rvRecentIncidents.layoutManager = LinearLayoutManager(requireContext())
        rvRecentIncidents.adapter = adapter
        rvRecentIncidents.isNestedScrollingEnabled = false

        // The three most recent reports, updating live from Firestore
        incidentViewModel.recentIncidents.observe(viewLifecycleOwner) { incidents ->
            adapter.updateList(incidents)
        }

        // Outcome of a panic report
        incidentViewModel.submitResult.observe(viewLifecycleOwner) { result ->
            if (result != null) {
                toast(
                    when {
                        result.queued -> "Saved on this phone. It will be sent when you are back online."
                        result.dispatchedCount > 0 -> "Emergency services have been notified of your location"
                        else -> "Your emergency report was received. An administrator will respond"
                    }
                )
                incidentViewModel.clearSubmitResult()
                findNavController().navigate(R.id.incidentsFragment)
            }
        }
        incidentViewModel.errorMessage.observe(viewLifecycleOwner) { message ->
            if (message != null) {
                toast(message)
                incidentViewModel.clearError()
            }
        }

        // Quick action cards
        view.findViewById<View>(R.id.card_report).setOnClickListener {
            findNavController().navigate(R.id.reportAccidentFragment)
        }
        view.findViewById<View>(R.id.card_track).setOnClickListener {
            findNavController().navigate(R.id.incidentsFragment)
        }
        view.findViewById<View>(R.id.card_emergency).setOnClickListener {
            showEmergencyDialog()
        }
        view.findViewById<View>(R.id.card_ai).setOnClickListener {
            findNavController().navigate(R.id.aiAssistantFragment)
        }
        view.findViewById<View>(R.id.card_voice_report).setOnClickListener {
            findNavController().navigate(R.id.voiceReportFragment)
        }
        view.findViewById<View>(R.id.card_weather_alert).setOnClickListener {
            findNavController().navigate(R.id.weatherAlertFragment)
        }
        view.findViewById<View>(R.id.card_community).setOnClickListener {
            findNavController().navigate(R.id.communityFragment)
        }
        view.findViewById<View>(R.id.card_safety_score).setOnClickListener {
            findNavController().navigate(R.id.safetyScoreFragment)
        }
        view.findViewById<View>(R.id.card_nearby_units).setOnClickListener {
            findNavController().navigate(R.id.nearbyUnitsFragment)
        }
        view.findViewById<View>(R.id.card_helplines).setOnClickListener {
            findNavController().navigate(R.id.helplinesFragment)
        }

        setupHomeMap(view)
    }

    private fun showEmergencyDialog() {
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Emergency Help")
            .setMessage("Do you need emergency assistance? Sending a panic report shares your current location immediately as a High priority incident.")
            .setPositiveButton("Call 112") { _, _ ->
                val intent = android.content.Intent(
                    android.content.Intent.ACTION_DIAL,
                    android.net.Uri.parse("tel:112")
                )
                startActivity(intent)
            }
            .setNeutralButton("Send Panic Report") { _, _ ->
                requestLocationAndSendPanic()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun requestLocationAndSendPanic() {
        val hasPermission = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (hasPermission) {
            sendPanicReport()
        } else {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun sendPanicReport() {
        toast("Finding your location...")

        viewLifecycleOwner.lifecycleScope.launch {
            val location = LocationHelper.currentLocation(requireContext())
            if (location == null) {
                toast("Couldn't get your location. Turn on location services and try again, or call 112.")
                return@launch
            }

            val locationText = GeocodeHelper.reverseGeocode(requireContext(), location.latitude, location.longitude)
                ?: "${location.latitude}, ${location.longitude}"

            incidentViewModel.submitReport(
                ReportDraft(
                    type = "Emergency",
                    description = "Panic button activated. Immediate assistance requested.",
                    priority = "High",
                    location = locationText,
                    latitude = location.latitude,
                    longitude = location.longitude,
                    panic = true
                )
            )
        }
    }

    private var mapSavedState: Bundle? = null
    private var activeMapStyle: org.maplibre.android.maps.Style? = null

    @SuppressLint("ClickableViewAccessibility")
    private fun setupHomeMap(view: View) {
        homeMapView = view.findViewById(R.id.map_home)
        homeMapView?.onCreate(mapSavedState)
        homeMapView?.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE ->
                    v.parent.requestDisallowInterceptTouchEvent(true)
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL ->
                    v.parent.requestDisallowInterceptTouchEvent(false)
            }
            false
        }
        homeMapView?.getMapAsync { map ->
            if (!isAdded) return@getMapAsync
            homeMap = map
            map.setStyle(MapHelper.osmStyle()) { style ->
                if (!isAdded) return@setStyle
                activeMapStyle = style
                map.cameraPosition = CameraPosition.Builder()
                    .target(LatLng(MapHelper.DEFAULT_LAT, MapHelper.DEFAULT_LNG))
                    .zoom(10.0)
                    .build()
                // Initial pins from whatever is already loaded
                updateHomePins(style)
                if (MapHelper.hasLocationPermission(requireContext())) {
                    MapHelper.enableLiveLocation(requireContext(), map, style)
                }
            }
        }

        // Observe incidents on the main thread - safe to do here in onViewCreated
        incidentViewModel.recentIncidents.observe(viewLifecycleOwner) { incidents ->
            val style = activeMapStyle ?: return@observe
            if (isAdded) updateHomePins(style)
        }
    }

    private fun updateHomePins(style: org.maplibre.android.maps.Style) {
        // Remove old incident pins before re-adding to avoid duplicate-source crashes
        try {
            style.removeLayer("arts-home-pins-layer")
            style.removeSource("arts-home-pins-source")
        } catch (_: Exception) { /* not yet added, ignore */ }

        val incidents = incidentViewModel.recentIncidents.value ?: return
        val points = incidents.mapNotNull { inc ->
            val lat = inc.latitude ?: return@mapNotNull null
            val lng = inc.longitude ?: return@mapNotNull null
            org.maplibre.geojson.Point.fromLngLat(lng, lat)
        }
        if (points.isEmpty()) return

        try {
            val collection = org.maplibre.geojson.FeatureCollection.fromFeatures(
                points.map { org.maplibre.geojson.Feature.fromGeometry(it) }
            )
            style.addSource(org.maplibre.android.style.sources.GeoJsonSource("arts-home-pins-source", collection))
            style.addLayer(
                org.maplibre.android.style.layers.CircleLayer("arts-home-pins-layer", "arts-home-pins-source")
                    .withProperties(
                        org.maplibre.android.style.layers.PropertyFactory.circleRadius(9f),
                        org.maplibre.android.style.layers.PropertyFactory.circleColor(android.graphics.Color.parseColor("#C81E3A")),
                        org.maplibre.android.style.layers.PropertyFactory.circleStrokeColor(android.graphics.Color.WHITE),
                        org.maplibre.android.style.layers.PropertyFactory.circleStrokeWidth(2.5f)
                    )
            )
        } catch (e: Exception) {
            android.util.Log.w("ARTS-HomeMap", "Could not draw incident pins: ${e.message}")
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        homeMapView?.onSaveInstanceState(outState)
    }

    override fun onResume() {
        super.onResume()
        homeMapView?.onResume()
    }

    override fun onPause() {
        super.onPause()
        homeMapView?.onPause()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        homeMapView?.onLowMemory()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        activeMapStyle = null
        homeMapView?.onDestroy()
        homeMapView = null
        homeMap = null
    }
}
