package com.example.funnysignsexamination

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

class FunnySignsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_funny_signs)

        val signs = mutableListOf(
            Sign("1", "Sign 1", "ImageUrl1","Location 1", 4.5, false),
            Sign("2", "Sign 2", "ImageUrl2", "Location 2", 3.5, true)
        )

        val recyclerView = findViewById<RecyclerView>(R.id.funny_signs_recycler_view)
        recyclerView.layoutManager = GridLayoutManager(this, 2)
        recyclerView.adapter = Adapter(signs)

    }
}