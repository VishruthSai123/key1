package com.vishruth.key1.data

/**
 * Represents different key background styles for the keyboard
 * Separate from main keyboard themes for granular control
 */
data class KeyBackgroundStyle(
    val id: String,
    val name: String,
    val description: String,
    val drawableResource: String
)

object KeyBackgroundStyles {
    val DARK = KeyBackgroundStyle(
        id = "dark",
        name = "Dark",
        description = "Dark gray keys",
        drawableResource = "modern_key_button"
    )
    
    val LIGHT_WHITE = KeyBackgroundStyle(
        id = "light_white",
        name = "Light White",
        description = "Clean white background with subtle shadows (default)",
        drawableResource = "modern_key_button_light_white"
    )
    
    val LIGHT_TRANSPARENT = KeyBackgroundStyle(
        id = "light_transparent",
        name = "Light Transparent",
        description = "Semi-transparent with subtle opacity",
        drawableResource = "modern_key_button_light_transparent"
    )
    
    fun getAllStyles(): List<KeyBackgroundStyle> = listOf(
        LIGHT_WHITE,
        DARK,
        LIGHT_TRANSPARENT
    )
    
    fun getStyleById(id: String): KeyBackgroundStyle {
        return getAllStyles().find { it.id == id } ?: LIGHT_WHITE
    }
    
    /**
     * Get drawable resource ID by style ID
     */
    fun getDrawableResourceId(context: android.content.Context, styleId: String): Int {
        val style = getStyleById(styleId)
        return context.resources.getIdentifier(
            style.drawableResource,
            "drawable",
            context.packageName
        )
    }
} 