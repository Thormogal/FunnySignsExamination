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
import android.widget.FrameLayout
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
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
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
    private lateinit var fragmentContainer: FrameLayout
    private var enteredSignName: String? = null
    private var remainingSigns: MutableList<Sign> = mutableListOf()
    private val handler = Handler(Looper.getMainLooper())
    private var isListCompletedToastShown = false
    private var photoUri: Uri? = null
    private var userPlacedMarker: LatLng? = null
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

        fragmentContainer = findViewById(R.id.fragmentContainer)

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
                    putDouble("latitude", randomSign.latitude)
                    putDouble("longitude", randomSign.longitude)
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

        try {
            if (requestCode == REQUEST_CODE && resultCode == RESULT_OK) {
                photoUri?.let {
                    selectedImageUri = it
                    showInputDialog()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            showToast("Error handling result: ${e.message}")
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
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = "JPEG_${timeStamp}_"

        val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)

        if (storageDir != null && (storageDir.exists() || storageDir.mkdirs())) {
            val imageFile = File.createTempFile(imageFileName, ".jpg", storageDir)
            Log.d("CameraWtf", "Created image file: ${imageFile.absolutePath}")
            return imageFile
        } else {
            throw IOException("Failed to create directory or directory does not exist.")
        }
    }

    private fun setupMapContainer(contextView: LinearLayout) {
        val mapContainer = FrameLayout(this)
        mapContainer.layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        )
        contextView.addView(mapContainer)

        mapContainer.setOnClickListener(null)
        mapContainer.isClickable = true
        mapContainer.isFocusable = true

        val mapFragment = SupportMapFragment()
        supportFragmentManager.beginTransaction().replace(fragmentContainer.id, mapFragment)
            .commit()

        setupMapClickListener(mapFragment)
    }

    private fun setupMapClickListener(mapFragment: SupportMapFragment) {
        val markerOptions = MarkerOptions()

        mapFragment.getMapAsync { googleMap ->
            Log.d("maperror", "MapAsync callback triggered")

            googleMap.setOnMapClickListener { latLng ->
                markerOptions.position(latLng)
                googleMap.clear()
                googleMap.addMarker(markerOptions)

                userPlacedMarker = latLng

                Log.d("maperror", "Selected LatLng: $latLng")

                if (enteredSignName != null && userPlacedMarker != null) {
                    val latitude = userPlacedMarker!!.latitude
                    val longitude = userPlacedMarker!!.longitude
                    val location = "Lat: $latitude, Long: $longitude"

                    if (enteredSignName!!.isNotBlank()) {
                        Log.d("maperror", "Upload to Firebase: $enteredSignName, $location")
                        uploadImageToFirebaseStorage(selectedImageUri, enteredSignName!!, location)
                    }
                }
                supportFragmentManager.beginTransaction().remove(mapFragment).commit()
            }
        }
    }

    private fun showInputDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("It's a siiiign!!")

        val contextView = LinearLayout(this)
        contextView.orientation = LinearLayout.VERTICAL

        val nameEditText = EditText(this)
        nameEditText.hint = "Name of the sign"
        contextView.addView(nameEditText)
        nameEditText.filters = arrayOf<InputFilter>(InputFilter.LengthFilter(20))

        setupMapContainer(contextView)

        builder.setNegativeButton("Dismiss") { dialog, _ ->
            dialog.cancel()
        }

        builder.setPositiveButton("OK") { _, _ ->
            val name = nameEditText.text.toString()

            Log.d("maperror", "Name: $name")

            enteredSignName = name

            if (userPlacedMarker != null) {
                val latitude = userPlacedMarker!!.latitude
                val longitude = userPlacedMarker!!.longitude
                val location = "Lat: $latitude, Long: $longitude"

                if (name.isNotBlank()) {
                    Log.d("maperror", "Upload to Firebase: $name, $location")
                    uploadImageToFirebaseStorage(selectedImageUri, name, location)
                } else {
                    Log.d("maperror", "Name is blank")
                    showToast("Enter the name")
                }
            } else {
                Log.d("maperror", "No position selected")
                showToast("Select a position on the map")
            }
        }
        builder.setView(contextView)
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
                            latitude = userPlacedMarker!!.latitude,
                            longitude = userPlacedMarker!!.longitude,
                            0.0,
                            listOf()
                        )
                    addSignToFirestore(newSign)
                }.addOnFailureListener { e ->
                    showToast("Error getting image URL: $e")
                    Log.e("eUploading", "Error getting image URL: $e")
                }
            }.addOnFailureListener { e ->
                showToast("Error uploading image: $e")
                Log.e("eUploading", "Error uploading image: $e")
            }
        } catch (e: Exception) {
            showToast("Error handling image: $e")
            Log.e("eUploading", "Error handling image: $e")
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