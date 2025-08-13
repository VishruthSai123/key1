package com.vishruth.key1.data

import androidx.compose.ui.graphics.Color

data class KeyboardTheme(
    val id: String,
    val name: String,
    val startColor: String,
    val endColor: String,
    val textColor: String = "#FFFFFF"
) {
    fun getStartColor(): Color = Color(android.graphics.Color.parseColor(startColor))
    fun getEndColor(): Color = Color(android.graphics.Color.parseColor(endColor))
    fun getTextColor(): Color = Color(android.graphics.Color.parseColor(textColor))
}

object KeyboardThemes {
    val WHITE = KeyboardTheme(
        id = "white",
        name = "Clean White",
        startColor = "#FFFFFF",
        endColor = "#F8F9FA",
        textColor = "#333333"
    )
    
    val DEFAULT = KeyboardTheme(
        id = "default",
        name = "Default Dark",
        startColor = "#1C1C1E",
        endColor = "#0F0F10"
    )
    
    val OCEAN_BLUE = KeyboardTheme(
        id = "ocean_blue",
        name = "Ocean Blue",
        startColor = "#667EEA",
        endColor = "#764BA2"
    )
    
    val SUNSET_ORANGE = KeyboardTheme(
        id = "sunset_orange",
        name = "Sunset Orange",
        startColor = "#FF6B35",
        endColor = "#F7931E"
    )
    
    val FOREST_GREEN = KeyboardTheme(
        id = "forest_green",
        name = "Forest Green",
        startColor = "#56CC9D",
        endColor = "#6EDBFF"
    )
    
    val ROYAL_PURPLE = KeyboardTheme(
        id = "royal_purple",
        name = "Royal Purple",
        startColor = "#667EEA",
        endColor = "#764BA2"
    )
    
    val FIRE_RED = KeyboardTheme(
        id = "fire_red",
        name = "Fire Red",
        startColor = "#FF416C",
        endColor = "#FF4B2B"
    )
    
    val GOLD_AMBER = KeyboardTheme(
        id = "gold_amber",
        name = "Gold Amber",
        startColor = "#FFCC70",
        endColor = "#C850C0"
    )
    
    val CYBER_PINK = KeyboardTheme(
        id = "cyber_pink",
        name = "Cyber Pink",
        startColor = "#FF0844",
        endColor = "#FFB199"
    )
    
    val ARCTIC_BLUE = KeyboardTheme(
        id = "arctic_blue",
        name = "Arctic Blue",
        startColor = "#00C9FF",
        endColor = "#92FE9D"
    )
    
    val COSMIC_PURPLE = KeyboardTheme(
        id = "cosmic_purple",
        name = "Cosmic Purple",
        startColor = "#9D50BB",
        endColor = "#6E48AA"
    )
    
    val MINT_GREEN = KeyboardTheme(
        id = "mint_green",
        name = "Mint Green",
        startColor = "#11998E",
        endColor = "#38EF7D"
    )
    
    fun getAllThemes(): List<KeyboardTheme> = listOf(
        WHITE,
        DEFAULT,
        OCEAN_BLUE,
        SUNSET_ORANGE,
        FOREST_GREEN,
        ROYAL_PURPLE,
        FIRE_RED,
        GOLD_AMBER,
        CYBER_PINK,
        ARCTIC_BLUE,
        COSMIC_PURPLE,
        MINT_GREEN
    )
    
    fun getThemeById(id: String): KeyboardTheme {
        return getAllThemes().find { it.id == id } ?: WHITE
    }
} 