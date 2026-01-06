package com.example.examen

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

class LocationStorage(private val context: Context) {

    private val fileName = "location_history.json"
    private val gson = Gson()

    // Guardar ubicación
    fun saveLocation(location: LocationData) {
        val locations = getAllLocations().toMutableList()
        locations.add(location)
        saveAllLocations(locations)
    }

    // Obtener todas las ubicaciones
    fun getAllLocations(): List<LocationData> {
        return try {
            val file = File(context.filesDir, fileName)
            if (!file.exists()) {
                return emptyList()
            }
            val json = file.readText()
            val type = object : TypeToken<List<LocationData>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    // Guardar todas las ubicaciones
    private fun saveAllLocations(locations:  List<LocationData>) {
        try {
            val file = File(context.filesDir, fileName)
            val json = gson.toJson(locations)
            file.writeText(json)
        } catch (e:  Exception) {
            e.printStackTrace()
        }
    }

    // Limpiar historial
    fun clearHistory() {
        try {
            val file = File(context.filesDir, fileName)
            if (file.exists()) {
                file.delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Obtener la última ubicación
    fun getLastLocation(): LocationData? {
        return getAllLocations().lastOrNull()
    }
}