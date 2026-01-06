package com.example.examen

data class LocationData(
    val latitude: Double,
    val longitude:  Double,
    val timestamp: Long,
    val accuracy: Float
) {
    fun getFormattedDate(): String {
        val sdf = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss", java.util. Locale.getDefault())
        return sdf. format(java.util.Date(timestamp))
    }
}