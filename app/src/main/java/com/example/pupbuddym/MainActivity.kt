package com.example.pupbuddym

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentValues.TAG
import android.content.Context
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.pupbuddym.dto.HotSpot
import com.example.pupbuddym.dto.Photo
import com.example.pupbuddym.ui.main.MainViewModel
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract
import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity(), LocationListener {

    private lateinit var locationManager: LocationManager
    private lateinit var tvGpsLocation: TextView
    private lateinit var gpsButton: Button
    private lateinit var loginButton: Button

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private var user: FirebaseUser? = FirebaseAuth.getInstance().currentUser
    private var uri: Uri? = null
    private var currentImagePath: String = ""
    private var strUri by mutableStateOf("")
    private val viewModel by viewModels<MainViewModel>()
    private lateinit var imgView: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (user != null) {
            setAppContent()
        } else {
            setLoginContent()
        }

        Toast.makeText(this, "@string/success", Toast.LENGTH_SHORT).show()

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
    }

    private fun setLoginContent() {
        setContentView(R.layout.login)

        loginButton = findViewById(R.id.loginButton)
        loginButton.setOnClickListener {
            signIn()
        }
    }

    private fun setAppContent() {
        setContentView(R.layout.activity_main)

        imgView = findViewById(R.id.capturedImage)

        gpsButton = findViewById(R.id.getLocationButton)
        gpsButton.setOnClickListener {
            takePhoto()
        }
    }

    private fun hasCameraPermission() = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
    private fun hasExternalStoragePermission() = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
    private fun hasFineLocationPermission() = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)

    private fun signIn() {
        val providers = arrayListOf(
            AuthUI.IdpConfig.EmailBuilder().build()
        )
        val signInIntent = AuthUI.getInstance()
            .createSignInIntentBuilder()
            .setAvailableProviders(providers)
            .build()
        signInLauncher.launch(signInIntent)
    }

    private val signInLauncher = registerForActivityResult(
        FirebaseAuthUIActivityResultContract()
    ) { res ->
        this.signInResult(res)
    }

    private fun signInResult(result: FirebaseAuthUIAuthenticationResult) {
        val response = result.idpResponse
        if (result.resultCode == RESULT_OK) {
            user = FirebaseAuth.getInstance().currentUser
            setAppContent()
        } else {
            Log.e("MainActivity.kt", "Error logging in: " + response?.error?.errorCode)
        }
    }

    private fun takePhoto() {
        if (hasCameraPermission() == PERMISSION_GRANTED
            && hasExternalStoragePermission() == PERMISSION_GRANTED
            && hasFineLocationPermission() == PERMISSION_GRANTED
        ) {
            invokeGps()
            invokeCamera()
        } else {
            requestMultipleCameraPermissionsLauncher.launch(
                arrayOf(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.CAMERA,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            )
        }
    }

    private val requestMultipleCameraPermissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { resultsMap ->
            var permissionGranted = false
            resultsMap.forEach {
                if (it.value == true) {
                    permissionGranted = it.value
                } else {
                    permissionGranted = false
                    return@forEach
                }
            }
            if (permissionGranted) {
                invokeGps()
                invokeCamera()
            } else {
                Toast.makeText(
                    this,
                    getString(R.string.camera_permission_denied),
                    Toast.LENGTH_LONG
                )
                    .show()
            }
        }

    private fun invokeCamera() {
        val file = createImageFile()
        try {
            uri = FileProvider.getUriForFile(this, "com.example.pupbuddym.fileprovider", file)
        } catch (e: Exception) {
            Log.e(TAG, "Error: ${e.message}")
            var foo = e.message
        }
        getCameraImage.launch(uri)
    }

    private fun createImageFile(): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_hhmmss").format(Date())
        val imageDirectory = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "Breadcrumb_${timestamp}",
            ".jpg",
            imageDirectory
        ).apply {
            currentImagePath = absolutePath
        }
    }

    private val getCameraImage =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) {
                Log.i(TAG, "Image location: $uri")
                imgView.setImageURI(uri)
                strUri = uri.toString()
                val photo = Photo(localUri = uri.toString())
                viewModel.photos.add(photo)
            } else {
                Log.e(TAG, "Image not saved. $uri")
            }
        }

    @SuppressLint("MissingPermission")
    // suppressed because invokeGps() is only called if all permissions are present
    private fun invokeGps() {
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 5f, this)
    }

    override fun onLocationChanged(location: Location) {
        tvGpsLocation = findViewById(R.id.gpsTextView)
        tvGpsLocation.text = getString(R.string.lat_lon, location.latitude, location.longitude)
        val docSaver = setLatLong(location.latitude, location.longitude)
        viewModel.saveSpot(docSaver)
    }

    private fun setLatLong(lat: Double, long: Double): HotSpot {
        return HotSpot("", lat, long)
    }
}