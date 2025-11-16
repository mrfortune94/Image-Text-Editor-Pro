package com.imagetexteditor.pro

import android.content.Intent
import android.graphics.*
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.imagetexteditor.pro.databinding.ActivityTextDetectionBinding
import java.io.File
import java.io.FileOutputStream

class TextDetectionActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityTextDetectionBinding
    private var sourceUri: Uri? = null
    private var originalBitmap: Bitmap? = null
    private var displayBitmap: Bitmap? = null
    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private val detectedTextBlocks = mutableListOf<TextBlock>()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTextDetectionBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupToolbar()
        
        sourceUri = intent.getParcelableExtra(EXTRA_IMAGE_URI)
        
        if (sourceUri == null) {
            Toast.makeText(this, "No image provided", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        loadAndDetectText()
        setupButtons()
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = "Text Detection"
        }
    }
    
    private fun loadAndDetectText() {
        try {
            binding.progressBar.visibility = android.view.View.VISIBLE
            
            val inputStream = contentResolver.openInputStream(sourceUri!!)
            originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
            
            originalBitmap?.let { bitmap ->
                val image = InputImage.fromBitmap(bitmap, 0)
                
                textRecognizer.process(image)
                    .addOnSuccessListener { visionText ->
                        detectedTextBlocks.clear()
                        
                        for (block in visionText.textBlocks) {
                            val boundingBox = block.boundingBox ?: continue
                            val text = block.text
                            detectedTextBlocks.add(TextBlock(text, boundingBox))
                        }
                        
                        if (detectedTextBlocks.isEmpty()) {
                            Toast.makeText(this, "No text detected in image", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(this, "Found ${detectedTextBlocks.size} text blocks", Toast.LENGTH_SHORT).show()
                        }
                        
                        drawTextBoxes()
                        binding.progressBar.visibility = android.view.View.GONE
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Text detection failed: ${e.message}", Toast.LENGTH_LONG).show()
                        binding.progressBar.visibility = android.view.View.GONE
                    }
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to load image: ${e.message}", Toast.LENGTH_SHORT).show()
            binding.progressBar.visibility = android.view.View.GONE()
            finish()
        }
    }
    
    private fun drawTextBoxes() {
        originalBitmap?.let { bitmap ->
            displayBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
            val canvas = Canvas(displayBitmap!!)
            
            val paint = Paint().apply {
                style = Paint.Style.STROKE
                strokeWidth = 3f
                color = Color.RED
            }
            
            val textPaint = Paint().apply {
                color = Color.RED
                textSize = 24f
                isAntiAlias = true
            }
            
            detectedTextBlocks.forEachIndexed { index, textBlock ->
                canvas.drawRect(textBlock.boundingBox, paint)
                
                // Draw label
                val label = "#${index + 1}"
                canvas.drawText(
                    label,
                    textBlock.boundingBox.left.toFloat(),
                    textBlock.boundingBox.top.toFloat() - 5,
                    textPaint
                )
            }
            
            binding.imageView.setImageBitmap(displayBitmap)
        }
    }
    
    private fun setupButtons() {
        binding.btnRemoveAllText.setOnClickListener {
            removeAllText()
        }
        
        binding.btnCancel.setOnClickListener {
            finish()
        }
    }
    
    private fun removeAllText() {
        originalBitmap?.let { bitmap ->
            val resultBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
            val canvas = Canvas(resultBitmap)
            
            val paint = Paint().apply {
                color = Color.WHITE
                style = Paint.Style.FILL
            }
            
            // Simple inpainting: fill text areas with white
            detectedTextBlocks.forEach { textBlock ->
                canvas.drawRect(textBlock.boundingBox, paint)
            }
            
            saveResult(resultBitmap)
        }
    }
    
    private fun saveResult(bitmap: Bitmap) {
        try {
            val file = File(cacheDir, "text_edited_${System.currentTimeMillis()}.jpg")
            val outputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream)
            outputStream.flush()
            outputStream.close()
            
            val resultUri = Uri.fromFile(file)
            val intent = Intent().apply {
                putExtra(EXTRA_RESULT_URI, resultUri)
            }
            setResult(RESULT_OK, intent)
            finish()
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to save image: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
    
    override fun onDestroy() {
        super.onDestroy()
        textRecognizer.close()
        originalBitmap?.recycle()
        displayBitmap?.recycle()
    }
    
    companion object {
        const val EXTRA_IMAGE_URI = "extra_image_uri"
        const val EXTRA_RESULT_URI = "extra_result_uri"
    }
    
    data class TextBlock(val text: String, val boundingBox: Rect)
}
