package com.vishruth.key1.emoji

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.Log
import android.view.View
import android.widget.*
import androidx.core.content.ContextCompat
import com.vishruth.key1.R
import com.vishruth.key1.api.OpenMojiService
import kotlinx.coroutines.*
import java.io.IOException

/**
 * Manager class for handling OpenMoji emoji system
 * Completely reconstructed with custom asset icons and proper API integration
 * Based on emojie.md documentation
 */
class OpenMojiManager(
    private val context: Context,
    private val keyboardView: View,
    private val onEmojiSelected: (String) -> Unit,
    private val onBackPressed: () -> Unit
) {
    
    companion object {
        private const val TAG = "OpenMojiManager"
        private const val MIN_EMOJI_SIZE = 36  // Minimum emoji button size
        private const val EMOJI_MARGIN = 2     // Margin between emojis
    }
    
    private val openMojiService = OpenMojiService()
    private var currentCategories: List<OpenMojiService.EmojiCategory> = emptyList()
    private var currentSelectedCategory: String = ""
    private var managerScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    // Enhanced cache for better performance
    private val emojiCache = mutableMapOf<String, List<OpenMojiService.EmojiData>>()
    private var categoriesCache: List<OpenMojiService.EmojiCategory>? = null
    private val assetIconCache = mutableMapOf<String, Drawable?>()
    
    // Dynamic layout calculations
    private var dynamicColumns = 8
    private var dynamicEmojiSize = 48
    
    // UI References
    private val emojiLayout: LinearLayout by lazy { keyboardView.findViewById(R.id.open_emojis_layout) }
    private val backButton: Button by lazy { keyboardView.findViewById(R.id.emoji_back_button) }
    private val searchButton: Button by lazy { keyboardView.findViewById(R.id.emoji_search_button) }
    private val loadingIndicator: ProgressBar by lazy { keyboardView.findViewById(R.id.emoji_loading_indicator) }
    private val errorMessage: TextView by lazy { keyboardView.findViewById(R.id.emoji_error_message) }
    private val emojiGridContainer: GridLayout by lazy { keyboardView.findViewById(R.id.emoji_grid_container) }
    private val categoryButtonsContainer: LinearLayout by lazy { keyboardView.findViewById(R.id.category_buttons_container) }
    private val spaceKey: Button by lazy { keyboardView.findViewById(R.id.key_space_emoji_new) }
    private val enterKey: Button by lazy { keyboardView.findViewById(R.id.key_enter_emoji_new) }
    private val backspaceKey: Button by lazy { keyboardView.findViewById(R.id.key_backspace_emoji_new) }
    
    /**
     * Initialize the OpenMoji system with custom assets
     */
    fun initialize() {
        Log.d(TAG, "Initializing OpenMoji Manager with custom assets")
        
        calculateDynamicLayout()
        setupClickListeners()
        preloadAssetIcons()
        loadCategories()
    }
    
    /**
     * Show the emoji layout
     */
    fun show() {
        emojiLayout.visibility = View.VISIBLE
        // Recalculate layout in case screen orientation changed
        calculateDynamicLayout()
        Log.d(TAG, "OpenMoji layout shown with responsive layout")
    }
    
    /**
     * Hide the emoji layout
     */
    fun hide() {
        emojiLayout.visibility = View.GONE
        Log.d(TAG, "OpenMoji layout hidden")
    }
    
    /**
     * Check if emoji layout is visible
     */
    fun isVisible(): Boolean {
        return emojiLayout.visibility == View.VISIBLE
    }
    
    /**
     * Clean up resources
     */
    fun cleanup() {
        managerScope.cancel()
        assetIconCache.clear()
        emojiCache.clear()
        Log.d(TAG, "OpenMoji Manager cleaned up")
    }
    
    /**
     * Calculate dynamic layout based on screen width for responsive design
     */
    private fun calculateDynamicLayout() {
        val displayMetrics = context.resources.displayMetrics
        val screenWidthDp = (displayMetrics.widthPixels / displayMetrics.density).toInt()
        
        // Calculate available width (subtract padding and margins)
        val availableWidth = screenWidthDp - 32 // Account for container padding
        
        // Calculate optimal column count and emoji size
        val totalMarginPerEmoji = EMOJI_MARGIN * 2
        val minTotalWidthPerEmoji = MIN_EMOJI_SIZE + totalMarginPerEmoji
        
        // Find maximum columns that fit
        dynamicColumns = (availableWidth / minTotalWidthPerEmoji).coerceAtLeast(6).coerceAtMost(10)
        
        // Calculate actual emoji size based on available space
        val availableWidthPerEmoji = availableWidth / dynamicColumns
        dynamicEmojiSize = (availableWidthPerEmoji - totalMarginPerEmoji).coerceAtLeast(MIN_EMOJI_SIZE)
        
        Log.d(TAG, "Dynamic layout: screenWidth=${screenWidthDp}dp, columns=$dynamicColumns, emojiSize=${dynamicEmojiSize}dp")
    }
    
    /**
     * Preload custom asset icons for better performance
     */
    private fun preloadAssetIcons() {
        Log.d(TAG, "Preloading custom asset icons")
        
        val assetNames = listOf(
            "faces.png",
            "people.png",
            "animals.png",
            "food.png",
            "vehiclesandbuildings.png",
            "Activities.png",
            "objects.png",
            "symbols.png",
            "flags.png"
        )
        
        assetNames.forEach { assetName ->
            try {
                val inputStream = context.assets.open(assetName)
                val drawable = Drawable.createFromStream(inputStream, null)
                assetIconCache[assetName] = drawable
                inputStream.close()
                Log.d(TAG, "Preloaded asset icon: $assetName")
            } catch (e: IOException) {
                Log.w(TAG, "Failed to load asset icon: $assetName", e)
                assetIconCache[assetName] = null
            }
        }
        
        Log.d(TAG, "Preloaded ${assetIconCache.size} asset icons")
    }
    
    /**
     * Setup click listeners for all interactive elements
     */
    private fun setupClickListeners() {
        // Header buttons
        backButton.setOnClickListener {
            Log.d(TAG, "Back button pressed")
            onBackPressed()
        }
        
        searchButton.setOnClickListener {
            Log.d(TAG, "Search button pressed")
            // TODO: Implement search functionality
        }
        
        // Keyboard control keys
        spaceKey.setOnClickListener {
            onEmojiSelected(" ")
        }
        
        enterKey.setOnClickListener {
            onEmojiSelected("\n")
        }
        
        // Note: Backspace is handled by the main keyboard service
        // The button functionality is set up in SimpleKeyWiseInputMethodService.setupBackspaceButtons()
    }
    
    /**
     * Load emoji categories with timeout and fallback
     */
    private fun loadCategories() {
        Log.d(TAG, "Loading emoji categories")
        
        // Check cache first for faster loading
        categoriesCache?.let { cachedCategories ->
            Log.d(TAG, "Using cached categories for instant loading")
            currentCategories = cachedCategories
            setupCategoryButtons(cachedCategories)
            if (cachedCategories.isNotEmpty()) {
                selectCategory(cachedCategories.first().slug)
            }
            return
        }
        
        // Show fallback categories immediately for better UX
        useFallbackCategories()
        
        // Try to load from API in background
        managerScope.launch {
            try {
                val result = openMojiService.getCategories()
                
                if (result.isSuccess) {
                    val categories = result.getOrNull() ?: emptyList()
                    if (categories.isNotEmpty()) {
                        Log.d(TAG, "Successfully loaded ${categories.size} categories from API")
                        categoriesCache = categories
                        currentCategories = categories
                        
                        // Update UI with API data
                        setupCategoryButtons(categories)
                        if (currentSelectedCategory.isBlank()) {
                            selectCategory(categories.first().slug)
                        }
                    }
                } else {
                    Log.w(TAG, "API failed, keeping fallback categories: ${result.exceptionOrNull()?.message}")
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Exception loading categories from API, keeping fallback", e)
            }
        }
    }
    
    /**
     * Use fallback categories with custom assets
     */
    private fun useFallbackCategories() {
        Log.d(TAG, "Using fallback categories with custom assets")
        val fallbackCategories = openMojiService.getFallbackCategories()
        currentCategories = fallbackCategories
        
        setupCategoryButtons(fallbackCategories)
        
        if (fallbackCategories.isNotEmpty()) {
            selectCategory(fallbackCategories.first().slug)
        }
    }
    
    /**
     * Setup category buttons with custom asset icons
     */
    private fun setupCategoryButtons(categories: List<OpenMojiService.EmojiCategory>) {
        categoryButtonsContainer.removeAllViews()
        
        categories.forEach { category ->
            val button = Button(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    dpToPx(48),
                    dpToPx(48)
                ).apply {
                    marginEnd = dpToPx(6)
                }
                
                // Load custom asset icon
                val assetIcon = assetIconCache[category.assetIcon]
                if (assetIcon != null) {
                    val iconSize = dpToPx(24)
                    assetIcon.setBounds(0, 0, iconSize, iconSize)
                    setCompoundDrawables(null, assetIcon, null, null)
                    compoundDrawablePadding = 0
                    Log.d(TAG, "Set asset icon for ${category.slug}: ${category.assetIcon}")
                } else {
                    // Fallback to text if asset not found
                    text = category.displayName.take(1)
                    textSize = 16f
                    Log.w(TAG, "Asset icon not found for ${category.slug}, using text fallback")
                }
                
                // Set 40% border radius background
                setBackgroundResource(R.drawable.category_button_background)
                elevation = dpToPx(2).toFloat()
                setPadding(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8))
                
                setOnClickListener {
                    selectCategory(category.slug)
                }
                
                contentDescription = category.displayName
            }
            
            categoryButtonsContainer.addView(button)
        }
        
        Log.d(TAG, "Setup ${categories.size} category buttons with custom assets")
    }
    
    /**
     * Select and load emojis for a category
     */
    private fun selectCategory(categorySlug: String) {
        if (currentSelectedCategory == categorySlug) return
        
        Log.d(TAG, "Selecting category: $categorySlug")
        currentSelectedCategory = categorySlug
        
        // Update category button states
        updateCategoryButtonStates()
        
        // Load emojis for category
        loadEmojisForCategory(categorySlug)
    }
    
    /**
     * Update visual states of category buttons with 40% border radius backgrounds
     */
    private fun updateCategoryButtonStates() {
        for (i in 0 until categoryButtonsContainer.childCount) {
            val button = categoryButtonsContainer.getChildAt(i) as Button
            val category = currentCategories[i]
            
            if (category.slug == currentSelectedCategory) {
                // Highlight selected category with rounded background
                button.setBackgroundResource(R.drawable.category_button_selected)
                button.elevation = dpToPx(4).toFloat()
            } else {
                // Unselected state with subtle rounded background
                button.setBackgroundResource(R.drawable.category_button_background)
                button.elevation = dpToPx(2).toFloat()
            }
        }
    }
    
    /**
     * Load emojis for specific category with proper API integration
     */
    private fun loadEmojisForCategory(categorySlug: String) {
        Log.d(TAG, "Loading emojis for category: $categorySlug")
        
        // Check cache first
        emojiCache[categorySlug]?.let { cachedEmojis ->
            Log.d(TAG, "Using cached emojis for $categorySlug (${cachedEmojis.size} emojis)")
            displayEmojis(cachedEmojis)
            return
        }
        
        // Show fallback emojis immediately
        displayFallbackEmojis(categorySlug)
        
        // Load from API in background
        managerScope.launch {
            try {
                val result = openMojiService.getEmojisForCategory(categorySlug)
                
                if (result.isSuccess) {
                    val emojis = result.getOrNull() ?: emptyList()
                    if (emojis.isNotEmpty()) {
                        Log.d(TAG, "Loaded ${emojis.size} emojis from API for $categorySlug")
                        emojiCache[categorySlug] = emojis
                        
                        // Only update if this category is still selected
                        if (currentSelectedCategory == categorySlug) {
                            displayEmojis(emojis)
                        }
                    } else {
                        Log.w(TAG, "API returned no emojis for $categorySlug")
                    }
                } else {
                    Log.w(TAG, "Failed to load emojis for $categorySlug: ${result.exceptionOrNull()?.message}")
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Exception loading emojis for $categorySlug", e)
            }
        }
    }
    
    /**
     * Display fallback emojis for immediate user feedback
     */
    private fun displayFallbackEmojis(categorySlug: String) {
        val fallbackEmojis = when (categorySlug) {
            "smileys-emotion" -> listOf(
                OpenMojiService.EmojiData("grinning-face", "😀", "grinning face", "1F600", "smileys-emotion", "face-smiling"),
                OpenMojiService.EmojiData("beaming-face", "😁", "beaming face with smiling eyes", "1F601", "smileys-emotion", "face-smiling"),
                OpenMojiService.EmojiData("grinning-face-big-eyes", "😃", "grinning face with big eyes", "1F603", "smileys-emotion", "face-smiling"),
                OpenMojiService.EmojiData("grinning-face-smiling-eyes", "😄", "grinning face with smiling eyes", "1F604", "smileys-emotion", "face-smiling"),
                OpenMojiService.EmojiData("grinning-sweat", "😅", "grinning face with sweat", "1F605", "smileys-emotion", "face-smiling"),
                OpenMojiService.EmojiData("rolling-on-floor-laughing", "🤣", "rolling on the floor laughing", "1F923", "smileys-emotion", "face-smiling"),
                OpenMojiService.EmojiData("face-with-tears-of-joy", "😂", "face with tears of joy", "1F602", "smileys-emotion", "face-smiling"),
                OpenMojiService.EmojiData("slightly-smiling-face", "🙂", "slightly smiling face", "1F642", "smileys-emotion", "face-smiling"),
                OpenMojiService.EmojiData("winking-face", "😉", "winking face", "1F609", "smileys-emotion", "face-smiling"),
                OpenMojiService.EmojiData("smiling-face-heart-eyes", "😍", "smiling face with heart-eyes", "1F60D", "smileys-emotion", "face-smiling"),
                OpenMojiService.EmojiData("kissing-face", "😘", "face blowing a kiss", "1F618", "smileys-emotion", "face-smiling"),
                OpenMojiService.EmojiData("thinking-face", "🤔", "thinking face", "1F914", "smileys-emotion", "face-neutral-skeptical"),
                OpenMojiService.EmojiData("neutral-face", "😐", "neutral face", "1F610", "smileys-emotion", "face-neutral-skeptical"),
                OpenMojiService.EmojiData("confused-face", "😕", "confused face", "1F615", "smileys-emotion", "face-concerned"),
                OpenMojiService.EmojiData("crying-face", "😢", "crying face", "1F622", "smileys-emotion", "face-concerned"),
                OpenMojiService.EmojiData("heart", "❤️", "red heart", "2764", "smileys-emotion", "emotion")
            )
            "people-body" -> listOf(
                OpenMojiService.EmojiData("thumbs-up", "👍", "thumbs up", "1F44D", "people-body", "hand-fingers-open"),
                OpenMojiService.EmojiData("thumbs-down", "👎", "thumbs down", "1F44E", "people-body", "hand-fingers-open"),
                OpenMojiService.EmojiData("clapping-hands", "👏", "clapping hands", "1F44F", "people-body", "hands"),
                OpenMojiService.EmojiData("waving-hand", "👋", "waving hand", "1F44B", "people-body", "hand-fingers-open"),
                OpenMojiService.EmojiData("person", "🧑", "person", "1F9D1", "people-body", "person"),
                OpenMojiService.EmojiData("man", "👨", "man", "1F468", "people-body", "person"),
                OpenMojiService.EmojiData("woman", "👩", "woman", "1F469", "people-body", "person"),
                OpenMojiService.EmojiData("raising-hand", "🙋", "person raising hand", "1F64B", "people-body", "person-gesture"),
                OpenMojiService.EmojiData("folded-hands", "🙏", "folded hands", "1F64F", "people-body", "hands"),
                OpenMojiService.EmojiData("muscle", "💪", "flexed biceps", "1F4AA", "people-body", "body-parts"),
                OpenMojiService.EmojiData("victory-hand", "✌️", "victory hand", "270C", "people-body", "hand-fingers-open"),
                OpenMojiService.EmojiData("ok-hand", "👌", "OK hand", "1F44C", "people-body", "hand-fingers-partial")
            )
            "animals-nature" -> listOf(
                OpenMojiService.EmojiData("dog-face", "🐶", "dog face", "1F436", "animals-nature", "animal-mammal"),
                OpenMojiService.EmojiData("cat-face", "🐱", "cat face", "1F431", "animals-nature", "animal-mammal"),
                OpenMojiService.EmojiData("rabbit-face", "🐰", "rabbit face", "1F430", "animals-nature", "animal-mammal"),
                OpenMojiService.EmojiData("lion-face", "🦁", "lion face", "1F981", "animals-nature", "animal-mammal"),
                OpenMojiService.EmojiData("tiger-face", "🐯", "tiger face", "1F42F", "animals-nature", "animal-mammal"),
                OpenMojiService.EmojiData("bear-face", "🐻", "bear face", "1F43B", "animals-nature", "animal-mammal"),
                OpenMojiService.EmojiData("tree", "🌳", "deciduous tree", "1F333", "animals-nature", "plant-other"),
                OpenMojiService.EmojiData("flower", "🌸", "cherry blossom", "1F338", "animals-nature", "plant-flower"),
                OpenMojiService.EmojiData("sun", "☀️", "sun", "2600", "animals-nature", "sky-weather"),
                OpenMojiService.EmojiData("moon", "🌙", "crescent moon", "1F319", "animals-nature", "sky-weather")
            )
            "food-drink" -> listOf(
                OpenMojiService.EmojiData("apple", "🍎", "red apple", "1F34E", "food-drink", "food-fruit"),
                OpenMojiService.EmojiData("pizza", "🍕", "pizza", "1F355", "food-drink", "food-prepared"),
                OpenMojiService.EmojiData("hamburger", "🍔", "hamburger", "1F354", "food-drink", "food-prepared"),
                OpenMojiService.EmojiData("coffee", "☕", "hot beverage", "2615", "food-drink", "drink"),
                OpenMojiService.EmojiData("cake", "🎂", "birthday cake", "1F382", "food-drink", "food-sweet"),
                OpenMojiService.EmojiData("ice-cream", "🍦", "soft ice cream", "1F366", "food-drink", "food-sweet"),
                OpenMojiService.EmojiData("banana", "🍌", "banana", "1F34C", "food-drink", "food-fruit"),
                OpenMojiService.EmojiData("grapes", "🍇", "grapes", "1F347", "food-drink", "food-fruit"),
                OpenMojiService.EmojiData("watermelon", "🍉", "watermelon", "1F349", "food-drink", "food-fruit"),
                OpenMojiService.EmojiData("donut", "🍩", "doughnut", "1F369", "food-drink", "food-sweet")
            )
            "travel-places" -> listOf(
                OpenMojiService.EmojiData("car", "🚗", "automobile", "1F697", "travel-places", "transport-ground"),
                OpenMojiService.EmojiData("airplane", "✈️", "airplane", "2708", "travel-places", "transport-air"),
                OpenMojiService.EmojiData("train", "🚆", "train", "1F686", "travel-places", "transport-ground"),
                OpenMojiService.EmojiData("bus", "🚌", "bus", "1F68C", "travel-places", "transport-ground"),
                OpenMojiService.EmojiData("ship", "🚢", "ship", "1F6A2", "travel-places", "transport-water"),
                OpenMojiService.EmojiData("house", "🏠", "house", "1F3E0", "travel-places", "place-building"),
                OpenMojiService.EmojiData("building", "🏢", "office building", "1F3E2", "travel-places", "place-building"),
                OpenMojiService.EmojiData("globe", "🌍", "globe showing Europe-Africa", "1F30D", "travel-places", "place-map")
            )
            "activities" -> listOf(
                OpenMojiService.EmojiData("soccer-ball", "⚽", "soccer ball", "26BD", "activities", "sport"),
                OpenMojiService.EmojiData("basketball", "🏀", "basketball", "1F3C0", "activities", "sport"),
                OpenMojiService.EmojiData("tennis", "🎾", "tennis", "1F3BE", "activities", "sport"),
                OpenMojiService.EmojiData("party-popper", "🎉", "party popper", "1F389", "activities", "event"),
                OpenMojiService.EmojiData("balloon", "🎈", "balloon", "1F388", "activities", "event"),
                OpenMojiService.EmojiData("gift", "🎁", "wrapped gift", "1F381", "activities", "event"),
                OpenMojiService.EmojiData("music", "🎵", "musical note", "1F3B5", "activities", "arts-crafts"),
                OpenMojiService.EmojiData("guitar", "🎸", "guitar", "1F3B8", "activities", "musical-instrument"),
                OpenMojiService.EmojiData("game-die", "🎲", "game die", "1F3B2", "activities", "game"),
                OpenMojiService.EmojiData("trophy", "🏆", "trophy", "1F3C6", "activities", "award-medal")
            )
            "objects" -> listOf(
                OpenMojiService.EmojiData("phone", "📱", "mobile phone", "1F4F1", "objects", "phone"),
                OpenMojiService.EmojiData("computer", "💻", "laptop computer", "1F4BB", "objects", "computer"),
                OpenMojiService.EmojiData("watch", "⌚", "watch", "231A", "objects", "time"),
                OpenMojiService.EmojiData("camera", "📷", "camera", "1F4F7", "objects", "light-video"),
                OpenMojiService.EmojiData("book", "📖", "open book", "1F4D6", "objects", "book-paper"),
                OpenMojiService.EmojiData("money", "💰", "money bag", "1F4B0", "objects", "money"),
                OpenMojiService.EmojiData("key", "🔑", "key", "1F511", "objects", "lock"),
                OpenMojiService.EmojiData("scissors", "✂️", "scissors", "2702", "objects", "tool"),
                OpenMojiService.EmojiData("light-bulb", "💡", "light bulb", "1F4A1", "objects", "light-video"),
                OpenMojiService.EmojiData("battery", "🔋", "battery", "1F50B", "objects", "science")
            )
            "symbols" -> listOf(
                OpenMojiService.EmojiData("heart-red", "❤️", "red heart", "2764", "symbols", "other-symbol"),
                OpenMojiService.EmojiData("check-mark", "✅", "check mark button", "2705", "symbols", "other-symbol"),
                OpenMojiService.EmojiData("cross-mark", "❌", "cross mark", "274C", "symbols", "other-symbol"),
                OpenMojiService.EmojiData("warning", "⚠️", "warning", "26A0", "symbols", "warning"),
                OpenMojiService.EmojiData("question-mark", "❓", "question mark", "2753", "symbols", "punctuation"),
                OpenMojiService.EmojiData("exclamation-mark", "❗", "exclamation mark", "2757", "symbols", "punctuation"),
                OpenMojiService.EmojiData("star-symbol", "⭐", "star", "2B50", "symbols", "other-symbol"),
                OpenMojiService.EmojiData("music-note", "🎵", "musical note", "1F3B5", "symbols", "av-symbol")
            )
            "flags" -> listOf(
                OpenMojiService.EmojiData("flag-us", "🇺🇸", "flag: United States", "1F1FA 1F1F8", "flags", "country-flag"),
                OpenMojiService.EmojiData("flag-gb", "🇬🇧", "flag: United Kingdom", "1F1EC 1F1E7", "flags", "country-flag"),
                OpenMojiService.EmojiData("flag-in", "🇮🇳", "flag: India", "1F1EE 1F1F3", "flags", "country-flag"),
                OpenMojiService.EmojiData("flag-jp", "🇯🇵", "flag: Japan", "1F1EF 1F1F5", "flags", "country-flag"),
                OpenMojiService.EmojiData("flag-de", "🇩🇪", "flag: Germany", "1F1E9 1F1EA", "flags", "country-flag"),
                OpenMojiService.EmojiData("flag-fr", "🇫🇷", "flag: France", "1F1EB 1F1F7", "flags", "country-flag"),
                OpenMojiService.EmojiData("flag-white", "🏳️", "white flag", "1F3F3", "flags", "flag"),
                OpenMojiService.EmojiData("checkered-flag", "🏁", "chequered flag", "1F3C1", "flags", "flag")
            )
            else -> listOf(
                OpenMojiService.EmojiData("grinning-face", "😀", "grinning face", "1F600", "smileys-emotion", "face-smiling"),
                OpenMojiService.EmojiData("thumbs-up", "👍", "thumbs up", "1F44D", "people-body", "hand-fingers-open"),
                OpenMojiService.EmojiData("heart", "❤️", "red heart", "2764", "smileys-emotion", "emotion")
            )
        }
        
        Log.d(TAG, "Displaying ${fallbackEmojis.size} fallback emojis for $categorySlug")
        displayEmojis(fallbackEmojis)
    }
    
    /**
     * Display emojis in responsive grid layout with transparent backgrounds
     */
    private fun displayEmojis(emojis: List<OpenMojiService.EmojiData>) {
        hideLoading()
        
        emojiGridContainer.removeAllViews()
        emojiGridContainer.columnCount = dynamicColumns
        
        Log.d(TAG, "Displaying ${emojis.size} emojis in ${dynamicColumns}-column responsive grid")
        
        if (emojis.isEmpty()) {
            showError("No emojis found")
            return
        }
        
        emojis.forEachIndexed { index, emoji ->
            val button = Button(context).apply {
                layoutParams = GridLayout.LayoutParams().apply {
                    width = dpToPx(dynamicEmojiSize)
                    height = dpToPx(dynamicEmojiSize)
                    setMargins(dpToPx(EMOJI_MARGIN), dpToPx(EMOJI_MARGIN), dpToPx(EMOJI_MARGIN), dpToPx(EMOJI_MARGIN))
                    columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                }
                
                text = emoji.character
                textSize = (dynamicEmojiSize * 0.6f).coerceAtLeast(20f) // Responsive text size
                setTextColor(ContextCompat.getColor(context, R.color.keywise_text_primary))
                typeface = android.graphics.Typeface.DEFAULT
                
                // Completely transparent background for maximum space utilization
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                elevation = 0f
                setPadding(0, 0, 0, 0)
                
                // Remove any default styling
                stateListAnimator = null
                
                contentDescription = emoji.unicodeName
                
                setOnClickListener {
                    Log.d(TAG, "Selected emoji: ${emoji.character} (${emoji.unicodeName})")
                    onEmojiSelected(emoji.character)
                }
                
                // Log first few for debugging
                if (index < 3) {
                    Log.d(TAG, "Added responsive emoji [$index]: '${emoji.character}' size=${dynamicEmojiSize}dp")
                }
            }
            
            emojiGridContainer.addView(button)
        }
        
        Log.d(TAG, "Successfully displayed ${emojis.size} responsive transparent emoji buttons")
    }
    
    /**
     * Show loading indicator
     */
    private fun showLoading() {
        loadingIndicator.visibility = View.VISIBLE
        errorMessage.visibility = View.GONE
        emojiGridContainer.visibility = View.GONE
    }
    
    /**
     * Hide loading indicator
     */
    private fun hideLoading() {
        loadingIndicator.visibility = View.GONE
        emojiGridContainer.visibility = View.VISIBLE
        errorMessage.visibility = View.GONE
    }
    
    /**
     * Show error message
     */
    private fun showError(message: String) {
        loadingIndicator.visibility = View.GONE
        emojiGridContainer.visibility = View.GONE
        errorMessage.visibility = View.VISIBLE
        errorMessage.text = message
    }
    
    /**
     * Convert dp to pixels
     */
    private fun dpToPx(dp: Int): Int {
        return (dp * context.resources.displayMetrics.density).toInt()
    }
}
