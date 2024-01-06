package com.example.funnysignsexamination

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.fragment.app.Fragment

class DetailFragment : Fragment() {

    private val ratingsMap: MutableMap<Float, Int> = mutableMapOf()
    private lateinit var signRatingBarFragment: RatingBar

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val imageUri = arguments?.getString("image")
        val name = arguments?.getString("name")
        val location = arguments?.getString("location")

        val signImageFragment = view.findViewById<ImageView>(R.id.signImageFragment)
        val signNameFragment = view.findViewById<TextView>(R.id.signNameFragment)
        val signLocationFragment = view.findViewById<TextView>(R.id.signLocationFragment)
        signRatingBarFragment = view.findViewById(R.id.signRatingBarFragment)

        signImageFragment.setImageURI(Uri.parse(imageUri))
        signNameFragment.text = name
        signLocationFragment.text = location

        signRatingBarFragment.setOnRatingBarChangeListener { _, rating, fromUser ->
            if (fromUser) {
                val currentVotes = ratingsMap[rating] ?:0
                ratingsMap[rating] = currentVotes + 1

                var totalRating = 0.0f
                var totalVotes = 0

                for ((key, value) in ratingsMap) {
                    totalRating += key * value
                    totalVotes += value
                }

                val averageRating = totalRating / totalVotes
            }
        }
    }
}