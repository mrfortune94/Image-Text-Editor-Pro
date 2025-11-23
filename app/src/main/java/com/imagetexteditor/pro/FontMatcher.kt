package com.imagetexteditor.pro

import android.graphics.Typeface

/**
 * Utility class to match detected text properties to available system fonts.
 * When font auto-detection is not available, provides sensible defaults.
 */
object FontMatcher {
    
    data class FontStyle(
        val typeface: Typeface,
        val size: Float,
        val isBold: Boolean = false,
        val isItalic: Boolean = false
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
     * Uses a heuristic: font size is approximately 70-80% of bounding box height.
     * 
     * @param boundingBoxHeight The height of the text bounding box
     * @return Estimated font size
     */
    fun estimateFontSize(boundingBoxHeight: Int): Float {
        // Font size is roughly 75% of bounding box height
        // This is a heuristic that works reasonably well
        val estimatedSize = boundingBoxHeight * 0.75f
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
}
