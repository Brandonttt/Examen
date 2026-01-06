package com.example.examen

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.examen.databinding.ActivityMainBinding
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var locationStorage: LocationStorage
    private var isTracking = false
    private var currentMarker: Marker? = null
    private var routeLine: Polyline? = null

    private val locationReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.d("MainActivity", "Broadcast recibido")

            val latitude = intent?.getDoubleExtra("latitude", 0.0) ?: 0.0
            val longitude = intent?.getDoubleExtra("longitude", 0.0) ?: 0.0
            val accuracy = intent?.getFloatExtra("accuracy", 0f) ?: 0f

            Log.d("MainActivity", "Lat: $latitude, Lng: $longitude, Acc: $accuracy")

            // Verificar que los valores sean válidos
            if (latitude != 0.0 && longitude != 0.0) {
                updateUI(latitude, longitude, accuracy)
                updateMap(latitude, longitude)
            }
        }
    }

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts. RequestMultiplePermissions()
    ) { permissions ->
        val fineLocation = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocation = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

        if (fineLocation && coarseLocation) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES. TIRAMISU) {
                requestNotificationPermission()
            } else {
                Toast.makeText(this, "Permisos de ubicación concedidos", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Se necesitan permisos de ubicación", Toast.LENGTH_LONG).show()
        }
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(this, "Permiso de notificaciones concedido", Toast.LENGTH_SHORT).show()
            requestBackgroundLocationIfNeeded()
        } else {
            Toast.makeText(this, "Las notificaciones no se mostrarán", Toast.LENGTH_SHORT).show()
        }
    }

    private val backgroundLocationLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(this, "Permiso de ubicación en segundo plano concedido", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "El rastreo en segundo plano puede ser limitado", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        locationStorage = LocationStorage(this)
        setupMap()
        setupListeners()
        requestPermissions()
        loadRoute()

        Log.d("MainActivity", "onCreate completado")
    }

    private fun setupMap() {
        Configuration.getInstance().load(this, getSharedPreferences("osm", MODE_PRIVATE))

        binding.mapView.apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller. setZoom(15.0)
            // Centro de Ciudad de México
            controller.setCenter(GeoPoint(19.4326, -99.1332))
        }
    }

    private fun setupListeners() {
        binding.btnStart.setOnClickListener {
            if (checkAllPermissions()) {
                startTracking()
            } else {
                requestPermissions()
            }
        }

        binding.btnStop. setOnClickListener {
            stopTracking()
        }

        binding.btnHistory.setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }

        binding.btnClear.setOnClickListener {
            showClearConfirmation()
        }
    }

    private fun startTracking() {
        val interval = when (binding.rgInterval.checkedRadioButtonId) {
            R.id. rb10s -> 10000L
            R.id.rb60s -> 60000L
            R.id.rb5m -> 300000L
            else -> 10000L
        }

        val showNotification = binding.switchNotification.isChecked

        val intent = Intent(this, LocationTrackingService::class.java).apply {
            action = LocationTrackingService.ACTION_START
            putExtra(LocationTrackingService.EXTRA_INTERVAL, interval)
            putExtra(LocationTrackingService.EXTRA_SHOW_NOTIFICATION, showNotification)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }

        isTracking = true
        updateTrackingUI()
        Toast.makeText(this, "Rastreo iniciado", Toast. LENGTH_SHORT).show()

        Log.d("MainActivity", "Tracking iniciado")
    }

    private fun stopTracking() {
        val intent = Intent(this, LocationTrackingService::class.java).apply {
            action = LocationTrackingService.ACTION_STOP
        }
        startService(intent)

        isTracking = false
        updateTrackingUI()
        Toast.makeText(this, "Rastreo detenido", Toast.LENGTH_SHORT).show()

        Log.d("MainActivity", "Tracking detenido")
    }

    private fun updateTrackingUI() {
        if (isTracking) {
            binding.btnStart.isEnabled = false
            binding.btnStop.isEnabled = true
            binding.tvStatus.text = "Rastreo activo"
            binding.statusIndicator.setBackgroundColor(
                ContextCompat.getColor(this, android.R.color.holo_green_dark)
            )
            binding.rgInterval.isEnabled = false
        } else {
            binding.btnStart.isEnabled = true
            binding.btnStop.isEnabled = false
            binding.tvStatus. text = "Rastreo inactivo"
            binding.statusIndicator.setBackgroundColor(
                ContextCompat.getColor(this, android.R. color.holo_red_dark)
            )
            binding. rgInterval.isEnabled = true
        }
    }

    private fun updateUI(latitude: Double, longitude: Double, accuracy: Float) {
        runOnUiThread {
            binding.tvLatitude.text = "Latitud: ${String.format("%.6f", latitude)}"
            binding.tvLongitude.text = "Longitud: ${String. format("%.6f", longitude)}"
            binding.tvAccuracy.text = "Precisión: ${String.format("%.2f", accuracy)} metros"

            Log.d("MainActivity", "UI actualizada:  Lat=$latitude, Lng=$longitude")
        }
    }

    private fun updateMap(latitude: Double, longitude: Double) {
        runOnUiThread {
            val geoPoint = GeoPoint(latitude, longitude)

            if (currentMarker == null) {
                currentMarker = Marker(binding.mapView).apply {
                    position = geoPoint
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    title = "Mi ubicación"
                    icon = resources.getDrawable(android.R.drawable.ic_menu_mylocation, null)
                    binding.mapView.overlays.add(this)
                }
                Log.d("MainActivity", "Marcador creado")
            } else {
                currentMarker?. position = geoPoint
                Log.d("MainActivity", "Marcador actualizado")
            }

            binding.mapView.controller.animateTo(geoPoint)
            loadRoute()
            binding.mapView.invalidate()
        }
    }

    private fun loadRoute() {
        val locations = locationStorage.getAllLocations()

        if (locations.isEmpty()) return

        routeLine?.let { binding.mapView.overlays.remove(it) }

        routeLine = Polyline().apply {
            outlinePaint.color = Color. BLUE
            outlinePaint. strokeWidth = 5f

            val points = locations.map { GeoPoint(it.latitude, it.longitude) }
            setPoints(points)

            binding.mapView.overlays.add(this)
        }

        binding.mapView.invalidate()
        Log.d("MainActivity", "Ruta cargada con ${locations.size} puntos")
    }

    private fun showClearConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Limpiar Historial")
            .setMessage("¿Estás seguro de que deseas eliminar todo el historial?")
            .setPositiveButton("Sí") { _, _ ->
                locationStorage.clearHistory()
                routeLine?.let { binding.mapView.overlays.remove(it) }
                routeLine = null
                binding.mapView.invalidate()
                Toast.makeText(this, "Historial limpiado", Toast. LENGTH_SHORT).show()
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun checkAllPermissions(): Boolean {
        val fineLocation = ContextCompat. checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseLocation = ContextCompat.checkSelfPermission(
            this, Manifest.permission. ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES. TIRAMISU) {
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        return fineLocation && coarseLocation && notification
    }

    private fun requestPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        locationPermissionLauncher.launch(permissions. toTypedArray())
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES. TIRAMISU) {
            val hasPermission = ContextCompat. checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasPermission) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                requestBackgroundLocationIfNeeded()
            }
        } else {
            requestBackgroundLocationIfNeeded()
        }
    }

    private fun requestBackgroundLocationIfNeeded() {
        if (Build. VERSION.SDK_INT >= Build. VERSION_CODES.Q) {
            val hasPermission = ContextCompat.checkSelfPermission(
                this, Manifest.permission. ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

            if (! hasPermission) {
                AlertDialog.Builder(this)
                    .setTitle("Permiso adicional necesario")
                    .setMessage("Para el rastreo continuo en segundo plano, permite 'Permitir todo el tiempo' en la siguiente pantalla.")
                    .setPositiveButton("Continuar") { _, _ ->
                        backgroundLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                    }
                    .setNegativeButton("Ahora no", null)
                    .show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        binding.mapView.onResume()

        Log.d("MainActivity", "onResume - Registrando receiver")

        // Registrar receiver
        try {
            ContextCompat.registerReceiver(
                this,
                locationReceiver,
                IntentFilter("LOCATION_UPDATE"),
                ContextCompat. RECEIVER_NOT_EXPORTED
            )
            Log.d("MainActivity", "Receiver registrado correctamente")
        } catch (e: Exception) {
            Log.e("MainActivity", "Error al registrar receiver", e)
        }

        loadRoute()
    }

    override fun onPause() {
        super.onPause()
        binding.mapView.onPause()

        Log. d("MainActivity", "onPause - Desregistrando receiver")

        try {
            unregisterReceiver(locationReceiver)
            Log.d("MainActivity", "Receiver desregistrado correctamente")
        } catch (e: Exception) {
            Log.e("MainActivity", "Error al desregistrar receiver", e)
        }
    }
}