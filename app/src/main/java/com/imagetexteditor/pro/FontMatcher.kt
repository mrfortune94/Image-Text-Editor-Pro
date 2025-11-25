package com.imagetexteditor.pro

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import android.graphics.Typeface

/**
 * Utility class to match detected text properties to available system fonts.
 * Analyzes image pixels to detect font characteristics and colors.
 */
object FontMatcher {
    
    // Heuristic: font size is approximately 75% of bounding box height
    private const val FONT_SIZE_RATIO = 0.75f
    
    // Threshold for determining if a color is "dark" (text) vs "light" (background)
    private const val LUMINANCE_THRESHOLD = 0.5f
    
    // Stroke width threshold for detecting bold text
    private const val BOLD_STROKE_RATIO = 0.12f
    
    data class FontStyle(
        val typeface: Typeface,
        val size: Float,
        val isBold: Boolean = false,
        val isItalic: Boolean = false,
        val textColor: Int = Color.BLACK,
        val backgroundColor: Int = Color.WHITE
    )
    
    /**
     * Attempts to match font characteristics to a Typeface.
     * Falls back to default fonts if matching fails.
     * 
     * @param estimatedSize The estimated font size based on bounding box height
     * @param isBold Whether the text appears bold (future enhancement)
     * @param isItalic Whether the text appears italic (future enhancement)
     * @return FontStyle with matched typeface and properties
     */
    fun matchFont(
        estimatedSize: Float,
        isBold: Boolean = false,
        isItalic: Boolean = false
    ): FontStyle {
        // Determine typeface style
        val style = when {
            isBold && isItalic -> Typeface.BOLD_ITALIC
            isBold -> Typeface.BOLD
            isItalic -> Typeface.ITALIC
            else -> Typeface.NORMAL
        }
        
        // Create typeface (using default system fonts)
        val typeface = Typeface.create(Typeface.DEFAULT, style)
        
        // Clamp font size to reasonable range (10-100)
        val clampedSize = estimatedSize.coerceIn(10f, 100f)
        
        return FontStyle(
            typeface = typeface,
            size = clampedSize,
            isBold = isBold,
            isItalic = isItalic
        )
    }
    
    /**
     * Estimates font size based on bounding box height.
     * Uses a heuristic: font size is approximately 75% of bounding box height.
     * 
     * @param boundingBoxHeight The height of the text bounding box
     * @return Estimated font size
     */
    fun estimateFontSize(boundingBoxHeight: Int): Float {
        // Font size is roughly 75% of bounding box height
        val estimatedSize = boundingBoxHeight * FONT_SIZE_RATIO
        return estimatedSize.coerceIn(10f, 100f)
    }
    
    /**
     * Gets the default font style.
     * Used when no font properties can be detected.
     * 
     * @return Default FontStyle
     */
    fun getDefaultFont(): FontStyle {
        return FontStyle(
            typeface = Typeface.DEFAULT,
            size = 40f,
            isBold = false,
            isItalic = false
        )
    }
    
    /**
     * Analyzes a bitmap region to detect font properties from the actual image pixels.
     * This provides auto-detection of text color, background color, and font weight.
     * 
     * @param bitmap The source bitmap containing the text
     * @param boundingBox The bounding box of the detected text
     * @return FontStyle with detected colors and estimated font properties
     */
    fun analyzeTextRegion(bitmap: Bitmap, boundingBox: Rect): FontStyle {
        // Ensure bounding box is within bitmap bounds
        val safeRect = Rect(
            boundingBox.left.coerceIn(0, bitmap.width - 1),
            boundingBox.top.coerceIn(0, bitmap.height - 1),
            boundingBox.right.coerceIn(1, bitmap.width),
            boundingBox.bottom.coerceIn(1, bitmap.height)
        )
        
        // Make sure we have valid dimensions
        if (safeRect.width() <= 0 || safeRect.height() <= 0) {
            return getDefaultFont()
        }
        
        // Analyze colors in the text region
        val colorAnalysis = analyzeColors(bitmap, safeRect)
        
        // Detect if text appears bold based on stroke density
        val isBold = detectBoldText(bitmap, safeRect, colorAnalysis.textColor, colorAnalysis.backgroundColor)
        
        // Estimate font size from bounding box height
        val fontSize = estimateFontSize(safeRect.height())
        
        // Create typeface with detected style
        val style = if (isBold) Typeface.BOLD else Typeface.NORMAL
        val typeface = Typeface.create(Typeface.DEFAULT, style)
        
        return FontStyle(
            typeface = typeface,
            size = fontSize,
            isBold = isBold,
            isItalic = false,
            textColor = colorAnalysis.textColor,
            backgroundColor = colorAnalysis.backgroundColor
        )
    }
    
    /**
     * Data class to hold color analysis results
     */
    private data class ColorAnalysis(
        val textColor: Int,
        val backgroundColor: Int
    )
    
