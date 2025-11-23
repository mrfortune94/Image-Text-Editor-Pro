package com.imagetexteditor.pro

import android.content.Intent
import android.graphics.*
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
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
    private var selectedBlockIndex: Int = -1  // Track which box is selected for editing
    
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
        setupImageViewTouchListener()
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
            binding.progressBar.visibility = android.view.View.GONE
            finish()
        }
    }
    
    private fun drawTextBoxes() {
        originalBitmap?.let { bitmap ->
            displayBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
            val canvas = Canvas(displayBitmap!!)
            
            detectedTextBlocks.forEachIndexed { index, textBlock ->
                // Use different colors for selected vs unselected boxes
                val isSelected = index == selectedBlockIndex
                val boxColor = if (isSelected) Color.GREEN else Color.RED
                val strokeWidth = if (isSelected) 5f else 3f
                
                val paint = Paint().apply {
                    style = Paint.Style.STROKE
                    this.strokeWidth = strokeWidth
                    color = boxColor
                }
                
                val textPaint = Paint().apply {
                    color = boxColor
                    textSize = 24f
                    isAntiAlias = true
                }
                
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
    
    private fun setupImageViewTouchListener() {
        binding.imageView.setOnTouchListener { view, event ->
            if (event.action == MotionEvent.ACTION_DOWN && detectedTextBlocks.isNotEmpty()) {
                handleImageTap(view, event.x, event.y)
                return@setOnTouchListener true
            }
            false
        }
    }
    
    private fun handleImageTap(view: View, x: Float, y: Float) {
        // Get the image matrix to map touch coordinates to bitmap coordinates
        val imageView = binding.imageView
        val drawable = imageView.drawable ?: return
        
        // Get the image bounds within the ImageView
        val matrix = imageView.imageMatrix
        val values = FloatArray(9)
        matrix.getValues(values)
        
        val scaleX = values[Matrix.MSCALE_X]
        val scaleY = values[Matrix.MSCALE_Y]
        val transX = values[Matrix.MTRANS_X]
        val transY = values[Matrix.MTRANS_Y]
        
        // Convert touch coordinates to bitmap coordinates
        val bitmapX = ((x - transX) / scaleX).toInt()
        val bitmapY = ((y - transY) / scaleY).toInt()
        
        // Check if tap is within any text block
        detectedTextBlocks.forEachIndexed { index, textBlock ->
            if (textBlock.boundingBox.contains(bitmapX, bitmapY)) {
                // User tapped on this text block
                selectedBlockIndex = index
                showEditTextDialog(textBlock, index)
                drawTextBoxes()  // Redraw to highlight selected box
                return
            }
        }
        
        // If no box was tapped, deselect
        if (selectedBlockIndex != -1) {
            selectedBlockIndex = -1
            drawTextBoxes()
        }
    }
    
    private fun showEditTextDialog(textBlock: TextBlock, index: Int) {
        // Inflate custom dialog layout
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_text, null)
        val editText = dialogView.findViewById<EditText>(R.id.etEditText)
        val fontPreview = dialogView.findViewById<TextView>(R.id.tvFontPreview)
        
        // Set current text
        editText.setText(textBlock.text)
        
        // Estimate font style based on bounding box
        val estimatedSize = FontMatcher.estimateFontSize(textBlock.boundingBox.height())
        val fontStyle = FontMatcher.matchFont(estimatedSize)
        
        // Apply font style to preview
        fontPreview.typeface = fontStyle.typeface
        fontPreview.textSize = fontStyle.size * 0.5f  // Scale down for preview
        fontPreview.text = textBlock.text
        
        // Update preview as user types
        editText.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                fontPreview.text = s?.toString() ?: ""
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })
        
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.edit_text_title)
            .setView(dialogView)
            .setPositiveButton(R.string.save) { _, _ ->
                val newText = editText.text.toString()
                if (newText.isNotEmpty()) {
                    replaceTextInImage(textBlock, newText, fontStyle, index)
                }
            }
            .setNegativeButton(R.string.cancel) { _, _ ->
                selectedBlockIndex = -1
                drawTextBoxes()
            }
            .setOnCancelListener {
                selectedBlockIndex = -1
                drawTextBoxes()
            }
            .show()
    }
    
    private fun replaceTextInImage(
        originalTextBlock: TextBlock,
        newText: String,
        fontStyle: FontMatcher.FontStyle,
        index: Int
    ) {
        originalBitmap?.let { bitmap ->
            // Create a mutable copy
            val resultBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
            val canvas = Canvas(resultBitmap)
            
            // First, cover the original text with white rectangle
            val erasePaint = Paint().apply {
                color = Color.WHITE
                style = Paint.Style.FILL
            }
            canvas.drawRect(originalTextBlock.boundingBox, erasePaint)
            
            // Now draw the new text in the same bounding box
            val textPaint = Paint().apply {
                color = Color.BLACK
                textSize = fontStyle.size
                typeface = fontStyle.typeface
                isAntiAlias = true
            }
            
            // Handle multi-line text
            val lines = newText.split("\n")
            val lineHeight = fontStyle.size * 1.2f  // Line spacing
            val boundingBox = originalTextBlock.boundingBox
            
            // Calculate starting Y position (vertically center in bounding box)
            val totalHeight = lines.size * lineHeight
            var startY = boundingBox.top + (boundingBox.height() - totalHeight) / 2 + fontStyle.size
            
            // Draw each line
            lines.forEach { line ->
                if (line.isNotEmpty()) {
                    // Calculate X position to center text (or align left)
                    val textWidth = textPaint.measureText(line)
                    val startX = boundingBox.left.toFloat()  // Left align
                    
                    // Make sure text fits in bounding box
                    if (startY <= boundingBox.bottom && startX >= boundingBox.left) {
                        canvas.drawText(line, startX, startY, textPaint)
                    }
                }
                startY += lineHeight
            }
            
            // Update the text block with new text
            detectedTextBlocks[index] = TextBlock(newText, originalTextBlock.boundingBox)
            
            // Update the display
            originalBitmap = resultBitmap
            selectedBlockIndex = -1
            drawTextBoxes()
            
            Toast.makeText(this, "Text updated", Toast.LENGTH_SHORT).show()
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
