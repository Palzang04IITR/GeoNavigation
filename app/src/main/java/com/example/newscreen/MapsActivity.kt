package com.example.newscreen

import android.Manifest

import android.app.AlertDialog
import android.app.PendingIntent

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.widget.Button

import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.codebyashish.googledirectionapi.AbstractRouting
import com.codebyashish.googledirectionapi.ErrorHandling
import com.codebyashish.googledirectionapi.RouteDrawing
import com.codebyashish.googledirectionapi.RouteInfoModel
import com.codebyashish.googledirectionapi.RouteListener

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {


    private lateinit var geofencingClient: GeofencingClient
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var handler: Handler
//    private val refreshTime: Long = 5000
    private lateinit var database: DatabaseReference
    private lateinit var googleMap: GoogleMap
    private lateinit var geofencePendingIntent: PendingIntent
    private var count = 0
//    private var locationsOrderedClockwise: MutableList<GeofenceData> = mutableListOf()
//    private var currentLocationIndex = 0
//    private var currentPolyline: Polyline? = null



    companion object {
        const val TAG = "MapsActivity"
        const val LOCATION_REQUEST_CODE = 10001
        const val BACKGROUND_REQUEST_CODE = 10002
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        enableEdgeToEdge()

        geofencingClient = LocationServices.getGeofencingClient(this)
        database = FirebaseDatabase.getInstance().reference
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)


        handler = Handler(mainLooper)

        // Initialize mapFragment and wait for it to be ready
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as? SupportMapFragment
        mapFragment?.getMapAsync(this) // Asynchronous; invokes onMapReady when the map is ready

//        val viewSequencesButton: Button = findViewById(R.id.viewSequencesButton)
//        viewSequencesButton.setOnClickListener {
//            val intent = Intent(this, SequenceListActivity::class.java)
//            intent.putExtra("locations_ordered", ArrayList(locationsOrderedClockwise))
//            startActivity(intent)
//        }
    }


    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        // Retrieve and process geofence data to draw polygon

        checkLocationPermission()

    }


    private fun GoogleMap.setOnMarkerClickListener(mapsActivity: MapsActivity) {
        this.setOnMarkerClickListener { marker ->
            // Show the info window with only the title on single tap
            marker.showInfoWindow()
            true
        }


        this.setOnInfoWindowClickListener { marker ->
            val geofenceData = marker.tag as? GeofenceData
            val intent = Intent(mapsActivity, geofence_description::class.java).apply {
                putExtra("name", marker.title)
                putExtra("description", marker.snippet)
                putExtra("imageUrl" +
                        "", geofenceData?.imageUrl)

            }
            mapsActivity.startActivity(intent)
        }


    }
    private fun enableLocationOnMap() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {

            googleMap.isMyLocationEnabled = true
            googleMap.uiSettings.isMyLocationButtonEnabled = true
            googleMap.setOnMarkerClickListener(this)
        }
    }


    private fun checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            // Invoke after permissions are granted
            enableLocationOnMap()
            fetchGeofenceData {
                // Geofence data fetching is complete, proceed to get user location
                getUserLocation()
            }

        } else {
            requestForPermissions()
        }
    }
    private fun getUserLocation() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e(TAG, "Location permissions are not granted")
            return
        }

        val task = fusedLocationProviderClient.lastLocation
        task.addOnSuccessListener { location ->
            if (location != null) {
                val userLatitude = location.latitude
                val userLongitude = location.longitude

                val userLocation = LatLng(userLatitude, userLongitude)
                Log.i(TAG, "User location found: $userLocation")

                // Move the camera to the user's location
                if (!::googleMap.isInitialized || googleMap.cameraPosition.zoom == 14f) {
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 14f))
                }

                // Retrieve all geofence data from the manager
                val geofenceLocations = GeofenceDataManager.getAllGeofenceData()

                // If there are geofence locations, draw routes between them
                if (geofenceLocations.isNotEmpty()) {
                    drawRoutesBetweenAllLocations(userLatitude, userLongitude, geofenceLocations)
                } else {
                    Log.e(TAG, "No geofence data available.")
                }

                // Debug message indicating the process is starting
                Log.d(TAG, "Starting navigation from the first location in the list.")
            } else {
                Log.e(TAG, "Failed to get user location. Location is null.")
            }
        }.addOnFailureListener {
            Log.e(TAG, "Failed to get user location: ${it.message}")
        }
    }


    private fun drawRoutesBetweenAllLocations(userLatitude: Double, userLongitude: Double, locations: List<GeofenceData>) {
        // Create a LatLng object for the user's location
        val userLocation = LatLng(userLatitude, userLongitude)

        // Iterate over each pair of locations to draw routes between them
        for (i in locations.indices) {
            for (j in locations.indices) {
                if (i != j) {
                    val startLocation = LatLng(locations[i].latitude, locations[i].longitude)
                    val endLocation = LatLng(locations[j].latitude, locations[j].longitude)

                    val routeDrawing = RouteDrawing.Builder()
                        .context(this)
                        .travelMode(AbstractRouting.TravelMode.DRIVING)
                        .withListener(object : RouteListener {
                            override fun onRouteStart() {
                                Log.d(TAG, "Routing started from ${locations[i].name} to ${locations[j].name}")
                            }

                            override fun onRouteSuccess(routeInfoModelArrayList: ArrayList<RouteInfoModel>, routeIndexing: Int) {
                                if (routeInfoModelArrayList.isNotEmpty()) {
                                    val route = routeInfoModelArrayList[routeIndexing]
                                    Log.d(TAG, "Selected Route Index: $routeIndexing with ${route.points.size} points")

                                    val polylineOptions = PolylineOptions()
                                        .color(Color.BLUE)
                                        .width(10f)
                                        .addAll(route.points)

                                    // Draw the polyline on the Google Map and save the reference to it
                                    googleMap.addPolyline(polylineOptions)
                                } else {
                                    Log.e(TAG, "No routes found in the RouteInfoModelArrayList.")
                                }
                            }

                            override fun onRouteFailure(e: ErrorHandling?) {
                                Log.e(TAG, "Routing failed: ${e?.message}")
                            }

                            override fun onRouteCancelled() {
                                Log.d(TAG, "Routing cancelled")
                            }
                        })
                        .alternativeRoutes(true)
                        .waypoints(startLocation, endLocation)
                        .build()

                    routeDrawing.execute()
                }
            }
        }
    }


    private fun requestForPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,


                ),
            LOCATION_REQUEST_CODE
        )
    }
    private fun fetchGeofenceData(onDataFetched: () -> Unit) {
        database.child("geofence").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                // Clear the manager to remove old data before updating with new data
                GeofenceDataManager.clearAllGeofenceData()

                for (geofenceSnapshot in dataSnapshot.children) {
                    val geofenceData = extractGeofenceData(geofenceSnapshot)
                    if (geofenceData != null) {
                        // Add each GeofenceData instance to the manager
                        GeofenceDataManager.addGeofenceData(geofenceData)
                    }
                }

                // Process all collected geofence data stored in the manager
                processGeofenceDataInManager()

                // Execute the callback after data processing is complete
                onDataFetched()
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.w(TAG, "loadPost:onCancelled", databaseError.toException())
            }
        })
    }


    private fun extractGeofenceData(geofenceSnapshot: DataSnapshot): GeofenceData? {
        val name = geofenceSnapshot.child("name").getValue(String::class.java) ?: ""
        val latitude = geofenceSnapshot.child("lat").getValue(Double::class.java) ?: Double.NaN
        val longitude = geofenceSnapshot.child("long").getValue(Double::class.java) ?: Double.NaN
        val description = geofenceSnapshot.child("description").getValue(String::class.java) ?: ""
        val radius = geofenceSnapshot.child("radius").getValue(Double::class.java) ?: Double.NaN
        val imageUrl = geofenceSnapshot.child("imageuri").getValue(String::class.java) ?: ""
        Log.d(TAG, " extract $imageUrl")

        return if (name.isNotEmpty() && !latitude.isNaN() && !longitude.isNaN()) {
            GeofenceData(name, latitude, longitude, description,imageUrl, radius)
        } else {
            null  // Return null if data is invalid
        }
    }

    private fun processGeofenceDataInManager() {

        GeofenceDataManager.getAllGeofenceData().forEach { geofenceData ->

            addMarkerAndGeofence(geofenceData)
        }
    }

    private fun addMarkerAndGeofence(geofenceData: GeofenceData) {
        val icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
        val marker = googleMap.addMarker(
            MarkerOptions()
                .position(LatLng(geofenceData.latitude, geofenceData.longitude))
                .title(geofenceData.name)
                .snippet(geofenceData.description)
                .icon(icon)
        )
        marker?.tag = geofenceData
// This is the part that adds the circle. Remove or comment it out:
// googleMap.addCircle(
//     CircleOptions()
//         .center(LatLng(geofenceData.latitude, geofenceData.longitude))
//         .radius(geofenceData.radius)
//         .strokeWidth(2f)
//         .strokeColor(0xFF6495ED.toInt())
//         .fillColor(0x446495ED)
// )
//        Log.d(TAG, "Geofence radius: ${geofenceData.radius}")
//

        val geofence = Geofence.Builder()
            .setRequestId(geofenceData.name)
            .setCircularRegion(geofenceData.latitude, geofenceData.longitude,
                geofenceData.radius.toFloat()
            )
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(
                Geofence.GEOFENCE_TRANSITION_ENTER
            )
            .build()


        val geofencingRequest = GeofencingRequest.Builder()
            .addGeofence(geofence)
            .build()
        if (geofenceData.imageUrl != null && geofenceData.imageUrl.isNotEmpty()) {
            Log.d(TAG, "Image URL exists for ${geofenceData.name}: ${geofenceData.imageUrl}")
        } else {
            Log.d(TAG, "No Image URL for ${geofenceData.name}")
        }

        if (count == 0) {
            val intent = Intent(this, GeofenceBroadcastReceiver::class.java)
            geofencePendingIntent = PendingIntent.getBroadcast(
                this, geofenceData.name.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
            count += 1
        }
        Log.d(TAG, "$count")

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            Log.e(TAG, "Required location permissions not granted")
//            Toast.makeText(this, "Location permissions are not granted", Toast.LENGTH_SHORT).show()
            return
        }

        geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent).run {
            addOnSuccessListener {
                Log.d(TAG, "Geofence '${geofenceData.name}' added successfully")
            }

            addOnFailureListener { e ->
                Log.w(TAG, "Failed to add geofence: ${geofenceData.name} ", e)
            }
        }
    }


    private fun checkBackgroundLocationPermission(backgroundLocationRequestCode: Int) {
        if (checkSinglePermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION)) return
        AlertDialog.Builder(this)
            .setTitle("Background permission required")
            .setMessage("Press on YES to grant permission")
            .setPositiveButton("yes") { _, _ ->
                // this request will take user to Application's Setting page
                requestPermissions(
                    arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION),
                    backgroundLocationRequestCode
                )
            }
            .setNegativeButton("no") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
            .show()
    }

    private fun checkSinglePermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_REQUEST_CODE) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                // Foreground permission granted, check for background now

                checkBackgroundLocationPermission(BACKGROUND_REQUEST_CODE)

            } else {
                Log.e(TAG, "Foreground permission denied")
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
            }
        }
        if (requestCode == BACKGROUND_REQUEST_CODE) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                Log.d(TAG, "Background location permission granted")

            } else {
                Log.e(TAG, "Background permission denied")
                Toast.makeText(
                    this,
                    "Background location permission is required for geofences",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}

//    private fun checkProximityAndNavigate() {
//        Log.d(TAG, "checkProximityAndNavigate: Function called")
//        if (ActivityCompat.checkSelfPermission(
//                this,
//                Manifest.permission.ACCESS_FINE_LOCATION
//            ) != PackageManager.PERMISSION_GRANTED &&
//            ActivityCompat.checkSelfPermission(
//                this,
//                Manifest.permission.ACCESS_COARSE_LOCATION
//            ) != PackageManager.PERMISSION_GRANTED
//        ) {
//            Log.d(TAG, "Location permissions are not granted. Requesting permissions.")
//            requestForPermissions()
//            return
//        }
//
//        Log.d(TAG, "Fetching the user's last known location.")
//        val task = fusedLocationProviderClient.lastLocation
//        task.addOnSuccessListener { location ->
//            if (location != null) {
//                val userLatitude = location.latitude
//                val userLongitude = location.longitude
//
//                Log.d(TAG, "User's current location: Latitude = $userLatitude, Longitude = $userLongitude")
//                drawRouteToNearestLocation(userLatitude, userLongitude)
//
//                if (locationsOrderedClockwise.isNotEmpty()) {
//                    // Log the current sequence of locations
//                    Log.d(TAG, "Current sequence of locations:")
//                    locationsOrderedClockwise.forEachIndexed { index, geofenceData ->
//                        Log.d(TAG, "[$index]: ${geofenceData.name} at Lat: ${geofenceData.latitude}, Lng: ${geofenceData.longitude}")
//                    }
//
//                    val currentLocation = locationsOrderedClockwise[currentLocationIndex]
//                    val distance = calculateDistance(
//                        userLatitude, userLongitude,
//                        currentLocation.latitude, currentLocation.longitude
//                    )
//
//                    Log.d(TAG, "Distance to ${currentLocation.name}: ${String.format("%.2f", distance)} meters")
//
//                    if (distance <= 50) {
//                        Log.d(TAG, "Arrived at location: ${currentLocation.name}. Moving it to the back of the sequence.")
//                        Toast.makeText(this, "Visited: ${currentLocation.name}", Toast.LENGTH_LONG).show()
//
//                        // Remove the current location and add it to the back of the list
//                        locationsOrderedClockwise.removeAt(currentLocationIndex)
//                        locationsOrderedClockwise.add(currentLocation)
//
//                        // Remove the current polyline
//                        currentPolyline?.remove()
//                        currentPolyline = null
//                        Log.d(TAG, "Path to ${currentLocation.name} has been cleared.")
//
//                        // Reset the current location index to the start of the list
//                        currentLocationIndex = 0
//
//                        // Log the new sequence of locations
//                        Log.d(TAG, "New sequence after moving visited location to the end:")
//                        locationsOrderedClockwise.forEachIndexed { index, geofenceData ->
//                            Log.d(TAG, "[$index]: ${geofenceData.name}")
//                        }
//                    } else {
//                        Log.d(TAG, "Approaching: ${currentLocation.name}. Distance: $distance meters")
//                        Toast.makeText(
//                            this, "Approaching ${currentLocation.name}, ${
//                                String.format("%.2f", distance)
//                            } m away", Toast.LENGTH_LONG
//                        ).show()
//                    }
//
//                    // Schedule the next proximity check
//                    Log.d(TAG, "Scheduling the next proximity check in $refreshTime milliseconds.")
//                    handler.postDelayed({ checkProximityAndNavigate() }, refreshTime)
//                } else {
//                    Log.d(TAG, "No geofence locations available in the sequence.")
//                }
//            } else {
//                Log.e(TAG, "User location is null. Unable to proceed.")
//            }
//        }.addOnFailureListener {
//            Log.e(TAG, "Failed to get user location: ${it.message}")
//        }
//    }

//    private fun drawRouteToNearestLocation(userLatitude: Double, userLongitude: Double) {
//        // Create a LatLng object for the user's location
//        val userLocation = LatLng(userLatitude, userLongitude)
//
//        // Retrieve the next nearest location
//        val nearestLocation = getNextNearestLocation()
//
//        if (nearestLocation != null) {
//            Log.d(TAG, "Nearest Location: Latitude = ${nearestLocation.latitude}, Longitude = ${nearestLocation.longitude}")
//
//            val routeDrawing = RouteDrawing.Builder()
//                .context(this)
//                .travelMode(AbstractRouting.TravelMode.DRIVING)
//                .withListener(object : RouteListener {
//                    override fun onRouteStart() {
//                        Log.d(TAG, "Routing started")
//                    }
//
//                    override fun onRouteSuccess(routeInfoModelArrayList: ArrayList<RouteInfoModel>, routeIndexing: Int) {
//                        if (routeInfoModelArrayList.isNotEmpty()) {
//                            val route = routeInfoModelArrayList[routeIndexing]
//                            Log.d(TAG, "Selected Route Index: $routeIndexing with ${route.points.size} points")
//
//                            val polylineOptions = PolylineOptions()
//                                .color(Color.BLUE)
//                                .width(10f)
//                                .addAll(route.points)
//
//                            // Draw the polyline on the Google Map and save the reference to it
//                            currentPolyline = googleMap.addPolyline(polylineOptions)
//                        } else {
//                            Log.e(TAG, "No routes found in the RouteInfoModelArrayList.")
//                        }
//                    }
//
//                    override fun onRouteFailure(e: ErrorHandling?) {
//                        Log.e(TAG, "Routing failed: ${e?.message}")
//                    }
//
//                    override fun onRouteCancelled() {
//                        Log.d(TAG, "Routing cancelled")
//                    }
//                })
//                .alternativeRoutes(true)
//                .waypoints(userLocation, nearestLocation)
//                .build()
//
//            routeDrawing.execute()
//        } else {
//            Log.e(TAG, "Unable to determine the nearest location.")
//        }
//    }

//    // Function to get the next nearest location
//    private fun getNextNearestLocation(): LatLng? {
//        // Check if locationsOrderedClockwise is initialized and has valid data
//        return if (locationsOrderedClockwise.isNotEmpty()) {
//            val geofence = locationsOrderedClockwise[0] // Access the first geofence data
//            // Return the LatLng of the first geofence
//            LatLng(geofence.latitude, geofence.longitude)
//        } else {
//            null // Return null if the list is empty
//        }
//    }

//    private fun getUserLocation() {
//        if (ActivityCompat.checkSelfPermission(
//                this,
//                Manifest.permission.ACCESS_FINE_LOCATION
//            ) != PackageManager.PERMISSION_GRANTED &&
//            ActivityCompat.checkSelfPermission(
//                this,
//                Manifest.permission.ACCESS_COARSE_LOCATION
//            ) != PackageManager.PERMISSION_GRANTED
//        ) {
//            Log.e(TAG, "Location permissions are not granted")
//            return
//        }
//
//        val task = fusedLocationProviderClient.lastLocation
//        task.addOnSuccessListener { location ->
//            if (location != null) {
//                val userLatitude = location.latitude
//                val userLongitude = location.longitude
//
//                val userLocation = LatLng(userLatitude, userLongitude)
//                Log.i(TAG, "User location found: $userLocation")
//
//                // Move the camera to the user's location
//                if (!::googleMap.isInitialized || googleMap.cameraPosition.zoom == 14f) {
//                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 14f))
//                }
//                fetchGeofenceData {  }
//                // Fetch geofence data and sort them in a clockwise order starting from the user's location
////                fetchGeofenceData {
////                    setupClockwiseOrder(userLatitude, userLongitude)
////                    startNavigation()
////                }
//
//                // Debug message indicating the process is starting
//                Log.d(TAG, "Starting navigation from the first location in the list.")
//            } else {
//                Log.e(TAG, "Failed to get user location. Location is null.")
//            }
//        }.addOnFailureListener {
//            Log.e(TAG, "Failed to get user location: ${it.message}")
//        }
//    }
//    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
//        val results = FloatArray(1)
//        Location.distanceBetween(lat1, lon1, lat2, lon2, results)
//        return results[0] // Convert to kilometers
//    }


//    private fun startNavigation() {
//        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
//            // Get user location and start the navigation process
//            checkProximityAndNavigate()
//        } else {
//            Log.e(TAG, "Permissions for location not granted. Cannot start navigation.")
//        }
//    }
//    private fun setupClockwiseOrder(userLatitude: Double, userLongitude: Double) {
//        // Get all locations from the data manager
//        val allLocations = GeofenceDataManager.getAllGeofenceData()
//
//        // Sort the locations using the method in GeofenceData, using the user's current location
//        locationsOrderedClockwise = GeofenceData.sortClockwise(allLocations, userLatitude, userLongitude).toMutableList()
//
//        // Debug log the sorted locations for verification
//        Log.d(TAG, "Location order after sorting clockwise:")
//        locationsOrderedClockwise.forEachIndexed { index, location ->
//            Log.d(TAG, "[$index]: ${location.name} at Lat: ${location.latitude}, Lng: ${location.longitude}")
//        }
//    }


