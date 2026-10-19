package com.example.arts.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.arts.R
import com.example.arts.data.Incident

class RecentIncidentsAdapter(
    private var incidents: List<Incident>
) : RecyclerView.Adapter<RecentIncidentsAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTitle: TextView = itemView.findViewById(R.id.tv_title)
        val tvTime: TextView = itemView.findViewById(R.id.tv_time)
        val tvStatus: TextView = itemView.findViewById(R.id.tv_status)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_incident_small, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val incident = incidents[position]
        val ctx = holder.itemView.context

        holder.tvTitle.text = incident.title
        holder.tvTime.text = incident.timeAgo
        holder.tvStatus.text = incident.status

        when (incident.status) {
            "Forwarded" -> {
                holder.tvStatus.setTextColor(ContextCompat.getColor(ctx, R.color.status_in_progress))
                holder.tvStatus.setBackgroundResource(R.drawable.bg_badge_blue)
            }
            "Pending" -> {
                holder.tvStatus.setTextColor(ContextCompat.getColor(ctx, R.color.status_pending))
                holder.tvStatus.setBackgroundResource(R.drawable.bg_badge_yellow)
            }
            "Resolved" -> {
                holder.tvStatus.setTextColor(ContextCompat.getColor(ctx, R.color.status_resolved))
                holder.tvStatus.setBackgroundResource(R.drawable.bg_badge_green)
            }
        }
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
