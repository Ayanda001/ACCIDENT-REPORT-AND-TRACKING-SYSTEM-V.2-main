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
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.arts.R
import com.example.arts.adapters.ChatAdapter
import com.example.arts.data.ChatMessage
import com.example.arts.data.ReportDraft
import com.example.arts.utils.AiAssistant
import com.example.arts.utils.GeocodeHelper
import com.example.arts.utils.LocationHelper
import com.example.arts.utils.toast
import com.example.arts.viewmodels.IncidentViewModel
import kotlinx.coroutines.launch
import java.util.Locale

class AiAssistantFragment : Fragment() {

    private val incidentViewModel: IncidentViewModel by viewModels()
    private lateinit var adapter: ChatAdapter
    private var rvChat: RecyclerView? = null
    private var speechRecognizer: SpeechRecognizer? = null

    private val micPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startListening() else toast("Microphone permission is needed for voice input")
    }

    // Words that trigger the SOS suggestion
    private val emergencyWords = listOf("fire", "trapped", "unconscious", "breathing", "bleeding",
        "dying", "dead", "pinned", "explosion", "critical", "emergency", "help")

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.fragment_ai_assistant, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<View>(R.id.btn_back)?.setOnClickListener { findNavController().navigateUp() }

        rvChat = view.findViewById(R.id.rv_chat)
        adapter = ChatAdapter()
        rvChat?.layoutManager = LinearLayoutManager(requireContext()).apply { stackFromEnd = true }
        rvChat?.adapter = adapter

        // Welcome message
        if (adapter.itemCount == 0) {
            addAiMessage("Hello! I'm your ARTS AI Assistant. How can I help you today?\n\nYou can describe an accident, ask about road safety, or report an emergency.")
        }

        view.findViewById<View>(R.id.btn_voice_input)?.setOnClickListener { requestMicAndListen() }
        view.findViewById<View>(R.id.btn_send)?.setOnClickListener { sendMessage() }
        
        view.findViewById<EditText>(R.id.et_message)?.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendMessage()
                true
            } else false
        }
        
        view.findViewById<View>(R.id.btn_send_panic)?.setOnClickListener { sendEmergencyFromAi() }
    }

    private fun sendMessage() {
        val et = view?.findViewById<EditText>(R.id.et_message) ?: return
        val text = et.text.toString().trim()
        if (text.isEmpty()) return
        
        et.text.clear()
        addUserMessage(text)
        
        // UI Check for emergency
        if (emergencyWords.any { text.lowercase().contains(it) }) {
            view?.findViewById<View>(R.id.card_panic_suggestion)?.visibility = View.VISIBLE
        }

        setThinking(true)
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val suggestion = AiAssistant.analyzeIncident(requireContext(), text)
                
                val reply = buildString {
                    append(suggestion.refinedDescription)
                    if (suggestion.priority == "High") {
                        append("\n\nThis looks like a High Priority incident. I recommend sending an SOS immediately.")
                        view?.findViewById<View>(R.id.card_panic_suggestion)?.visibility = View.VISIBLE
                    } else {
                        append("\n\nType: ${suggestion.type} | Priority: ${suggestion.priority}")
                    }
                }
                addAiMessage(reply)
            } catch (e: Exception) {
                addAiMessage("I'm having trouble connecting to my brain right now. If this is an emergency, please call 112.")
            } finally {
                setThinking(false)
            }
        }
    }

    private fun addUserMessage(text: String) {
        adapter.add(ChatMessage(text, fromUser = true, time = currentTime()))
        rvChat?.scrollToPosition(adapter.itemCount - 1)
    }

    private fun addAiMessage(text: String) {
        adapter.add(ChatMessage(text, fromUser = false, time = currentTime()))
        rvChat?.scrollToPosition(adapter.itemCount - 1)
    }

    private fun setThinking(show: Boolean) {
        view?.findViewById<View>(R.id.layout_thinking)?.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun sendEmergencyFromAi() {
        val context = context ?: return
        view?.findViewById<View>(R.id.card_panic_suggestion)?.visibility = View.GONE
        addAiMessage("Initiating emergency report. Locating you now...")
        
        viewLifecycleOwner.lifecycleScope.launch {
            val location = LocationHelper.currentLocation(context)
            if (location == null) {
                addAiMessage("Could not get your GPS location. Please call 112 directly.")
                return@launch
            }
            
            val address = GeocodeHelper.reverseGeocode(context, location.latitude, location.longitude)
                ?: "${location.latitude}, ${location.longitude}"
                
            val details = adapter.getLastUserMessage() ?: "Emergency SOS via AI Assistant"
            
            incidentViewModel.submitReport(
                ReportDraft(
                    type = "Emergency",
                    description = details,
                    priority = "High",
                    location = address,
                    latitude = location.latitude,
                    longitude = location.longitude,
                    panic = true
                )
            )
            addAiMessage("SOS Sent! Help is being dispatched to $address. Please stay calm and stay on the line if emergency services call you.")
        }
    }

    private fun requestMicAndListen() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            startListening()
        } else {
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun startListening() {
        val context = context ?: return
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            toast("Voice recognition is not available on this device")
            return
        }
        
        toast("Listening...")
        speechRecognizer?.destroy()
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onResults(results: Bundle?) {
                    val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull() ?: return
                    view?.findViewById<EditText>(R.id.et_message)?.setText(text)
                }
                override fun onError(error: Int) { toast("Voice input timed out") }
                override fun onReadyForSpeech(params: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}
                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
            startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            })
        }
    }

    private fun currentTime(): String {
        return java.text.SimpleDateFormat("HH:mm", Locale.getDefault()).format(java.util.Date())
    }

    override fun onDestroyView() {
        speechRecognizer?.destroy()
        speechRecognizer = null
        rvChat = null
        super.onDestroyView()
    }
}
