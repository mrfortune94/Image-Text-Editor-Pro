package com.imagetexteditor.pro

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.imagetexteditor.pro.databinding.ActivityFilterBinding
import jp.co.cyberagent.android.gpuimage.GPUImage
import jp.co.cyberagent.android.gpuimage.filter.*
import java.io.File
import java.io.FileOutputStream

class FilterActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityFilterBinding
    private lateinit var gpuImage: GPUImage
    private var sourceUri: Uri? = null
    private var originalBitmap: Bitmap? = null
    private var filteredBitmap: Bitmap? = null
    
    private val filters = listOf(
        FilterItem("Original", null),
        FilterItem("Grayscale", GPUImageGrayscaleFilter()),
        FilterItem("Sepia", GPUImageSepiaToneFilter()),
        FilterItem("Contrast", GPUImageContrastFilter(1.5f)),
        FilterItem("Brightness", GPUImageBrightnessFilter(0.2f)),
        FilterItem("Saturation", GPUImageSaturationFilter(1.5f)),
        FilterItem("Sharpen", GPUImageSharpenFilter()),
        FilterItem("Emboss", GPUImageEmbossFilter()),
        FilterItem("Posterize", GPUImagePosterizeFilter(5)),
        FilterItem("Pixelate", GPUImagePixelationFilter()),
        FilterItem("Sketch", GPUImageSketchFilter()),
        FilterItem("Toon", GPUImageToonFilter()),
        FilterItem("Invert", GPUImageColorInvertFilter()),
        FilterItem("Vignette", GPUImageVignetteFilter())
    )
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFilterBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupToolbar()
        
        sourceUri = intent.getParcelableExtra(EXTRA_IMAGE_URI)
        
        if (sourceUri == null) {
            Toast.makeText(this, "No image provided", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        gpuImage = GPUImage(this)
        loadImage()
        setupFilterList()
        setupButtons()
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = "Apply Filter"
        }
    }
    
    private fun loadImage() {
        try {
            val inputStream = contentResolver.openInputStream(sourceUri!!)
            originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
            
            originalBitmap?.let {
                gpuImage.setImage(it)
                binding.imageView.setImageBitmap(it)
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
    
    private fun setupFilterList() {
        val adapter = FilterAdapter(filters) { filter ->
            applyFilter(filter)
        }
        
        binding.recyclerViewFilters.apply {
            layoutManager = LinearLayoutManager(this@FilterActivity, LinearLayoutManager.HORIZONTAL, false)
            this.adapter = adapter
        }
    }
    
    private fun applyFilter(filterItem: FilterItem) {
        originalBitmap?.let { bitmap ->
            if (filterItem.filter == null) {
                binding.imageView.setImageBitmap(bitmap)
                filteredBitmap = bitmap
            } else {
                gpuImage.setFilter(filterItem.filter)
                filteredBitmap = gpuImage.bitmapWithFilterApplied
                binding.imageView.setImageBitmap(filteredBitmap)
            }
        }
    }
    
    private fun setupButtons() {
        binding.btnApply.setOnClickListener {
            saveFilteredImage()
        }
        
        binding.btnCancel.setOnClickListener {
            finish()
        }
    }
    
    private fun saveFilteredImage() {
        try {
            val bitmap = filteredBitmap ?: originalBitmap
            
            if (bitmap == null) {
                Toast.makeText(this, "No image to save", Toast.LENGTH_SHORT).show()
                return
            }
            
            val file = File(cacheDir, "filtered_${System.currentTimeMillis()}.jpg")
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
        filteredBitmap?.recycle()
    }
    
    companion object {
        const val EXTRA_IMAGE_URI = "extra_image_uri"
        const val EXTRA_RESULT_URI = "extra_result_uri"
    }
    
    data class FilterItem(val name: String, val filter: GPUImageFilter?)
}

class FilterAdapter(
    private val filters: List<FilterActivity.FilterItem>,
    private val onFilterClick: (FilterActivity.FilterItem) -> Unit
) : RecyclerView.Adapter<FilterAdapter.FilterViewHolder>() {
    
    private var selectedPosition = 0
    
    class FilterViewHolder(val binding: com.imagetexteditor.pro.databinding.ItemFilterBinding) :
        RecyclerView.ViewHolder(binding.root)
    
    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): FilterViewHolder {
        val binding = com.imagetexteditor.pro.databinding.ItemFilterBinding.inflate(
            android.view.LayoutInflater.from(parent.context), parent, false
        )
        return FilterViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: FilterViewHolder, position: Int) {
        val filter = filters[position]
        holder.binding.tvFilterName.text = filter.name
        
        holder.binding.root.isSelected = position == selectedPosition
        holder.binding.root.alpha = if (position == selectedPosition) 1.0f else 0.6f
        
        holder.binding.root.setOnClickListener {
            val oldPosition = selectedPosition
            selectedPosition = position
            notifyItemChanged(oldPosition)
            notifyItemChanged(selectedPosition)
            onFilterClick(filter)
        }
    }
    
    override fun getItemCount() = filters.size
}
