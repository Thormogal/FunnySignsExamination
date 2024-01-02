package com.example.funnysignsexamination

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore

class FunnySignsActivity : AppCompatActivity() {

    val db = Firebase.firestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_funny_signs)

        val signs = mutableListOf(
            Sign("1", "Sign 1", "ImageUrl1","Location 1", 4.5, false),
            Sign("2", "Sign 2", "ImageUrl2", "Location 2", 3.5, true),
            Sign("3", "Sign 3", "ImageUrl3", "Location 3", 2.0,true)
        )

        val recyclerView = findViewById<RecyclerView>(R.id.funnySignsRecyclerView)
        recyclerView.layoutManager = GridLayoutManager(this, 2)
        recyclerView.adapter = Adapter(signs)

    }
}