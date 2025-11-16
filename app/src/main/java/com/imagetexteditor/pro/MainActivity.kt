package com.imagetexteditor.pro

import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import com.imagetexteditor.pro.databinding.ActivityMainBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private var currentImageUri: Uri? = null
    
    companion object {
        private const val REQUEST_CROP = 1001
        private const val REQUEST_FILTER = 1002
        private const val REQUEST_TEXT_DETECTION = 1003
        private const val REQUEST_PHOTO_EDITOR = 1004
        private const val REQUEST_ADJUST = 1005
    }
    
    // Activity result launcher for image picker
    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.data?.let { uri ->
                currentImageUri = uri
                loadImage(uri)
            }
        }
    }
    
    // Permission launcher
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            openImagePicker()
        } else {
            Toast.makeText(this, "Storage permission is required", Toast.LENGTH_LONG).show()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Enable dark mode by default
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupToolbar()
        setupClickListeners()
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = getString(R.string.app_name)
    }
    
    private fun setupClickListeners() {
        // Open image button
        binding.btnOpenImage.setOnClickListener {
            checkPermissionsAndOpenPicker()
        }
        
        // Crop button
        binding.btnCrop.setOnClickListener {
            currentImageUri?.let { uri ->
                val intent = Intent(this, CropActivity::class.java).apply {
                    putExtra(CropActivity.EXTRA_IMAGE_URI, uri)
                }
                startActivityForResult(intent, REQUEST_CROP)
            } ?: showNoImageDialog()
        }
        
        // Filter button
        binding.btnFilter.setOnClickListener {
            currentImageUri?.let { uri ->
                val intent = Intent(this, FilterActivity::class.java).apply {
                    putExtra(FilterActivity.EXTRA_IMAGE_URI, uri)
                }
                startActivityForResult(intent, REQUEST_FILTER)
            } ?: showNoImageDialog()
        }
        
        // Text detection (OCR) button
        binding.btnDetectText.setOnClickListener {
            currentImageUri?.let { uri ->
                val intent = Intent(this, TextDetectionActivity::class.java).apply {
                    putExtra(TextDetectionActivity.EXTRA_IMAGE_URI, uri)
                }
                startActivityForResult(intent, REQUEST_TEXT_DETECTION)
            } ?: showNoImageDialog()
        }
        
        // Draw button
        binding.btnDraw.setOnClickListener {
            currentImageUri?.let { uri ->
                val intent = Intent(this, PhotoEditorActivity::class.java).apply {
                    putExtra(PhotoEditorActivity.EXTRA_IMAGE_URI, uri)
                }
                startActivityForResult(intent, REQUEST_PHOTO_EDITOR)
            } ?: showNoImageDialog()
        }
        
        // Sticker button
        binding.btnSticker.setOnClickListener {
            currentImageUri?.let { uri ->
                val intent = Intent(this, PhotoEditorActivity::class.java).apply {
                    putExtra(PhotoEditorActivity.EXTRA_IMAGE_URI, uri)
                }
                startActivityForResult(intent, REQUEST_PHOTO_EDITOR)
            } ?: showNoImageDialog()
        }
        
        // Adjust button
        binding.btnAdjust.setOnClickListener {
            currentImageUri?.let { uri ->
                val intent = Intent(this, AdjustActivity::class.java).apply {
                    putExtra(AdjustActivity.EXTRA_IMAGE_URI, uri)
                }
                startActivityForResult(intent, REQUEST_ADJUST)
            } ?: showNoImageDialog()
        }
        
        // Save button
        binding.btnSave.setOnClickListener {
            currentImageUri?.let { uri ->
                saveImageToGallery(uri)
            } ?: showNoImageDialog()
        }
        
        // Theme toggle button
        binding.btnThemeToggle.setOnClickListener {
            toggleTheme()
        }
    }
    
    private fun checkPermissionsAndOpenPicker() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        
        val allPermissionsGranted = permissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
        
        if (allPermissionsGranted) {
            openImagePicker()
        } else {
            permissionLauncher.launch(permissions)
        }
    }
    
    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        imagePickerLauncher.launch(intent)
    }
    
    private fun loadImage(uri: Uri) {
        binding.imageView.setImageURI(uri)
        binding.tvNoImage.visibility = android.view.View.GONE
        binding.imageView.visibility = android.view.View.VISIBLE
        enableEditingButtons(true)
    }
    
    private fun enableEditingButtons(enabled: Boolean) {
        binding.btnCrop.isEnabled = enabled
        binding.btnFilter.isEnabled = enabled
        binding.btnDetectText.isEnabled = enabled
        binding.btnDraw.isEnabled = enabled
        binding.btnSticker.isEnabled = enabled
        binding.btnAdjust.isEnabled = enabled
        binding.btnSave.isEnabled = enabled
    }
    
    private fun showNoImageDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("No Image")
            .setMessage("Please select an image first")
            .setPositiveButton("OK", null)
            .show()
    }
    
    private fun toggleTheme() {
        val currentNightMode = AppCompatDelegate.getDefaultNightMode()
        if (currentNightMode == AppCompatDelegate.MODE_NIGHT_YES) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        }
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        if (resultCode == RESULT_OK && data != null) {
            when (requestCode) {
                REQUEST_CROP, REQUEST_FILTER, REQUEST_TEXT_DETECTION, 
                REQUEST_PHOTO_EDITOR, REQUEST_ADJUST -> {
                    val resultUri = data.getParcelableExtra<Uri>(CropActivity.EXTRA_RESULT_URI)
                    resultUri?.let {
                        currentImageUri = it
                        loadImage(it)
                    }
                }
            }
        }
    }
    
    private fun saveImageToGallery(uri: Uri) {
        try {
            val inputStream = contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
            
            val filename = "IMG_EDIT_${System.currentTimeMillis()}.jpg"
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                }
                
                val imageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                imageUri?.let {
                    contentResolver.openOutputStream(it)?.use { outputStream ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream)
                    }
                    Toast.makeText(this, "Image saved to gallery", Toast.LENGTH_SHORT).show()
                }
            } else {
                val imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                val imageFile = File(imagesDir, filename)
                FileOutputStream(imageFile).use { outputStream ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream)
                }
                
                // Notify gallery
                val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
                mediaScanIntent.data = Uri.fromFile(imageFile)
                sendBroadcast(mediaScanIntent)
                
                Toast.makeText(this, "Image saved to gallery", Toast.LENGTH_SHORT).show()
            }
            
            bitmap.recycle()
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to save image: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
