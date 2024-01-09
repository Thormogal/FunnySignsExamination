package com.example.funnysignsexamination

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.squareup.picasso.Picasso

class DetailFragment : Fragment() {

    private lateinit var signImageFragment: ImageView
    private lateinit var signNameFragment: TextView
    private lateinit var signLocationFragment: TextView
    private lateinit var signRatingBarFragment: RatingBar
    private lateinit var signRatingVotesFragment: TextView
    private lateinit var sign: Sign
    private lateinit var myRef: DatabaseReference
    private var userId: String? = null
    private var ratingsMap: MutableMap<String, Float> = mutableMapOf()
    private var voters: MutableList<String> = mutableListOf()
    private var totalVotes = 0
    private var averageRating = 0.0f

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
        val rating = arguments?.getFloat("rating") ?: 0f

        sign = Sign(
            id = "",
            name = name ?: "",
            imageUrl = imageUri ?: "",
            location = location ?: "",
            rating = rating.toDouble(),
            voters = mutableListOf()
        )

        voters = sign.voters.toMutableList()
        signImageFragment = view.findViewById(R.id.signImageFragment)
        signNameFragment = view.findViewById(R.id.signNameFragment)
        signLocationFragment = view.findViewById(R.id.signLocationFragment)
        signRatingVotesFragment = view.findViewById(R.id.signRatingBarVotesFragment)
        signRatingBarFragment = view.findViewById(R.id.signRatingBarFragment)
        signRatingBarFragment.rating = 0f

        Picasso.get()
            .load(imageUri)
            .into(signImageFragment)
        signNameFragment.text = createColoredNameString("Sign name:", name ?: "Unknown")
        signLocationFragment.text = createColoredNameString("Sign location:", location ?: "Unknown")

        myRef = Firebase.database("https://funnysignsexamination-default-rtdb.europe-west1.firebasedatabase.app")
            .getReference("signs").child(sign.id)

        signRatingBarFragment.setOnRatingBarChangeListener { _, rating, fromUser ->
            if (fromUser) {
                userId = FirebaseAuth.getInstance().currentUser?.uid
                if (userId != null) {
                    if (userId in voters) {
                        showToast("You have already voted on this sign!")
                        signRatingBarFragment.rating = averageRating
                    } else {
                        voters.add(userId!!)
                        ratingsMap[userId!!] = rating

                        var totalRating = 0.0f
                        totalVotes = ratingsMap.size

                        for ((_, value) in ratingsMap) {
                            totalRating += value
                        }

                        val averageRating = totalRating / totalVotes
                        signRatingBarFragment.rating = averageRating
                        signRatingVotesFragment.text =
                            createColoredNameString("Votes:", totalVotes.toString())

                        myRef.child("votes").setValue(ratingsMap)

                        signRatingBarFragment.isClickable = false
                    }
                }
            }
            view.setOnClickListener {
                parentFragmentManager.popBackStack()
            }
        }
        updateRatingFromDatabase()
    }

    override fun onStart() {
        super.onStart()
        updateRatingFromDatabase()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        updateVotesAndRatings()
    }

    private fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    private fun updateVotesAndRatings() {
        myRef.child("votes").setValue(ratingsMap)
            .addOnSuccessListener {
                myRef.child("votes").get().addOnSuccessListener { dataSnapshot ->
                    val votes = dataSnapshot.value as? Map<String, Float>
                    votes?.let {
                        ratingsMap = it.toMutableMap()
                        var totalRating = 0.0f
                        totalVotes = 0

                        for ((_, value) in ratingsMap) {
                            totalRating += value
                            totalVotes++
                        }

                        val averageRating = totalRating / totalVotes
                        signRatingBarFragment.rating = averageRating
                        signRatingVotesFragment.text =
                            createColoredNameString("Votes:", totalVotes.toString())
                    }
                }
            }
    }

    private fun updateRatingFromDatabase() {
        myRef.child("votes").get().addOnSuccessListener { dataSnapshot ->
            val votes = dataSnapshot.value as? Map<String, Float>
            votes?.let {
                ratingsMap = it.toMutableMap()
                var totalRating = 0.0f
                totalVotes = 0

                for ((_, value) in ratingsMap) {
                    totalRating += value
                    totalVotes++
                }

                averageRating = totalRating / totalVotes
                signRatingBarFragment.rating = averageRating
                signRatingVotesFragment.text =
                    createColoredNameString("Votes:", totalVotes.toString())
            }
        }
    }
}