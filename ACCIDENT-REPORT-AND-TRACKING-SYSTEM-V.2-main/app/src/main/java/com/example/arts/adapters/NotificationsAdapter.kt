package com.example.arts.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.arts.R
import com.example.arts.data.AppAlert

class NotificationsAdapter(
    private var notifications: List<AppAlert>,
    private val onItemClick: (AppAlert) -> Unit = {}
) : RecyclerView.Adapter<NotificationsAdapter.NotificationViewHolder>() {

    inner class NotificationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val iconBg: FrameLayout = itemView.findViewById(R.id.icon_bg)
        val ivIcon: ImageView = itemView.findViewById(R.id.iv_icon)
        val tvTitle: TextView = itemView.findViewById(R.id.tv_title)
        val tvMessage: TextView = itemView.findViewById(R.id.tv_message)
        val tvTime: TextView = itemView.findViewById(R.id.tv_time)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notification, parent, false)
        return NotificationViewHolder(view)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        val notification = notifications[position]
        val ctx = holder.itemView.context

        holder.tvTitle.text = notification.title
        holder.tvMessage.text = notification.message
        holder.tvTime.text = notification.timeAgo

        // Dim if read
        holder.itemView.alpha = if (notification.isRead) 0.6f else 1.0f

        when (notification.type) {
            "EMERGENCY" -> {
                holder.iconBg.setBackgroundResource(R.drawable.bg_badge_red)
                holder.ivIcon.setImageResource(R.drawable.ic_warning)
                holder.ivIcon.setColorFilter(ContextCompat.getColor(ctx, R.color.status_emergency))
            }
            "INCIDENT_UPDATE" -> {
                holder.iconBg.setBackgroundResource(R.drawable.bg_badge_blue)
                holder.ivIcon.setImageResource(R.drawable.ic_incidents)
                holder.ivIcon.setColorFilter(ContextCompat.getColor(ctx, R.color.accent_green))
            }
            "INCIDENT_RESOLVED" -> {
                holder.iconBg.setBackgroundResource(R.drawable.bg_badge_green)
                holder.ivIcon.setImageResource(R.drawable.ic_alerts)
                holder.ivIcon.setColorFilter(ContextCompat.getColor(ctx, R.color.status_resolved))
            }
            "SAFETY_TIP" -> {
                holder.iconBg.setBackgroundResource(R.drawable.bg_badge_yellow)
                holder.ivIcon.setImageResource(R.drawable.ic_warning)
                holder.ivIcon.setColorFilter(ContextCompat.getColor(ctx, R.color.status_pending))
            }
            "ROAD_CLOSURE" -> {
                holder.iconBg.setBackgroundResource(R.drawable.bg_badge_red)
                holder.ivIcon.setImageResource(R.drawable.ic_warning)
                holder.ivIcon.setColorFilter(ContextCompat.getColor(ctx, R.color.status_emergency))
            }
        }

        holder.itemView.setOnClickListener { onItemClick(notification) }
    }

    override fun getItemCount() = notifications.size

    fun updateList(newList: List<AppAlert>) {
        val diff = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize() = notifications.size
            override fun getNewListSize() = newList.size
            override fun areItemsTheSame(oldPos: Int, newPos: Int) =
                notifications[oldPos].id == newList[newPos].id
            override fun areContentsTheSame(oldPos: Int, newPos: Int) =
                notifications[oldPos] == newList[newPos]
        })
        notifications = newList
        diff.dispatchUpdatesTo(this)
    }
}
