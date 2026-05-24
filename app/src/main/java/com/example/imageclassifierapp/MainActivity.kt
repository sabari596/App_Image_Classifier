package com.example.imageclassifierapp

import ImageClassifier
import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var classifier: ImageClassifier
    private lateinit var imageView: ImageView
    private lateinit var resultText: TextView

    // Gallery launcher
    private val galleryLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                val bitmap =
                    MediaStore.Images.Media.getBitmap(contentResolver, it)
                showResult(bitmap)
            }
        }

    // Camera launcher
    private val cameraLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicturePreview()) {
            it?.let { showResult(it) }
        }

    // Camera permission launcher
    private val cameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                cameraLauncher.launch(null)
            } else {
                Toast.makeText(
                    this,
                    "Camera permission is required to use this feature",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        classifier = ImageClassifier(this)
        imageView = findViewById(R.id.imageView)
        resultText = findViewById(R.id.resultText)

        // Gallery button
        findViewById<Button>(R.id.btnGallery).setOnClickListener {
            galleryLauncher.launch("image/*")
        }

        // Camera button (FIXED)
        findViewById<Button>(R.id.btnCamera).setOnClickListener {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                cameraLauncher.launch(null)
            } else {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun showResult(bitmap: Bitmap) {
        imageView.setImageBitmap(bitmap)
        val (label, confidence) = classifier.classify(bitmap)
        resultText.text =
            "Prediction: $label\nConfidence: %.2f%%".format(confidence)
    }
}
