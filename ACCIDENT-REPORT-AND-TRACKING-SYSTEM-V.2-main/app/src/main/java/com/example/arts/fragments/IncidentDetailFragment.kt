package com.example.arts.fragments

import android.Manifest
import android.annotation.SuppressLint
import android.os.Bundle
import android.view.MotionEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.arts.R
import com.example.arts.utils.MapHelper
import com.example.arts.utils.QrHelper
import com.example.arts.utils.WeatherCodeMapper
import com.example.arts.utils.toast
import com.example.arts.viewmodels.IncidentViewModel
import com.example.arts.viewmodels.WeatherState
import com.example.arts.viewmodels.WeatherViewModel
import org.maplibre.android.MapLibre
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapView

class IncidentDetailFragment : Fragment() {

    private val args: IncidentDetailFragmentArgs by navArgs()
    private val incidentViewModel: IncidentViewModel by viewModels()
    private val weatherViewModel: WeatherViewModel by viewModels()

    private var mapView: MapView? = null
    private var mapLibreMap: MapLibreMap? = null
    private var mapStyle: Style? = null
    private var mapLoaded = false

    // Asks once for location so the person's live position can be shown next to the incident.
    private val locationPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val map = mapLibreMap
        val style = mapStyle
        if (granted && map != null && style != null && isAdded) {
            MapHelper.enableLiveLocation(requireContext(), map, style)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // MapLibre must be initialized before the MapView inflates/creates.
        MapLibre.getInstance(requireContext())
        return inflater.inflate(R.layout.fragment_incident_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<View>(R.id.btn_back).setOnClickListener {
            findNavController().navigateUp()
        }

        view.findViewById<View>(R.id.btn_show_qr).setOnClickListener {
            showQrDialog(args.incidentId)
        }

        mapView = view.findViewById(R.id.map_incident_location)
        enableMapDragInsideScroll()
        mapView?.onCreate(savedInstanceState)

        observeWeather(view)

        incidentViewModel.getIncidentById(args.incidentId).observe(viewLifecycleOwner) { incident ->
            if (incident == null) {
                toast("That incident could not be found")
                return@observe
            }

            view.findViewById<TextView>(R.id.tv_title).text = incident.title
            view.findViewById<TextView>(R.id.tv_status).text = "Status: ${incident.status}"
            view.findViewById<TextView>(R.id.tv_location).text = incident.location
            view.findViewById<TextView>(R.id.tv_time).text = incident.timeAgo
            view.findViewById<TextView>(R.id.tv_description).text = incident.description

            val photoBitmap = com.example.arts.utils.ImageStorageHelper.decodeDataUrl(incident.photoUrl)
            if (photoBitmap != null) {
                view.findViewById<ImageView>(R.id.iv_incident_photo).setImageBitmap(photoBitmap)
                view.findViewById<View>(R.id.card_incident_photo).visibility = View.VISIBLE
            }

            val lat = incident.latitude
            val lng = incident.longitude
            if (lat != null && lng != null) {
                val position = LatLng(lat, lng)
                if (!mapLoaded) {
                    mapLoaded = true
                    mapView?.getMapAsync { map ->
                        mapLibreMap = map
                        map.setStyle(MapHelper.osmStyle()) { style ->
                            if (!isAdded) return@setStyle
                            mapStyle = style
                            map.cameraPosition = CameraPosition.Builder().target(position).zoom(14.0).build()
                            MapHelper.setPin(style, lat, lng)
                            // Request location permission on the main thread after style loads
                            view?.post {
                                if (isAdded) {
                                    if (MapHelper.hasLocationPermission(requireContext())) {
                                        MapHelper.enableLiveLocation(requireContext(), map, style)
                                    } else {
                                        locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                                    }
                                }
                            }
                        }
                    }
                } else {
                    mapStyle?.let { MapHelper.setPin(it, lat, lng) }
                }
                weatherViewModel.loadWeather(lat, lng)
            } else {
                // Reports made without pinning a location have no map or weather.
                (mapView?.parent as? View)?.visibility = View.GONE
                view.findViewById<TextView>(R.id.tv_weather_condition).text = "Weather unavailable"
                view.findViewById<TextView>(R.id.tv_weather_details).text = "No pinned location on this report"
            }
        }
    }

    private fun observeWeather(view: View) {
        val tvCondition = view.findViewById<TextView>(R.id.tv_weather_condition)
        val tvDetails = view.findViewById<TextView>(R.id.tv_weather_details)
        val tvEmoji = view.findViewById<TextView>(R.id.tv_weather_emoji)

        weatherViewModel.weatherState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is WeatherState.Loading -> {
                    tvCondition.text = "Current weather at this location"
                    tvDetails.text = "Loading…"
                }
                is WeatherState.Success -> {
                    val w = state.weather
                    tvCondition.text = WeatherCodeMapper.describe(w.weathercode)
                    tvDetails.text = "${w.temperature}°C · Wind ${w.windspeed} km/h"
                }
                is WeatherState.Error -> {
                    tvCondition.text = "Weather unavailable"
                    tvDetails.text = state.message
                }
                else -> Unit
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun enableMapDragInsideScroll() {
        mapView?.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> v.parent.requestDisallowInterceptTouchEvent(true)
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> v.parent.requestDisallowInterceptTouchEvent(false)
            }
            false
        }
    }

    private fun showQrDialog(incidentId: String) {
        val payload = QrHelper.buildIncidentPayload(incidentId)
        val bitmap = QrHelper.generateQrBitmap(payload)

        val imageView = android.widget.ImageView(requireContext()).apply {
            setImageBitmap(bitmap)
            val padding = (24 * resources.displayMetrics.density).toInt()
            setPadding(padding, padding, padding, padding)
        }

        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Incident QR Code")
            .setMessage("Scan this from the Incidents list to pull up this record instantly.")
            .setView(imageView)
            .setPositiveButton("Close", null)
            .show()
    }

    // MapView requires forwarding the full fragment lifecycle to work correctly.
    override fun onStart() {
        super.onStart()
        mapView?.onStart()
    }

    override fun onResume() {
        super.onResume()
        mapView?.onResume()
    }

    override fun onPause() {
        mapView?.onPause()
        super.onPause()
    }

    override fun onStop() {
        mapView?.onStop()
        super.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView?.onSaveInstanceState(outState)
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView?.onLowMemory()
    }

    override fun onDestroyView() {
        mapView?.onDestroy()
        mapView = null
        mapLibreMap = null
        mapStyle = null
        mapLoaded = false
        super.onDestroyView()
    }
}
