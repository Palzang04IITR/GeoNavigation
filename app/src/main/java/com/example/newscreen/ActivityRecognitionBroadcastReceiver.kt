package com.example.newscreen//package com.example.newscreen
//
//import android.content.BroadcastReceiver
//import android.content.Context
//import android.content.Intent
//import com.google.android.gms.location.ActivityTransitionResult
//import com.google.android.gms.location.DetectedActivity
//import com.google.android.gms.maps.model.BitmapDescriptorFactory
//import com.google.android.gms.maps.model.Marker
//
//class ActivityRecognitionBroadcastReceiver : BroadcastReceiver() {
//    override fun onReceive(context: Context, intent: Intent) {
//        if (ActivityTransitionResult.hasResult(intent)) {
//            val result = ActivityTransitionResult.extractResult(intent)
//            val mapsActivity = context as MapsActivity
//
//            if (result != null) {
//                for (event in result.transitionEvents) {
//                    val iconResource = when (event.activityType) {
//                        DetectedActivity.WALKING -> R.drawable.walking // Custom icon for walking
//                        DetectedActivity.IN_VEHICLE -> R.drawable.car // Custom icon for in-vehicle
//                        else -> R.drawable.default_user // Fallback/default icon
//                    }
//
//                    val currentLocation = mapsActivity.userMarker?.position
//                    if (currentLocation != null) {
//                        mapsActivity.updateOrCreateUserMarker(currentLocation, iconResource)
//                    }
//                }
//            }
//        }
//    }
//}