package com.example.arts.fragments

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.arts.R
import com.example.arts.data.ReportDraft
import com.example.arts.utils.AiAssistant
import com.example.arts.utils.GeocodeHelper
import com.example.arts.utils.LocationHelper
import com.example.arts.utils.toast
import com.example.arts.viewmodels.IncidentViewModel
import kotlinx.coroutines.launch
import java.util.Locale

class VoiceReportFragment : Fragment() {
    private val incidentViewModel: IncidentViewModel by viewModels()
    private var recognizer: SpeechRecognizer? = null
    private var isListening = false
    private var transcribed = ""

    private val micPerm = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startListening() else toast("Microphone permission needed")
    }

    override fun onCreateView(inflater: LayoutInflater, c: ViewGroup?, s: Bundle?): View =
        inflater.inflate(R.layout.fragment_voice_report, c, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.findViewById<View>(R.id.btn_back)?.setOnClickListener { findNavController().navigateUp() }
        val tvStatus = view.findViewById<TextView>(R.id.tv_voice_status)
        val tvTranscript = view.findViewById<TextView>(R.id.tv_transcript)
        val btnRecord = view.findViewById<Button>(R.id.btn_record)
        val btnSubmit = view.findViewById<Button>(R.id.btn_submit_voice)

        btnRecord?.setOnClickListener {
            if (isListening) stopListening(tvStatus, btnRecord)
            else {
                if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
                    startListening()
                else micPerm.launch(Manifest.permission.RECORD_AUDIO)
            }
        }

        recognizer = SpeechRecognizer.createSpeechRecognizer(requireContext()).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(p: Bundle?) { isListening = true; tvStatus?.text = "Listening... speak now"; btnRecord?.text = "Stop" }
                override fun onResults(r: Bundle?) {
                    val text = r?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull() ?: return
                    transcribed = text
                    tvTranscript?.text = text
                    tvStatus?.text = "Tap Submit to send or Record to try again"
                    btnRecord?.text = "Record again"
                    isListening = false
                }
                override fun onError(code: Int) { tvStatus?.text = "Did not catch that. Tap Record to try again."; isListening = false; btnRecord?.text = "Record" }
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(r: Float) {}
                override fun onBufferReceived(b: ByteArray?) {}
                override fun onEndOfSpeech() { tvStatus?.text = "Processing..." }
                override fun onPartialResults(p: Bundle?) {
                    val partial = p?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull() ?: return
                    tvTranscript?.text = partial
                }
                override fun onEvent(e: Int, p: Bundle?) {}
            })
        }

        btnSubmit?.setOnClickListener {
            if (transcribed.isBlank()) { toast("Record something first"); return@setOnClickListener }
            btnSubmit.isEnabled = false
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val suggestion = AiAssistant.analyzeIncident(requireContext(), transcribed)
                    val location = LocationHelper.currentLocation(requireContext())
                    val locationText = if (location != null) GeocodeHelper.reverseGeocode(requireContext(), location.latitude, location.longitude) ?: "${location.latitude}, ${location.longitude}" else "Location unavailable"
                    incidentViewModel.submitReport(ReportDraft(
                        type = if (suggestion.type == "Pedestrian") "Pedestrian Incident" else suggestion.type,
                        description = suggestion.refinedDescription,
                        priority = suggestion.priority,
                        location = locationText,
                        latitude = location?.latitude,
                        longitude = location?.longitude
                    ))
                    toast("Voice report submitted")
                    findNavController().navigateUp()
                } catch (e: Exception) {
                    toast("Failed to submit: ${e.message}")
                    btnSubmit.isEnabled = true
                }
            }
        }
    }

    private fun startListening() {
        recognizer?.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        })
    }

    private fun stopListening(status: TextView?, btn: Button?) {
        recognizer?.stopListening(); isListening = false; status?.text = "Processing..."; btn?.text = "Record"
    }

    override fun onDestroyView() { super.onDestroyView(); recognizer?.destroy(); recognizer = null }
}
