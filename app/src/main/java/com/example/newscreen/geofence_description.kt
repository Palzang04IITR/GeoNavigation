package com.example.newscreen

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.example.newscreen.MapsActivity.Companion.TAG

class geofence_description : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_geofence_description)
        val description = intent.getStringExtra("description")
        val name=intent.getStringExtra("name")
        val imageUrl =intent.getStringExtra("imageUrl")
        Log.d(TAG,"inside newactivity2")
        Log.d(TAG, "Image URL: $imageUrl")

        findViewById<TextView>(R.id.nameTextView).text = name
        findViewById<TextView>(R.id.descriptionTextView).text = description
        val imageView = findViewById<ImageView>(R.id.imageView)
        if (!imageUrl.isNullOrEmpty()) {
            // Load the image using Glide
            Glide.with(this)
                .load(imageUrl)
                .into(imageView)

        }

        findViewById<Button>(R.id.backButton).setOnClickListener {
            finish() // This will close the current activity and go back to the previous activity
        }
    }
}