package com.example.foodapp.services

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.example.foodapp.MainActivity
import com.example.foodapp.utils.UniversalLocationEngine
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class LocationBroadcastService : Service() {

    private var orderId: String? = null
    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    companion object {
        const val CHANNEL_ID = "LocationBroadcastChannel"
        const val NOTIFICATION_ID = 1001
        const val EXTRA_ORDER_ID = "EXTRA_ORDER_ID"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        orderId = intent?.getStringExtra(EXTRA_ORDER_ID)
        
        if (orderId == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)

        startLocationUpdates()

        return START_STICKY
    }

    private fun startLocationUpdates() {
        serviceScope.launch(Dispatchers.IO) {
            try {
                val id = orderId ?: return@launch
                
                // 1. Fetch Order
                val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                val doc = db.collection("orders").document(id).get().await()
                if (!doc.exists()) return@launch
                
                val branchLoc = doc.get("branchLocation") as? Map<String, Double> ?: return@launch
                val deliveryLoc = doc.get("deliveryLocation") as? Map<String, Double> 
                    ?: (doc.get("deliveryAddress") as? Map<String, Any>)?.get("location") as? Map<String, Double> 
                    ?: return@launch

                val branchLat = branchLoc["latitude"] ?: return@launch
                val branchLng = branchLoc["longitude"] ?: return@launch
                val destLat = deliveryLoc["latitude"] ?: return@launch
                val destLng = deliveryLoc["longitude"] ?: return@launch

                // 2. Fetch Mapbox Route
                val token = getString(resources.getIdentifier("mapbox_access_token", "string", packageName))
                val url = "https://api.mapbox.com/directions/v5/mapbox/driving/${branchLng},${branchLat};${destLng},${destLat}?geometries=geojson&overview=full&access_token=$token"
                val response = java.net.URL(url).readText()
                val json = org.json.JSONObject(response)
                val routes = json.getJSONArray("routes")
                if (routes.length() == 0) return@launch
                
                val coords = routes.getJSONObject(0).getJSONObject("geometry").getJSONArray("coordinates")
                val points = mutableListOf<Pair<Double, Double>>()
                for (i in 0 until coords.length()) {
                    val pair = coords.getJSONArray(i)
                    points.add(Pair(pair.getDouble(1), pair.getDouble(0))) // lat, lng
                }

                // 3. Simulate Movement
                val dbRef = FirebaseDatabase.getInstance().getReference("active_deliveries").child(id)
                var currentIndex = 0
                while (isActive && currentIndex < points.size) {
                    val pt = points[currentIndex]
                    dbRef.setValue(mapOf(
                        "lat" to pt.first,
                        "lng" to pt.second,
                        "timestamp" to System.currentTimeMillis()
                    ))
                    currentIndex++
                    delay(2000) // update every 2 seconds to trace the road perfectly
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
        orderId?.let { id ->
            FirebaseDatabase.getInstance().getReference("active_deliveries").child(id).removeValue()
        }
    }

    private fun createNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Ice Land Delivery - Tracking Active")
            .setContentText("Tap to return to job.")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Delivery Tracking",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }



    override fun onBind(intent: Intent?): IBinder? = null
}
