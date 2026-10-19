package com.example.arts.fragments

import android.Manifest
import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.arts.R
import com.example.arts.data.ReportDraft
import com.example.arts.utils.AiAssistant
import com.example.arts.utils.GeocodeHelper
import com.example.arts.utils.ImageStorageHelper
import com.example.arts.utils.MapHelper
import com.example.arts.utils.toast
import com.example.arts.viewmodels.IncidentViewModel
import kotlinx.coroutines.launch
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style

class ReportAccidentFragment : Fragment() {

    private val incidentViewModel: IncidentViewModel by viewModels()

    private var selectedType = "Collision"
    private var selectedPriority = "High"
    private var reportPhotoDataUrl: String = ""

    private var selectedLat: Double? = null
    private var selectedLng: Double? = null

    private var mapView: MapView? = null
    private var mapLibreMap: MapLibreMap? = null
    private var mapStyle: Style? = null
    private var addressAutoFilled = false
    private var fillingAddress = false
    private val handler = Handler(Looper.getMainLooper())

    private val locationPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            enableLiveLocation()
            useMyLocation(announce = false)
            scheduleAutoPin(attemptsLeft = 5)
        } else {
            toast("Location is off. You can still tap the map to place the pin.")
        }
    }

    private val pickPhotoLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@registerForActivityResult
        val ctx = context ?: return@registerForActivityResult
        val dataUrl = ImageStorageHelper.toDataUrl(ctx, uri)
        if (dataUrl == null) {
            toast("Couldn't read that image, please try another")
            return@registerForActivityResult
        }
        reportPhotoDataUrl = dataUrl
        val bitmap = ImageStorageHelper.decodeDataUrl(dataUrl)
        view?.findViewById<ImageView>(R.id.iv_report_photo)?.apply {
            setImageBitmap(bitmap)
            visibility = View.VISIBLE
        }
        view?.findViewById<View>(R.id.layout_add_photo_prompt)?.visibility = View.GONE
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        MapLibre.getInstance(requireContext())
        return inflater.inflate(R.layout.fragment_report_accident, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mapView = view.findViewById(R.id.map_pick_location)
        mapView?.onCreate(savedInstanceState)
        setupMap()

        view.findViewById<View>(R.id.card_add_photo)?.setOnClickListener {
            pickPhotoLauncher.launch("image/*")
        }

        view.findViewById<ImageView>(R.id.btn_back)?.setOnClickListener {
            findNavController().navigateUp()
        }

        val typeCollision = view.findViewById<TextView>(R.id.type_collision)
        val typePedestrian = view.findViewById<TextView>(R.id.type_pedestrian)
        val typeBreakdown = view.findViewById<TextView>(R.id.type_breakdown)

        typeCollision?.setOnClickListener { selectType("Collision", typeCollision, typePedestrian, typeBreakdown) }
        typePedestrian?.setOnClickListener { selectType("Pedestrian", typePedestrian, typeCollision, typeBreakdown) }
        typeBreakdown?.setOnClickListener { selectType("Breakdown", typeBreakdown, typeCollision, typePedestrian) }

        val priorityHigh = view.findViewById<TextView>(R.id.priority_high)
        val priorityMedium = view.findViewById<TextView>(R.id.priority_medium)
        val priorityLow = view.findViewById<TextView>(R.id.priority_low)

        priorityHigh?.setOnClickListener { selectPriority("High", priorityHigh, priorityMedium, priorityLow) }
        priorityMedium?.setOnClickListener { selectPriority("Medium", priorityMedium, priorityHigh, priorityLow) }
        priorityLow?.setOnClickListener { selectPriority("Low", priorityLow, priorityHigh, priorityMedium) }

        val etDescription = view.findViewById<EditText>(R.id.et_description)
        val etLocation = view.findViewById<EditText>(R.id.et_location)
        val btnAiAssist = view.findViewById<TextView>(R.id.btn_ai_assist)
        
        btnAiAssist?.setOnClickListener {
            val ctx = context ?: return@setOnClickListener
            val raw = etDescription?.text?.toString()?.trim().orEmpty()
            if (raw.length < 10) {
                toast("Write a bit more detail first, then try AI Assist")
                return@setOnClickListener
            }

            btnAiAssist.isEnabled = false
            btnAiAssist.text = "Thinking…"

            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val suggestion = AiAssistant.analyzeIncident(ctx, raw)

                    etDescription?.setText(suggestion.refinedDescription)

                    when (suggestion.type) {
                        "Collision" -> if (typeCollision != null) selectType("Collision", typeCollision, typePedestrian, typeBreakdown)
                        "Pedestrian" -> if (typePedestrian != null) selectType("Pedestrian", typePedestrian, typeCollision, typeBreakdown)
                        "Breakdown" -> if (typeBreakdown != null) selectType("Breakdown", typeBreakdown, typeCollision, typePedestrian)
                    }
                    when (suggestion.priority) {
                        "High" -> if (priorityHigh != null) selectPriority("High", priorityHigh, priorityMedium, priorityLow)
                        "Medium" -> if (priorityMedium != null) selectPriority("Medium", priorityMedium, priorityHigh, priorityLow)
                        "Low" -> if (priorityLow != null) selectPriority("Low", priorityLow, priorityHigh, priorityMedium)
                    }

                    toast("Description updated. Please review before submitting.")
                } catch (e: Exception) {
                    toast("AI Assist unavailable: ${e.message}")
                } finally {
                    btnAiAssist.isEnabled = true
                    btnAiAssist.text = "AI Assist: clean up & suggest priority"
                }
            }
        }

        val btnSubmit = view.findViewById<Button>(R.id.btn_submit_report)

        incidentViewModel.submitResult.observe(viewLifecycleOwner) { result ->
            if (result != null) {
                toast(
                    when {
                        result.queued -> "Saved on this phone. It will be sent when you are back online."
                        result.dispatchedCount > 0 -> "Report submitted. Emergency services notified."
                        else -> "Report submitted successfully."
                    }
                )
                incidentViewModel.clearSubmitResult()
                findNavController().navigateUp()
            }
        }

        incidentViewModel.errorMessage.observe(viewLifecycleOwner) { message ->
            if (message != null) {
                toast(message)
                incidentViewModel.clearError()
                btnSubmit?.isEnabled = true
            }
        }

        btnSubmit?.setOnClickListener {
            val description = etDescription?.text?.toString()?.trim().orEmpty()
            val location = etLocation?.text?.toString()?.trim().orEmpty()

            when {
                location.isEmpty() -> toast("Please enter the location")
                description.isEmpty() -> toast("Please describe the incident")
                else -> {
                    btnSubmit.isEnabled = false
                    submitReport(location, description)
                }
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupMap() {
        val root = view ?: return
        
        mapView?.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> v.parent.requestDisallowInterceptTouchEvent(true)
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> v.parent.requestDisallowInterceptTouchEvent(false)
            }
            false
        }

        root.findViewById<View>(R.id.btn_my_location)?.setOnClickListener { useMyLocation(announce = true) }

        root.findViewById<EditText>(R.id.et_location)?.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(text: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(text: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(editable: android.text.Editable?) {
                if (!fillingAddress) addressAutoFilled = false
            }
        })

        mapView?.getMapAsync { map ->
            if (!isAdded) return@getMapAsync
            mapLibreMap = map
            map.setStyle(MapHelper.osmStyle()) { style ->
                if (!isAdded) return@setStyle
                mapStyle = style

                val lat = selectedLat
                val lng = selectedLng
                if (lat != null && lng != null) {
                    MapHelper.setPin(style, lat, lng)
                    map.cameraPosition = CameraPosition.Builder().target(LatLng(lat, lng)).zoom(15.0).build()
                } else {
                    map.cameraPosition = CameraPosition.Builder()
                        .target(LatLng(MapHelper.DEFAULT_LAT, MapHelper.DEFAULT_LNG))
                        .zoom(10.0)
                        .build()
                }

                map.addOnMapClickListener { point ->
                    placePin(point.latitude, point.longitude, moveCamera = false)
                    true
                }

                if (MapHelper.hasLocationPermission(requireContext())) {
                    enableLiveLocation()
                    if (selectedLat == null) {
                        useMyLocation(announce = false)
                        scheduleAutoPin(attemptsLeft = 5)
                    }
                } else {
                    locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                }
            }
        }
    }

    private fun enableLiveLocation() {
        val map = mapLibreMap ?: return
        val style = mapStyle ?: return
        if (isAdded) MapHelper.enableLiveLocation(requireContext(), map, style)
    }

    private fun scheduleAutoPin(attemptsLeft: Int) {
        if (attemptsLeft <= 0) return
        handler.postDelayed({
            if (isAdded && selectedLat == null) {
                useMyLocation(announce = false)
                scheduleAutoPin(attemptsLeft - 1)
            }
        }, 2_000)
    }

    private fun useMyLocation(announce: Boolean) {
        val map = mapLibreMap ?: return
        if (!isAdded) return
        if (!MapHelper.hasLocationPermission(requireContext())) {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            return
        }
        enableLiveLocation()
        val here = if (map.locationComponent.isLocationComponentActivated) map.locationComponent.lastKnownLocation else null
        if (here == null) {
            if (announce) toast("Still finding your location. Please wait.")
            return
        }
        placePin(here.latitude, here.longitude, moveCamera = true)
    }

    private fun placePin(lat: Double, lng: Double, moveCamera: Boolean) {
        selectedLat = lat
        selectedLng = lng
        mapStyle?.let { MapHelper.setPin(it, lat, lng) }
        if (moveCamera) mapLibreMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(lat, lng), 15.0))
        view?.findViewById<TextView>(R.id.tv_map_hint)?.text = "Pinned at %.4f, %.4f".format(lat, lng)
        fillAddress(lat, lng)
    }

    private fun fillAddress(lat: Double, lng: Double) {
        viewLifecycleOwner.lifecycleScope.launch {
            val address = GeocodeHelper.reverseGeocode(requireContext(), lat, lng) ?: return@launch
            val box = view?.findViewById<EditText>(R.id.et_location) ?: return@launch
            if (box.text.isNullOrBlank() || addressAutoFilled) {
                fillingAddress = true
                box.setText(address)
                fillingAddress = false
                addressAutoFilled = true
            }
        }
    }

    private fun submitReport(location: String, description: String) {
        incidentViewModel.submitReport(
            ReportDraft(
                type = if (selectedType == "Pedestrian") "Pedestrian Incident" else selectedType,
                description = description,
                priority = selectedPriority,
                location = location,
                latitude = selectedLat,
                longitude = selectedLng,
                photoDataUrl = reportPhotoDataUrl
            )
        )
    }

    private fun selectType(type: String, selected: TextView?, vararg others: TextView?) {
        selectedType = type
        selected?.setBackgroundResource(R.drawable.bg_chip_selected)
        selected?.setTextColor(resources.getColor(R.color.white, null))
        others.forEach {
            it?.setBackgroundResource(R.drawable.bg_chip_unselected)
            it?.setTextColor(resources.getColor(R.color.text_secondary, null))
        }
    }

    private fun selectPriority(priority: String, selected: TextView?, vararg others: TextView?) {
        selectedPriority = priority
        val bgRes = when (priority) {
            "High" -> R.drawable.bg_badge_red
            "Medium" -> R.drawable.bg_badge_yellow
            else -> R.drawable.bg_badge_green
        }
        val colorRes = when (priority) {
            "High" -> R.color.priority_high
            "Medium" -> R.color.priority_medium
            else -> R.color.priority_low
        }
        selected?.setBackgroundResource(bgRes)
        selected?.setTextColor(resources.getColor(colorRes, null))
        others.forEach {
            it?.setBackgroundResource(R.drawable.bg_chip_unselected)
            it?.setTextColor(resources.getColor(R.color.text_secondary, null))
        }
    }

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
        handler.removeCallbacksAndMessages(null)
        mapView = null
        mapLibreMap = null
        mapStyle = null
        super.onDestroyView()
    }
}
