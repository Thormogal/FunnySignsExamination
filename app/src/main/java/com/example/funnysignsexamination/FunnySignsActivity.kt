package com.example.funnysignsexamination

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
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
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.FirebaseStorage
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random
import java.util.UUID

class FunnySignsActivity : AppCompatActivity() {

    private lateinit var adapter: Adapter
    private lateinit var selectedImageUri: Uri
    private var remainingSigns: MutableList<Sign> = mutableListOf()
    private val handler = Handler(Looper.getMainLooper())
    private var isListCompletedToastShown = false
    private var photoUri: Uri? = null
    private val db = Firebase.firestore
    private val getContent =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                selectedImageUri = it
                showInputDialog()
            }
        }

    companion object {
        private const val REQUEST_CODE = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_funny_signs)
        setupViews()

        fetchSignsFromFirestore()
    }

    private fun setupViews() {
        setupRecyclerView()
        setupAddSignButton()
        setupRandomSignButton()
        setupAddThroughCameraButton()
    }

    private fun setupRecyclerView() {
        val recyclerView = findViewById<RecyclerView>(R.id.funnySignsRecyclerView)
        recyclerView.layoutManager = GridLayoutManager(this, 2)

        adapter = Adapter(mutableListOf(), object : Adapter.OnSignClickListener {
            override fun onSignClicked(fragment: DetailFragment) {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragmentContainer, fragment)
                    .addToBackStack(null)
                    .commit()
            }
        })
        recyclerView.adapter = adapter
    }

    private fun setupAddSignButton() {
        val addSignButton = findViewById<ImageButton>(R.id.signAddButton)
        addSignButton.setOnClickListener {
            openImagePicker()
        }
    }

    private fun setupRandomSignButton() {
        val randomSignButton = findViewById<ImageButton>(R.id.signRandomButton)
        randomSignButton.setOnClickListener {
            showRandomSignFragment()
        }
    }


    private fun showRandomSignFragment() {
        if (remainingSigns.isNotEmpty()) {
            val randomIndex = Random.nextInt(remainingSigns.size)
            val randomSign = remainingSigns[randomIndex]

            val fragment = DetailFragment().apply {
                arguments = Bundle().apply {
                    putString("image", randomSign.imageUrl)
                    putString("name", randomSign.name)
                    putString("location", randomSign.location)
                    putFloat("rating", randomSign.rating.toFloat())
                }
            }

            supportFragmentManager.popBackStack()

            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .addToBackStack(null)
                .commit()

            remainingSigns.removeAt(randomIndex)
        } else {
            if (!isListCompletedToastShown) {
                Toast.makeText(
                    this,
                    "You've completed the list of signs to show!",
                    Toast.LENGTH_SHORT
                ).show()
                isListCompletedToastShown = true

                handler.postDelayed({
                    isListCompletedToastShown = false
                }, 2000)
            }
        }
    }


    private fun setupAddThroughCameraButton() {
        val addThroughCameraButton = findViewById<ImageButton>(R.id.signCameraButton)
        addThroughCameraButton.setOnClickListener {
            if (checkCameraPermission(this)) {
                startCameraActivity(this, REQUEST_CODE)
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.CAMERA),
                    REQUEST_CODE
                )
            }
        }
    }

    private fun openImagePicker() {
        getContent.launch("image/*")
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        Log.d("Permissions", "onRequestPermissionsResult called")
        when (requestCode) {
            REQUEST_CODE -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    startCameraActivity(this, REQUEST_CODE)
                } else {
                    Toast.makeText(this, "Access needed to upload pictures", Toast.LENGTH_SHORT)
                        .show()
                }
                return
            }

            else -> {
            }
        }
    }

    private fun checkCameraPermission(activity: Activity): Boolean {
        return ContextCompat.checkSelfPermission(
            activity,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun startCameraActivity(activity: Activity, requestCode: Int) {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (intent.resolveActivity(activity.packageManager) != null) {
            photoUri = createImageUri(activity)
            intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
            activity.startActivityForResult(intent, requestCode)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE && resultCode == RESULT_OK) {
            photoUri?.let {
                selectedImageUri = it
                showInputDialog()
            }
        }
    }

    //creates an URI for the temporary picture
    @Throws(IOException::class)
    fun createImageUri(context: Context): Uri {
        val photoFile: File = createImageFile(context)
        return FileProvider.getUriForFile(
            context,
            "com.example.funnysignsexamination.fileprovider",
            photoFile
        )
    }

    //create a temporary picture in the app's specific space on the phone
    @Throws(IOException::class)
    fun createImageFile(context: Context): File {
        val timeStamp: String =
            SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = "JPEG_${timeStamp}_"

        val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)

        if (storageDir != null && storageDir.exists() || storageDir!!.mkdirs()) {
            return File.createTempFile(
                imageFileName,
                ".jpg",
                storageDir
            )
        } else {
            throw IOException("Failed to create directory or directory does not exist.")
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
                        Sign(
                            UUID.randomUUID().toString(),
                            name,
                            imageUrl,
                            location,
                            0.0,
                            listOf()
                        )
                    addSignToFirestore(newSign)
                }
            }.addOnFailureListener { e ->
                showToast("Error uploading image: $e")
            }
        } catch (e: Exception) {
            Log.e("FunnySignsActivity", "Error handling image: $e")
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

                remainingSigns = updatedSigns.toMutableList()
            }
            .addOnFailureListener { e ->
                showToast("Error fetching signs: $e")
            }
    }
}