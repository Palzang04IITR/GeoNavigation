package com.example.newscreen

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

class SequenceListActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_sequence_list)

        val backButton: Button = findViewById(R.id.backButton)
        backButton.setOnClickListener {
            finish()
        }

        // Retrieve the ordered locations from the Intent
        val orderedGeofences = intent.getSerializableExtra("locations_ordered") as? ArrayList<GeofenceData> ?: arrayListOf()

        // Extract the sequence names for display
        val sequenceNames = orderedGeofences.map { it.name }

        // Get a reference to your ListView
        val listView: ListView = findViewById(R.id.sequenceListView)

        // Set adapter for ListView with the ordered sequence names
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, sequenceNames)
        listView.adapter = adapter
    }
}
