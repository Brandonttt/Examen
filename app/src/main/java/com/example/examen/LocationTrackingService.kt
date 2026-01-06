package com.example.examen

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*

class LocationTrackingService : Service() {

    private lateinit var fusedLocationClient:  FusedLocationProviderClient
    private lateinit var locationCallback:  LocationCallback
    private lateinit var locationStorage: LocationStorage
    private var updateInterval: Long = 10000L
    private var showNotification:  Boolean = true

    companion object {
        const val CHANNEL_ID = "LocationTrackingChannel"
        const val NOTIFICATION_ID = 1
        const val ACTION_START = "START_TRACKING"
        const val ACTION_STOP = "STOP_TRACKING"
        const val EXTRA_INTERVAL = "INTERVAL"
        const val EXTRA_SHOW_NOTIFICATION = "SHOW_NOTIFICATION"
    }

    override fun onCreate() {
        super.onCreate()
        Log.d("LocationService", "onCreate")
        locationStorage = LocationStorage(this)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("LocationService", "onStartCommand:  ${intent?.action}")

        when (intent?.action) {
            ACTION_START -> {
                updateInterval = intent.getLongExtra(EXTRA_INTERVAL, 10000L)
                showNotification = intent.getBooleanExtra(EXTRA_SHOW_NOTIFICATION, true)
                Log.d("LocationService", "Starting with interval: $updateInterval ms")
                startLocationUpdates()
            }
            ACTION_STOP -> {
                Log.d("LocationService", "Stopping service")
                stopLocationUpdates()
                stopSelf()
            }
        }
        return START_STICKY
    }

    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            updateInterval
        ).apply {
            setMinUpdateIntervalMillis(updateInterval / 2)
            setWaitForAccurateLocation(false)
        }.build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    Log.d("LocationService", "Nueva ubicación:  Lat=${location.latitude}, Lng=${location.longitude}")

                    saveLocation(location)
                    updateNotification(location)
                    sendBroadcastUpdate(location)
                }
            }
        }

        if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )

            if (showNotification) {
                startForeground(NOTIFICATION_ID, createNotification(null))
            }

            Log.d("LocationService", "Location updates iniciados")
        } else {
            Log.e("LocationService", "No hay permisos de ubicación")
        }
    }

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
        if (showNotification) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        }
        Log.d("LocationService", "Location updates detenidos")
    }

    private fun saveLocation(location: Location) {
        val locationData = LocationData(
            latitude = location.latitude,
            longitude = location.longitude,
            timestamp = System.currentTimeMillis(),
            accuracy = location.accuracy
        )
        locationStorage.saveLocation(locationData)
        Log.d("LocationService", "Ubicación guardada")
    }

    private fun sendBroadcastUpdate(location:  Location) {
        val intent = Intent("LOCATION_UPDATE").apply {
            putExtra("latitude", location.latitude)
            putExtra("longitude", location.longitude)
            putExtra("accuracy", location.accuracy)

            // IMPORTANTE: Agregar el paquete para que el broadcast sea explícito
            setPackage(packageName)
        }

        sendBroadcast(intent)
        Log.d("LocationService", "Broadcast enviado:  Lat=${location.latitude}, Lng=${location.longitude}")
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES. O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Rastreo de Ubicación",
                NotificationManager. IMPORTANCE_LOW
            ).apply {
                description = "Notificación de rastreo de ubicación en segundo plano"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager. createNotificationChannel(channel)
            Log.d("LocationService", "Canal de notificación creado")
        }
    }

    private fun createNotification(location: Location? ): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val contentText = if (location != null) {
            "Lat: ${String.format("%.6f", location.latitude)}, " +
                    "Lng: ${String.format("%.6f", location.longitude)}"
        } else {
            "Rastreando ubicación..."
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("🔴 Rastreo Activo")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification(location: Location) {
        if (showNotification) {
            val manager = getSystemService(NotificationManager::class.java)
            manager. notify(NOTIFICATION_ID, createNotification(location))
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        Log.d("LocationService", "onDestroy")
    }
}