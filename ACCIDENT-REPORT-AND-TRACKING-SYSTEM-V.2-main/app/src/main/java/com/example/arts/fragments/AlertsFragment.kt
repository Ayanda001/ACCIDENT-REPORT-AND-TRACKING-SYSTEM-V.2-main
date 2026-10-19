package com.example.arts.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.arts.R
import com.example.arts.adapters.NotificationsAdapter
import com.example.arts.viewmodels.NotificationViewModel

class AlertsFragment : Fragment() {

    private val notificationViewModel: NotificationViewModel by viewModels()

    private lateinit var rvNotifications: RecyclerView
    private lateinit var btnBack: ImageView
    private lateinit var tvBadgeCount: TextView
    private lateinit var adapter: NotificationsAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_alerts, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rvNotifications = view.findViewById(R.id.rv_notifications)
        btnBack = view.findViewById(R.id.btn_back)
        tvBadgeCount = view.findViewById(R.id.tv_badge_count)

        btnBack.setOnClickListener { findNavController().navigateUp() }

        adapter = NotificationsAdapter(emptyList()) { notification ->
            notificationViewModel.markAsRead(notification.id)
        }
        rvNotifications.layoutManager = LinearLayoutManager(requireContext())
        rvNotifications.adapter = adapter

        // The signed-in person's alerts, updating live from Firestore
        notificationViewModel.allNotifications.observe(viewLifecycleOwner) { notifications ->
            adapter.updateList(notifications)
        }

        // Observe unread count for badge
        notificationViewModel.unreadCount.observe(viewLifecycleOwner) { count ->
            tvBadgeCount.text = count.toString()
            tvBadgeCount.visibility = if (count > 0) View.VISIBLE else View.GONE
        }
    }
}
