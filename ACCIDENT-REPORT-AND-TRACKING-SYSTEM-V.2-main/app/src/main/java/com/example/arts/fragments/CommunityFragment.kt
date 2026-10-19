package com.example.arts.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.arts.R
import com.example.arts.utils.TimeFormat
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class CommunityFragment : Fragment() {
    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View =
        i.inflate(R.layout.fragment_community, c, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.findViewById<View>(R.id.btn_back).setOnClickListener { findNavController().navigateUp() }
        val container = view.findViewById<LinearLayout>(R.id.feed_container)
        val tvEmpty = view.findViewById<TextView>(R.id.tv_no_feed)
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val docs = FirebaseFirestore.getInstance().collection("incidents")
                    .orderBy("createdAt", Query.Direction.DESCENDING).limit(20).get().await()
                if (docs.isEmpty) { tvEmpty.visibility = View.VISIBLE; return@launch }
                tvEmpty.visibility = View.GONE
                docs.documents.forEach { doc ->
                    val title = doc.getString("title") ?: return@forEach
                    val type = doc.getString("type") ?: ""
                    val priority = doc.getString("priority") ?: ""
                    val location = doc.getString("location") ?: ""
                    val createdAt = doc.getString("createdAt") ?: ""
                    val card = LayoutInflater.from(requireContext()).inflate(R.layout.item_feed_card, container, false)
                    card.findViewById<TextView>(R.id.tv_feed_title).text = title
                    card.findViewById<TextView>(R.id.tv_feed_type).text = "$type - $priority priority"
                    card.findViewById<TextView>(R.id.tv_feed_location).text = location
                    card.findViewById<TextView>(R.id.tv_feed_time).text = TimeFormat.ago(createdAt)
                    container.addView(card)
                }
            } catch (e: Exception) {
                tvEmpty.text = "Could not load feed: ${e.message}"; tvEmpty.visibility = View.VISIBLE
            }
        }
    }
}
