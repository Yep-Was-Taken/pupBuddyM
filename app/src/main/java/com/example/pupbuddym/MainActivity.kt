package com.example.pupbuddym

import android.Manifest
import android.content.ContentValues.TAG
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.app.ActivityCompat
import com.example.pupbuddym.ui.theme.PupBuddyMTheme
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.Task
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModelProvider
import coil.compose.AsyncImage
import com.example.pupbuddym.dto.Photo
import com.example.pupbuddym.ui.main.MainViewModel
import com.example.pupbuddym.ui.theme.PupBuddyMTheme
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract
import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
  
    lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private var user: FirebaseUser? = FirebaseAuth.getInstance().currentUser
    private var uri: Uri? = null
    private var currentImagePath: String = ""
    private var strUri by mutableStateOf("")
    val viewModel by viewModels<MainViewModel>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Toast.makeText(applicationContext,"We did it",Toast.LENGTH_SHORT).show();

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        findViewById<Button>(R.id.btn_get_location).setOnClickListener {
            fetchLocation()
        }
    }

    private fun fetchLocation() {
        if(ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            {
                ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), 101)
                return
            }
        fusedLocationProviderClient.lastLocation.addOnSuccessListener {
            if(it != null){
                Toast.makeText(applicationContext, "${it.latitude} ${it.longitude}", Toast.LENGTH_SHORT).show()
            }
        }

    }

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
    ) {
        res -> this.signInResult(res)
    }

    private fun signInResult(result: FirebaseAuthUIAuthenticationResult) {
        val response = result.idpResponse
        if (result.resultCode == RESULT_OK) {
            user = FirebaseAuth.getInstance().currentUser
        } else {
            Log.e("MainActivity.kt", "Error logging in: " + response?.error?.errorCode)
        }
    }

    private fun takePhoto() {
        if (hasCameraPermission() == PERMISSION_GRANTED && hasExternalStoragePermission() == PERMISSION_GRANTED) {
            invokeCamera()
        } else {
            requestMultiplePermissionsLauncher.launch(arrayOf(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.CAMERA
            ))
        }
    }

    private val requestMultiplePermissionsLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        resultsMap ->
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
            invokeCamera()
        } else {
            Toast.makeText(this, getString(R.string.cameraPermissionDenied), Toast.LENGTH_LONG).show()
        }
    }

    private fun invokeCamera() {
        var file = createImageFile()
        try {
            uri = FileProvider.getUriForFile(this, "com.example.pupbuddym.fileprovider", file)
        } catch (e: Exception) {
            Log.e(TAG, "Error: ${e.message}")
            var foo = e.message
        }
        getCameraImage.launch(uri)
    }

    private fun createImageFile() : File {
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

    private val getCameraImage = registerForActivityResult(ActivityResultContracts.TakePicture()) {
        success ->
        if (success) {
            Log.i(TAG, "Image location: $uri")
            strUri = uri.toString()
            val photo = Photo(localUri = uri.toString())
            viewModel.photos.add(photo)
        } else {
            Log.e(TAG, "Image not saved. $uri")
        }
    }

    fun hasCameraPermission() = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
    fun hasExternalStoragePermission() = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)

    @Composable
    fun Greeting(name: String) {
        Text(text = "Hello $name!")
    }

    @Preview(showBackground = true)
    @Composable
    fun DefaultPreview() {
        Column() {
            Row() {
                Button(
                    onClick = { signIn() }
                ) {
                    Text(text = "Sign in")
                }
            }
            Row() {
                Greeting("Roxie")
                Button(
                    onClick = {
                        takePhoto()
                    }
                ) {
                    Text(text = "Photo")
                }
            }
            Row() {
                AsyncImage(model = strUri, contentDescription = "Dog image")
                Text(text = "Dog image")
            }
            Row() {
                Text(text = "dog")
            }
        }
    }