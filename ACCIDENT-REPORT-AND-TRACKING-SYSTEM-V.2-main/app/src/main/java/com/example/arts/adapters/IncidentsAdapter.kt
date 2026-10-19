package com.example.arts.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.arts.R
import com.example.arts.data.Incident

class IncidentsAdapter(
    private var incidents: List<Incident>,
    private val onItemClick: (Incident) -> Unit
) : RecyclerView.Adapter<IncidentsAdapter.IncidentViewHolder>() {

    inner class IncidentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTitle: TextView = itemView.findViewById(R.id.tv_title)
        val tvStatus: TextView = itemView.findViewById(R.id.tv_status)
        val tvLocation: TextView = itemView.findViewById(R.id.tv_location)
        val tvTime: TextView = itemView.findViewById(R.id.tv_time)
        val tvType: TextView = itemView.findViewById(R.id.tv_type)
        val tvPriority: TextView = itemView.findViewById(R.id.tv_priority)
        val progressStatus: ProgressBar = itemView.findViewById(R.id.progress_status)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IncidentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_incident, parent, false)
        return IncidentViewHolder(view)
    }

    override fun onBindViewHolder(holder: IncidentViewHolder, position: Int) {
        val incident = incidents[position]
        val ctx = holder.itemView.context

        holder.tvTitle.text = incident.title
        holder.tvLocation.text = incident.location
        holder.tvTime.text = incident.timeAgo
        holder.tvType.text = incident.type
        holder.tvPriority.text = incident.priority
        holder.progressStatus.progress = incident.progressPercent

        when (incident.status) {
            "Forwarded" -> {
                holder.tvStatus.text = "Forwarded"
                holder.tvStatus.setTextColor(ContextCompat.getColor(ctx, R.color.status_in_progress))
                holder.tvStatus.setBackgroundResource(R.drawable.bg_badge_blue)
            }
            "Pending" -> {
                holder.tvStatus.text = "Pending"
                holder.tvStatus.setTextColor(ContextCompat.getColor(ctx, R.color.status_pending))
                holder.tvStatus.setBackgroundResource(R.drawable.bg_badge_yellow)
            }
            "Resolved" -> {
                holder.tvStatus.text = "Resolved"
                holder.tvStatus.setTextColor(ContextCompat.getColor(ctx, R.color.status_resolved))
                holder.tvStatus.setBackgroundResource(R.drawable.bg_badge_green)
            }
        }

        when (incident.priority) {
            "High" -> {
                holder.tvPriority.setTextColor(ContextCompat.getColor(ctx, R.color.priority_high))
                holder.tvPriority.setBackgroundResource(R.drawable.bg_badge_red)
            }
            "Medium" -> {
                holder.tvPriority.setTextColor(ContextCompat.getColor(ctx, R.color.priority_medium))
                holder.tvPriority.setBackgroundResource(R.drawable.bg_badge_yellow)
            }
            "Low" -> {
                holder.tvPriority.setTextColor(ContextCompat.getColor(ctx, R.color.priority_low))
                holder.tvPriority.setBackgroundResource(R.drawable.bg_badge_green)
            }
        }

        holder.itemView.setOnClickListener { onItemClick(incident) }
    }

    override fun getItemCount() = incidents.size

    fun updateList(newList: List<Incident>) {
        val diff = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize() = incidents.size
            override fun getNewListSize() = newList.size
            override fun areItemsTheSame(oldPos: Int, newPos: Int) =
                incidents[oldPos].id == newList[newPos].id
            override fun areContentsTheSame(oldPos: Int, newPos: Int) =
                incidents[oldPos] == newList[newPos]
        })
        incidents = newList
        diff.dispatchUpdatesTo(this)
    }
}
