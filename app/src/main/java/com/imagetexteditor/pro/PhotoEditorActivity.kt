package com.imagetexteditor.pro

import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.imagetexteditor.pro.databinding.ActivityPhotoEditorBinding
import ja.burhanrashid52.photoeditor.*
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class PhotoEditorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPhotoEditorBinding
    private lateinit var photoEditor: PhotoEditor
    private var sourceUri: Uri? = null

    private var currentTool = Tool.NONE
    private var eraserSize = 30f   // ← our manual eraser size store

    enum class Tool {
        NONE, BRUSH, TEXT, ERASER, EMOJI
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPhotoEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()

        sourceUri = intent.getParcelableExtra(EXTRA_IMAGE_URI)

        if (sourceUri == null) {
            Toast.makeText(this, "No image provided", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        loadImage()
        setupPhotoEditor()
        setupToolButtons()
        setupColorPicker()
        setupBrushSize()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = "Photo Editor"
        }
    }

    private fun loadImage() {
        try {
            val inputStream = contentResolver.openInputStream(sourceUri!!)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            binding.photoEditorView.source.setImageBitmap(bitmap)
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun setupPhotoEditor() {
        photoEditor = PhotoEditor.Builder(this, binding.photoEditorView)
            .setPinchTextScalable(true)
            .build()

        photoEditor.setOnPhotoEditorListener(object : OnPhotoEditorListener {
            override fun onAddViewListener(viewType: ViewType, numberOfAddedViews: Int) {}
            override fun onEditTextChangeListener(rootView: View, text: String, colorCode: Int) {}
            override fun onRemoveViewListener(viewType: ViewType, numberOfAddedViews: Int) {}
            override fun onStartViewChangeListener(viewType: ViewType) {}
            override fun onStopViewChangeListener(viewType: ViewType) {}
            override fun onTouchSourceImage(event: android.view.MotionEvent) {}
        })
    }

    private fun setupToolButtons() {
        binding.btnBrush.setOnClickListener {
            if (currentTool == Tool.BRUSH) {
                photoEditor.setBrushDrawingMode(false)
                currentTool = Tool.NONE
                binding.brushControls.visibility = View.GONE
            } else {
                photoEditor.setBrushDrawingMode(true)
                currentTool = Tool.BRUSH
                binding.brushControls.visibility = View.VISIBLE
                binding.eraserControls.visibility = View.GONE
                photoEditor.brushSize = 40f
            }
        }

        binding.btnEraser.setOnClickListener {
            currentTool = Tool.ERASER
            binding.eraserControls.visibility = View.VISIBLE
            binding.brushControls.visibility = View.GONE

            // Switch into eraser mode
            photoEditor.brushEraser()
            photoEditor.brushSize = eraserSize
        }

        binding.btnText.setOnClickListener { showAddTextDialog() }
        binding.btnEmoji.setOnClickListener { showEmojiDialog() }
        binding.btnUndo.setOnClickListener { photoEditor.undo() }
        binding.btnRedo.setOnClickListener { photoEditor.redo() }
        binding.btnSave.setOnClickListener { saveImage() }
    }

    private fun setupColorPicker() {
        val colors = listOf(
            Color.BLACK, Color.WHITE, Color.RED, Color.GREEN,
            Color.BLUE, Color.YELLOW, Color.CYAN, Color.MAGENTA
        )

        val adapter = ColorAdapter(colors) { color ->
            photoEditor.brushColor = color
        }

        binding.recyclerViewColors.apply {
            layoutManager = LinearLayoutManager(this@PhotoEditorActivity, LinearLayoutManager.HORIZONTAL, false)
            this.adapter = adapter
        }
    }

    private fun setupBrushSize() {
        binding.seekBarBrushSize.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (currentTool == Tool.BRUSH) {
                    photoEditor.brushSize = progress.toFloat()
                }
                binding.tvBrushSize.text = "Size: $progress"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        binding.seekBarEraserSize.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                eraserSize = progress.toFloat()
                if (currentTool == Tool.ERASER) {
                    photoEditor.brushSize = eraserSize
                }
                binding.tvEraserSize.text = "Size: $progress"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun showAddTextDialog() {
        val editText = android.widget.EditText(this)
        editText.hint = "Enter text"

        MaterialAlertDialogBuilder(this)
            .setTitle("Add Text")
            .setView(editText)
            .setPositiveButton("Add") { _, _ ->
                val text = editText.text.toString()
                if (text.isNotEmpty()) {
                    val textStyle = TextStyleBuilder().apply {
                        withTextColor(Color.BLACK)
                        withTextSize(40f)
                    }
                    photoEditor.addText(text, textStyle)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showEmojiDialog() {
        val emojis = listOf("😀", "😂", "😍", "😎", "🤔", "👍", "❤️", "🎉", "⭐", "🔥")

        MaterialAlertDialogBuilder(this)
            .setTitle("Select Emoji")
            .setItems(emojis.toTypedArray()) { _, which ->
                photoEditor.addEmoji(emojis[which])
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun saveImage() {
        try {
            binding.progressBar.visibility = View.VISIBLE

            val saveFile = File(cacheDir, "edited_${System.currentTimeMillis()}.jpg")

            photoEditor.saveAsFile(saveFile.absolutePath, object : PhotoEditor.OnSaveListener {
                override fun onSuccess(imagePath: String) {
                    runOnUiThread {
                        binding.progressBar.visibility = View.GONE
                        val file = File(imagePath)
                        val resultUri = Uri.fromFile(file)

                        val intent = Intent().apply {
                            putExtra(EXTRA_RESULT_URI, resultUri)
                        }
                        setResult(RESULT_OK, intent)
                        Toast.makeText(this@PhotoEditorActivity, "Image saved", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }

                override fun onFailure(exception: Exception) {
                    runOnUiThread {
                        binding.progressBar.visibility = View.GONE
                        Toast.makeText(this@PhotoEditorActivity, "Save failed: ${exception.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            })

        } catch (e: Exception) {
            binding.progressBar.visibility = View.GONE
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    companion object {
        const val EXTRA_IMAGE_URI = "extra_image_uri"
        const val EXTRA_RESULT_URI = "extra_result_uri"
    }
}