    /**
     * Analyzes colors in a region to detect text and background colors.
     * Uses luminance-based clustering to separate foreground from background.
     */
    private fun analyzeColors(bitmap: Bitmap, rect: Rect): ColorAnalysis {
        val darkColors = mutableListOf<Int>()
        val lightColors = mutableListOf<Int>()
        
        // Sample pixels from the region
        val stepX = maxOf(1, rect.width() / 20)
        val stepY = maxOf(1, rect.height() / 10)
        
        for (y in rect.top until rect.bottom step stepY) {
            for (x in rect.left until rect.right step stepX) {
                if (x in 0 until bitmap.width && y in 0 until bitmap.height) {
                    val pixel = bitmap.getPixel(x, y)
                    val luminance = calculateLuminance(pixel)
                    
                    if (luminance < LUMINANCE_THRESHOLD) {
                        darkColors.add(pixel)
                    } else {
                        lightColors.add(pixel)
                    }
                }
            }
        }
        
        // Determine text and background colors
        // Usually text is darker (more common in dark list) and background is lighter
        val textColor = if (darkColors.isNotEmpty()) {
            averageColor(darkColors)
        } else {
            Color.BLACK
        }
        
        val backgroundColor = if (lightColors.isNotEmpty()) {
            averageColor(lightColors)
        } else {
            // Sample background from edges of bounding box
            sampleBackgroundFromEdges(bitmap, rect)
        }
        
        return ColorAnalysis(textColor, backgroundColor)
    }
    
    /**
     * Sample background color from the edges around the bounding box.
     * This helps get the true background color when text fills most of the box.
     */
    private fun sampleBackgroundFromEdges(bitmap: Bitmap, rect: Rect): Int {
        val edgeColors = mutableListOf<Int>()
        val padding = 5
        
        // Sample above the text box
        for (x in rect.left until rect.right step 3) {
            val y = (rect.top - padding).coerceIn(0, bitmap.height - 1)
            if (x in 0 until bitmap.width) {
                edgeColors.add(bitmap.getPixel(x, y))
            }
        }
        
        // Sample below the text box
        for (x in rect.left until rect.right step 3) {
            val y = (rect.bottom + padding).coerceIn(0, bitmap.height - 1)
            if (x in 0 until bitmap.width) {
                edgeColors.add(bitmap.getPixel(x, y))
            }
        }
        
        // Sample left of the text box
        for (y in rect.top until rect.bottom step 3) {
            val x = (rect.left - padding).coerceIn(0, bitmap.width - 1)
            if (y in 0 until bitmap.height) {
                edgeColors.add(bitmap.getPixel(x, y))
            }
        }
        
        // Sample right of the text box
        for (y in rect.top until rect.bottom step 3) {
            val x = (rect.right + padding).coerceIn(0, bitmap.width - 1)
            if (y in 0 until bitmap.height) {
                edgeColors.add(bitmap.getPixel(x, y))
            }
        }
        
        return if (edgeColors.isNotEmpty()) {
            averageColor(edgeColors)
        } else {
            Color.WHITE
        }
    }
    
    /**
     * Detects if text appears to be bold based on stroke density.
     * Bold text typically has thicker strokes, meaning more text pixels per unit area.
     */
    private fun detectBoldText(
        bitmap: Bitmap,
        rect: Rect,
        textColor: Int,
        backgroundColor: Int
    ): Boolean {
        var textPixelCount = 0
        var totalPixelCount = 0
        
        val textLuminance = calculateLuminance(textColor)
        
        // Count pixels that match the text color (within tolerance)
        val stepX = maxOf(1, rect.width() / 30)
        val stepY = maxOf(1, rect.height() / 15)
        
        for (y in rect.top until rect.bottom step stepY) {
            for (x in rect.left until rect.right step stepX) {
                if (x in 0 until bitmap.width && y in 0 until bitmap.height) {
                    val pixel = bitmap.getPixel(x, y)
                    val pixelLuminance = calculateLuminance(pixel)
                    
                    // Check if pixel is close to text color (dark for dark text)
                    if (kotlin.math.abs(pixelLuminance - textLuminance) < 0.2f) {
                        textPixelCount++
                    }
                    totalPixelCount++
                }
            }
        }
        
        // Bold text typically has 12%+ pixel density
        val density = if (totalPixelCount > 0) {
            textPixelCount.toFloat() / totalPixelCount
        } else {
            0f
        }
        
        return density > BOLD_STROKE_RATIO
    }
    
    /**
     * Calculate luminance of a color using the standard formula.
     */
    private fun calculateLuminance(color: Int): Float {
        val r = Color.red(color) / 255f
        val g = Color.green(color) / 255f
        val b = Color.blue(color) / 255f
        
        return 0.299f * r + 0.587f * g + 0.114f * b
    }
    
    /**
     * Calculate the average color from a list of colors.
     */
    private fun averageColor(colors: List<Int>): Int {
        if (colors.isEmpty()) return Color.BLACK
        
        var totalR = 0L
        var totalG = 0L
        var totalB = 0L
        
        for (color in colors) {
            totalR += Color.red(color)
            totalG += Color.green(color)
            totalB += Color.blue(color)
        }
        
        val count = colors.size
        return Color.rgb(
            (totalR / count).toInt(),
            (totalG / count).toInt(),
            (totalB / count).toInt()
        )
    }
}
