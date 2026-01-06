package com.example.examen

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class LocationAdapter(private val locations: List<LocationData>) :
    RecyclerView. Adapter<LocationAdapter.LocationViewHolder>() {

    class LocationViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvCoordinates: TextView = view.findViewById(R.id.tvCoordinates)
        val tvTimestamp:  TextView = view.findViewById(R. id.tvTimestamp)
        val tvAccuracy: TextView = view.findViewById(R.id.tvAccuracy)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LocationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_location, parent, false)
        return LocationViewHolder(view)
    }

    override fun onBindViewHolder(holder:  LocationViewHolder, position: Int) {
        val location = locations[position]
        holder.tvCoordinates.text = "Lat: ${String.format("%.6f", location.latitude)}, " +
                "Lng: ${String.format("%.6f", location.longitude)}"
        holder.tvTimestamp.text = location.getFormattedDate()
        holder.tvAccuracy.text = "Precisión: ${String.format("%.2f", location.accuracy)} m"
    }

    override fun getItemCount() = locations.size
}