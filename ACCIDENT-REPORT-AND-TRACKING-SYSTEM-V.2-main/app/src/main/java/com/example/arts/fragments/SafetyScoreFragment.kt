package com.example.arts.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.arts.R
import com.example.arts.viewmodels.ProfileViewModel

class SafetyScoreFragment : Fragment() {
    private val profileViewModel: ProfileViewModel by viewModels()
    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View =
        i.inflate(R.layout.fragment_safety_score, c, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.findViewById<View>(R.id.btn_back).setOnClickListener { findNavController().navigateUp() }
        profileViewModel.user.observe(viewLifecycleOwner) { user ->
            val score = user?.safetyScore ?: 85
            view.findViewById<TextView>(R.id.tv_score_number).text = score.toString()
            view.findViewById<TextView>(R.id.tv_score_label).text = when {
                score >= 90 -> "Excellent driver"
                score >= 75 -> "Good driver"
                score >= 60 -> "Average"
                else -> "Needs improvement"
            }
            view.findViewById<TextView>(R.id.tv_score_detail).text =
                "Your safety score is based on the number of incidents reported in your area and your reporting history. A higher score means you are in a safer zone with fewer accidents."
        }
        profileViewModel.totalReports.observe(viewLifecycleOwner) { count ->
            view.findViewById<TextView>(R.id.tv_reports_count).text = "$count reports submitted"
        }
    }
}
