package com.example.arts.fragments

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.arts.R

/** In-app help: common questions and answers, grouped by topic. Tap a question to open its answer. */
class HelpFragment : Fragment() {

    private val faq: List<Pair<String, List<Pair<String, String>>>> = listOf(
        "Getting started" to listOf(
            "How do I report an accident?" to "Tap Report on the Home screen. Choose the type and priority, describe what happened, and pin the exact spot on the map. You can add a photo too. Then tap Submit. You can follow it under Incidents.",
            "What does the Emergency button do?" to "On the Home screen, Emergency lets you call 112 or send a panic report. A panic report shares your current location as a High priority emergency and notifies ambulance and police straight away. The app asks for location permission the first time.",
            "Does ARTS replace calling the emergency services?" to "No. If someone is in danger, call 112 (any cellphone), 10111 (police) or 10177 (ambulance) first. ARTS helps by sending your location and details to the services we work with."
        ),
        "Reports and alerts" to listOf(
            "How does ARTS decide who to notify?" to "A report is urgent if it is marked High priority, or if the description has two or more warning words such as fire, trapped or unconscious. Urgent reports go automatically to the matching active services. Everything else waits for an administrator to review it.",
            "What do the statuses mean?" to "Pending: not reviewed yet. Forwarded: sent to emergency services. Resolved: dealt with.",
            "Where do I see updates?" to "Open the Alerts tab for messages about your reports, or the Incidents tab and tap a report to see its status, map, weather and QR code."
        ),
        "Your information" to listOf(
            "Why add medical information?" to "If you cannot speak for yourself after an accident, responders can see your blood type, allergies, medication, medical aid and emergency contact. Only you and ARTS administrators can see it. Find it under Profile, Medical Information.",
            "How do I change my details or photo?" to "Open Profile, then Settings. You can change your name, phone number and photo. Your email is locked because it is your sign-in."
        ),
        "Signing in and security" to listOf(
            "Can I sign in with Google?" to "Yes. Tap Continue with Google on the sign-in screen. The first time, it creates your ARTS account.",
            "How do I turn on two-factor login?" to "Open Profile, then Security, then Two-Factor Authentication. Add the key to Google Authenticator or Authy, then enter the 6-digit code it shows. After that you will be asked for a code each time you open ARTS.",
            "What is biometric unlock?" to "When it is on, ARTS asks for your fingerprint or face before opening an account that is already signed in on this phone. Turn it on under Profile, Security.",
            "I forgot my password." to "On the sign-in screen, type your email address and tap Forgot password. A reset link is emailed to you."
        ),
        "Troubleshooting" to listOf(
            "The map or weather is missing on a report." to "That report was sent without a pinned location. Tap the map when you report next time so the spot and its weather are saved.",
            "The panic report says it cannot get my location." to "Turn on location services on your phone and allow ARTS to use your location, then try again.",
            "I have no signal." to "A report you send while offline is saved on your phone and sent automatically when your connection returns. Call 112 if it is urgent."
        )
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_help, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<View>(R.id.btn_back).setOnClickListener { findNavController().navigateUp() }

        val container = view.findViewById<LinearLayout>(R.id.faq_container)
        faq.forEach { (group, items) ->
            container.addView(groupTitle(group))
            items.forEach { (question, answer) -> container.addView(faqCard(question, answer)) }
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun groupTitle(text: String) = TextView(requireContext()).apply {
        this.text = text.uppercase()
        setTextColor(resources.getColor(R.color.accent_green, null))
        textSize = 12f
        setTypeface(typeface, android.graphics.Typeface.BOLD)
        letterSpacing = 0.08f
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            .apply { setMargins(0, dp(20), 0, dp(8)) }
    }

    /** One tappable question that expands to show its answer. */
    private fun faqCard(question: String, answer: String): View {
        val card = CardView(requireContext()).apply {
            radius = dp(12).toFloat()
            cardElevation = 0f
            setCardBackgroundColor(resources.getColor(R.color.bg_card, null))
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                .apply { setMargins(0, 0, 0, dp(8)) }
        }

        val column = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
        }

        val row = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val questionView = TextView(requireContext()).apply {
            text = question
            setTextColor(resources.getColor(R.color.text_primary, null))
            textSize = 14.5f
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        val marker = TextView(requireContext()).apply {
            text = "+"
            setTextColor(resources.getColor(R.color.accent_green, null))
            textSize = 20f
        }
        row.addView(questionView)
        row.addView(marker)

        val answerView = TextView(requireContext()).apply {
            text = answer
            setTextColor(resources.getColor(R.color.text_secondary, null))
            textSize = 13.5f
            setLineSpacing(dp(2).toFloat(), 1f)
            visibility = View.GONE
            setPadding(0, dp(10), 0, 0)
        }

        column.addView(row)
        column.addView(answerView)
        card.addView(column)

        card.isClickable = true
        card.setOnClickListener {
            val open = answerView.visibility == View.VISIBLE
            answerView.visibility = if (open) View.GONE else View.VISIBLE
            marker.text = if (open) "+" else "\u2212"
        }
        return card
    }
}
