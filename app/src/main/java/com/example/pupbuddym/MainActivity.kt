package com.example.pupbuddym

import android.Manifest
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import com.example.pupbuddym.ui.theme.PupBuddyMTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PupBuddyMTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    Greeting("Android")
                }
                Button(
                    onClick = {
                        takePhoto()
                    }
                ) {
                    Text(text = "Photo")
                }
            }
        }
    }

    private fun takePhoto() {
        if (hasCameraPermission() == PERMISSION_GRANTED && hasExternalStoragePermission() == PERMISSION_GRANTED) {
            invokeCamera()
        } else {

        }
    }

    private val requestMultiplePermissionsLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        resultsMap ->
        var permissionsGranted = false
        resultsMap.forEach {
            if (it.value == true) {

            }
        }
    }

    private fun invokeCamera() {
        TODO("Not yet implemented")
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
        PupBuddyMTheme {
            Greeting("Android")
        }
    }
}