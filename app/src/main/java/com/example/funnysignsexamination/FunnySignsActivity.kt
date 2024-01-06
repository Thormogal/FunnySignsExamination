package com.example.funnysignsexamination

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.InputFilter
import android.util.Log
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.FirebaseStorage
import java.util.UUID

class FunnySignsActivity : AppCompatActivity() {

    private lateinit var adapter: Adapter
    private lateinit var selectedImageUri: Uri
    private val db = Firebase.firestore
    private val getContent =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                selectedImageUri = it
                showInputDialog()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_funny_signs)

        val recyclerView = findViewById<RecyclerView>(R.id.funnySignsRecyclerView)
        recyclerView.layoutManager = GridLayoutManager(this, 2)

        adapter = Adapter(mutableListOf())
        recyclerView.adapter = adapter
        fetchSignsFromFirestore()

        val addSignButton = findViewById<ImageButton>(R.id.signAddButton)
        addSignButton.setOnClickListener {
                openImagePicker()
        }

        val addThroughCameraButton = findViewById<ImageButton>(R.id.signCameraButton)
        addThroughCameraButton.setOnClickListener {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED
            ) {
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.CAMERA),
                    REQUEST_CODE
                )
            }
        }
    }

    companion object {
        private const val REQUEST_CODE = 1
    }

    private fun openImagePicker() {
        getContent.launch("image/*")
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        Log.d("Permissions", "onRequestPermissionsResult called")
        when (requestCode) {
            REQUEST_CODE -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    openImagePicker()
                } else {
                    Toast.makeText(this, "Access needed to upload pictures", Toast.LENGTH_SHORT).show()
                }
                return
            }
            else -> {
            }
        }
    }

    private fun setMaxTextLength(editText: EditText, maxLength: Int) {
        editText.filters = arrayOf<InputFilter>(InputFilter.LengthFilter(maxLength))
    }

    private fun showInputDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Name for sign and the location")

        val inputLayout = LinearLayout(this)
        inputLayout.orientation = LinearLayout.VERTICAL

        val nameEditText = EditText(this)
        nameEditText.hint = "Name of the sign"
        inputLayout.addView(nameEditText)
        setMaxTextLength(nameEditText, 20)

        val locationEditText = EditText(this)
        locationEditText.hint = "Location of the sign"
        inputLayout.addView(locationEditText)
        setMaxTextLength(locationEditText, 25)

        builder.setView(inputLayout)

        builder.setPositiveButton("OK") { _, _ ->
            val name = nameEditText.text.toString()
            val location = locationEditText.text.toString()

            if (name.isNotBlank() && location.isNotBlank()) {
                uploadImageToFirebaseStorage(selectedImageUri, name, location)
            } else {
                showToast("Enter both the name and the location")
            }
        }

        builder.setNegativeButton("Dismiss") { dialog, _ ->
            dialog.cancel()
        }

        builder.show()
    }

    private fun uploadImageToFirebaseStorage(imageUri: Uri, name: String, location: String) {
        try {
            val storageRef = FirebaseStorage.getInstance().reference
            val imagesRef =
                storageRef.child("images/${UUID.randomUUID()}_${imageUri.lastPathSegment}")

            val uploadTask = imagesRef.putFile(imageUri)
            uploadTask.addOnSuccessListener { _ ->
                imagesRef.downloadUrl.addOnSuccessListener { uri ->
                    val imageUrl = uri.toString()
                    val newSign =
                        Sign(UUID.randomUUID().toString(), name, imageUrl, location, 0.0, false)
                    addSignToFirestore(newSign)
                }
            }.addOnFailureListener { e ->
                showToast("Error uploading image: $e")
            }
        } catch (e: Exception) {
            Log.e("FunnySignsActivity", "Error handling image: $e")
        }
    }


    private fun addSignToFirestore(sign: Sign) {
        db.collection("signs").document(sign.id)
            .set(sign.toMap())
            .addOnSuccessListener {
                fetchSignsFromFirestore()
            }
            .addOnFailureListener { e ->
                showToast("Error adding sign: $e")
            }
    }

    private fun fetchSignsFromFirestore() {
        db.collection("signs")
            .get()
            .addOnSuccessListener { querySnapshot ->
                val updatedSigns = mutableListOf<Sign>()
                for (document in querySnapshot) {
                    val sign = document.toObject(Sign::class.java)
                    updatedSigns.add(sign)
                }
                adapter.updateData(updatedSigns)
            }
            .addOnFailureListener { e ->
                showToast("Error fetching signs: $e")
            }
    }
}