package com.imagetexteditor.pro

import android.content.Intent
import android.graphics.*
import android.net.Uri
import android.os.Bundle
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.imagetexteditor.pro.databinding.ActivityAdjustBinding
import java.io.File
import java.io.FileOutputStream

class AdjustActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityAdjustBinding
    private var sourceUri: Uri? = null
    private var originalBitmap: Bitmap? = null
    private var adjustedBitmap: Bitmap? = null
    
    private var brightness = 0f
    private var contrast = 1f
    private var saturation = 1f
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdjustBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupToolbar()
        
        sourceUri = intent.getParcelableExtra(EXTRA_IMAGE_URI)
        
        if (sourceUri == null) {
            Toast.makeText(this, "No image provided", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        loadImage()
        setupControls()
        setupButtons()
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = "Adjust Image"
        }
    }
    
    private fun loadImage() {
        try {
            val inputStream = contentResolver.openInputStream(sourceUri!!)
            originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
            
            originalBitmap?.let {
                adjustedBitmap = it.copy(Bitmap.Config.ARGB_8888, true)
                binding.imageView.setImageBitmap(adjustedBitmap)
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
    
    private fun setupControls() {
        // Brightness control (-100 to 100, default 0)
        binding.seekBarBrightness.max = 200
        binding.seekBarBrightness.progress = 100
        binding.seekBarBrightness.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                brightness = (progress - 100) / 100f * 255
                binding.tvBrightness.text = "Brightness: ${progress - 100}"
                applyAdjustments()
            }
            
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        
        // Contrast control (0 to 2, default 1)
        binding.seekBarContrast.max = 200
        binding.seekBarContrast.progress = 100
        binding.seekBarContrast.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                contrast = progress / 100f
                binding.tvContrast.text = "Contrast: ${"%.2f".format(contrast)}"
                applyAdjustments()
            }
            
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        
        // Saturation control (0 to 2, default 1)
        binding.seekBarSaturation.max = 200
        binding.seekBarSaturation.progress = 100
        binding.seekBarSaturation.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                saturation = progress / 100f
                binding.tvSaturation.text = "Saturation: ${"%.2f".format(saturation)}"
                applyAdjustments()
            }
            
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }
    
    private fun applyAdjustments() {
        originalBitmap?.let { bitmap ->
            adjustedBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
            
            val colorMatrix = ColorMatrix()
            
            // Apply brightness
            if (brightness != 0f) {
                val brightnessMatrix = ColorMatrix(floatArrayOf(
                    1f, 0f, 0f, 0f, brightness,
                    0f, 1f, 0f, 0f, brightness,
                    0f, 0f, 1f, 0f, brightness,
                    0f, 0f, 0f, 1f, 0f
                ))
                colorMatrix.postConcat(brightnessMatrix)
            }
            
            // Apply contrast
            if (contrast != 1f) {
                val scale = contrast
                val translate = (-.5f * scale + .5f) * 255f
                val contrastMatrix = ColorMatrix(floatArrayOf(
                    scale, 0f, 0f, 0f, translate,
                    0f, scale, 0f, 0f, translate,
                    0f, 0f, scale, 0f, translate,
                    0f, 0f, 0f, 1f, 0f
                ))
                colorMatrix.postConcat(contrastMatrix)
            }
            
            // Apply saturation
            if (saturation != 1f) {
                val saturationMatrix = ColorMatrix()
                saturationMatrix.setSaturation(saturation)
                colorMatrix.postConcat(saturationMatrix)
            }
            
            val paint = Paint()
            paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
            
            val canvas = Canvas(adjustedBitmap!!)
            canvas.drawBitmap(bitmap, 0f, 0f, paint)
            
            binding.imageView.setImageBitmap(adjustedBitmap)
        }
    }
    
    private fun setupButtons() {
        binding.btnReset.setOnClickListener {
            brightness = 0f
            contrast = 1f
            saturation = 1f
            
            binding.seekBarBrightness.progress = 100
            binding.seekBarContrast.progress = 100
            binding.seekBarSaturation.progress = 100
            
            originalBitmap?.let {
                adjustedBitmap = it.copy(Bitmap.Config.ARGB_8888, true)
                binding.imageView.setImageBitmap(adjustedBitmap)
            }
        }
        
        binding.btnApply.setOnClickListener {
            saveAdjustedImage()
        }
        
        binding.btnCancel.setOnClickListener {
            finish()
        }
    }
    
    private fun saveAdjustedImage() {
        try {
            val bitmap = adjustedBitmap ?: originalBitmap
            
            if (bitmap == null) {
                Toast.makeText(this, "No image to save", Toast.LENGTH_SHORT).show()
                return
            }
            
            val file = File(cacheDir, "adjusted_${System.currentTimeMillis()}.jpg")
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
            Toast.makeText(this, "Failed to save image", Toast.LENGTH_SHORT).show()
        }
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
    
    override fun onDestroy() {
        super.onDestroy()
        originalBitmap?.recycle()
        adjustedBitmap?.recycle()
    }
    
    companion object {
        const val EXTRA_IMAGE_URI = "extra_image_uri"
        const val EXTRA_RESULT_URI = "extra_result_uri"
    }
}
