package com.example.newscreen

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.newscreen.Constants.NOTIFICATION_CHANNEL_ID
import com.example.newscreen.Constants.NOTIFICATION_CHANNEL_NAME
import com.example.newscreen.Constants.NOTIFICATION_ID
import com.example.newscreen.MapsActivity.Companion.TAG
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent

class GeofenceBroadcastReceiver : BroadcastReceiver() {


    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "inside receiver")
        val geofencingEvent = GeofencingEvent.fromIntent(intent)
        if (geofencingEvent != null) {
            if (geofencingEvent.hasError()) {
                val errorMessage = GeofenceStatusCodes.getStatusCodeString(geofencingEvent.errorCode)
                Log.e("BroadcastReceiver", errorMessage)
                return
            }
        }



        val geofenceTransition = geofencingEvent?.geofenceTransition

        // Check if the transition was an entry
        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER ) {
            // Retrieve geofences that triggered this transition
            val triggeringGeofences = geofencingEvent.triggeringGeofences

            // Iterate over the triggering geofences
            if (triggeringGeofences != null) {
                for (geofence in triggeringGeofences) {
                    val geofenceName = geofence.requestId

                    // Log for debugging purposes
                    Log.d(TAG, "Checking geofence: $geofenceName")
                    val geofenceData = GeofenceDataManager.getGeofenceData(geofenceName)
                    // Compare with the specific geofence name you are interested in
                    if (geofenceData != null) {
                        if (geofenceName ==geofenceData.name) { // replace with the actual name

                            Log.i(TAG, "Entered specific geofence: ${geofenceData.name}")
                            if (geofenceData.imageUrl != null && geofenceData.imageUrl.isNotEmpty()) {
                                Log.d(TAG, "Image URL exists for ${geofenceData.name}: ${geofenceData.imageUrl}")
                            } else {
                                Log.d(TAG, "No Image URL for ${geofenceData.name}")
                            }
                            startGeofenceDescriptionActivity(
                                context,
                                geofenceData.description,
                                geofenceData.name,
                                geofenceData.imageUrl
                            )
                        } else {
                            Log.d(TAG, "Ignoring geofence: $geofenceName")
                        }
                    }
                }
            }
        } else {
            Log.e(TAG, "Invalid transition type or non-entry transition: $geofenceTransition")
        }
    }

    private fun startGeofenceDescriptionActivity(
        context: Context,
        description: String,
        name: String,
        imageUrl: String?
    ) {
        Log.d(TAG, "Inside new activity with Description: $description and Name: $name")
        val locationIntent = Intent(context, geofence_description::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            putExtra("description", description)
            putExtra("name", name)
            putExtra("imageUrl",imageUrl)
        }
        context.startActivity(locationIntent)
    }
}

    private fun displayNotification(context: Context, geofenceTransition: String) {
        Log.d(TAG, "Displaying notification with message: $geofenceTransition")

        // Create the notification channel if necessary
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for geofence transitions"
            }
            val notificationManager: NotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        // Create the notification
        val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification) // Make sure this drawable resource exists
            .setContentTitle("Geofence Notification")
            .setContentText(geofenceTransition)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        // Display the notification
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

