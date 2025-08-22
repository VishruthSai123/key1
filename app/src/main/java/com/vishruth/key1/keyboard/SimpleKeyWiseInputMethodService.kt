package com.vishruth.key1.keyboard

import android.inputmethodservice.InputMethodService
import android.view.KeyEvent
import android.view.View
import android.widget.Button
import android.util.Log
import com.vishruth.key1.R
import com.vishruth.key1.data.AIAction
import com.vishruth.key1.repository.AIRepository
import kotlinx.coroutines.*
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.Toast
import android.content.ClipboardManager
import android.content.Context
import android.content.ClipData
import android.content.Intent
import android.os.Vibrator
import android.os.VibrationEffect
import android.os.Build
import android.media.AudioManager
import kotlin.math.abs
import com.vishruth.key1.data.KeyboardTheme
import com.vishruth.key1.data.KeyboardThemes
import com.vishruth.key1.data.KeyBackgroundStyle
import com.vishruth.key1.data.KeyBackgroundStyles
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import com.vishruth.key1.MainActivity

/**
 * Simple XML-based InputMethodService for KeyWise AI Keyboard
 * This version uses traditional XML layouts for better stability
 */
class SimpleKeyWiseInputMethodService : InputMethodService() {
    
    private lateinit var repository: AIRepository
    private val keyboardScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    // Keyboard state management
    private var currentMode = KeyboardMode.LETTERS
    private var isShiftEnabled = false
    private var isShiftLocked = false
    private var isAISectionVisible = true // AI section visibility state
    private var isAIProcessing = false // AI processing state
    private var currentEmojiCategory = EmojiCategory.SMILEYS // Current emoji category
    private var isSelectionBarVisible = false // Selection operations bar visibility
    
    // Undo/Redo state management
    private var textHistory = mutableListOf<String>()
    private var currentHistoryIndex = -1
    private val maxHistorySize = 50
    
    // Long press handling
    private val longPressHandler = Handler(Looper.getMainLooper())
    private var longPressRunnable: Runnable? = null
    private var isLongPressing = false
    private val LONG_PRESS_DELAY = 500L
    private val REPEAT_DELAY = 100L
    
    // Keyboard layouts
    private lateinit var lettersLayout: LinearLayout
    private lateinit var numbersLayout: LinearLayout 
    private lateinit var symbolsLayout: LinearLayout
    private lateinit var emojisLayout: LinearLayout
    private lateinit var aiActionsContainer: LinearLayout
    // private lateinit var selectionOperationsBar: LinearLayout // UI element doesn't exist

    // Auto-suggestions state
    private var isAutoSuggestionsVisible = false
    private var currentSuggestions = listOf<String>()
    private var currentWord = ""
    private val suggestionEngine = WordSuggestionEngine()
    
    // Auto-hide timer for suggestions
    private val suggestionsHideHandler = Handler(Looper.getMainLooper())
    private var suggestionsHideRunnable: Runnable? = null
    private val SUGGESTIONS_HIDE_DELAY = 2000L // 2 seconds
    
    // Personal dictionary for learned words
    private val personalDictionary = mutableSetOf<String>()
    private val PERSONAL_DICT_KEY = "personal_dictionary"

    // Store the keyboard view
    private var keyboardView: View? = null
    
    // Haptic feedback and sound effects
    private var vibrator: Vibrator? = null
    private var audioManager: AudioManager? = null
    private var isHapticEnabled = true
    private var isSoundEnabled = true
    
    // Theme support
    private var currentTheme: KeyboardTheme = KeyboardThemes.WHITE
    private var currentKeyBackgroundStyle: KeyBackgroundStyle = KeyBackgroundStyles.DARK
    private lateinit var sharedPreferences: SharedPreferences

    enum class KeyboardMode {
        LETTERS, NUMBERS, SYMBOLS, EMOJIS
    }

    enum class EmojiCategory {
        SMILEYS, PEOPLE, NATURE, OBJECTS, SYMBOLS
    }

    override fun onCreate() {
        super.onCreate()
        Log.d("SimpleKeyWise", "InputMethodService created")
        repository = AIRepository(this)
        
        // Initialize haptic feedback and sound
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        audioManager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        
        // Initialize theme support
        sharedPreferences = getSharedPreferences("sendright_settings", Context.MODE_PRIVATE)
        loadCurrentTheme()
        loadCurrentKeyBackgroundStyle()
        
        // Load personal dictionary
        loadPersonalDictionary()
        
        Log.d("SimpleKeyWise", "Haptic and sound services initialized")
    }
    
    // Haptic feedback and sound methods
    private fun performKeyClickFeedback() {
        // Haptic feedback
        if (isHapticEnabled) {
            vibrator?.let { vib ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vib.vibrate(VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vib.vibrate(30)
                }
            }
        }
        
        // Sound feedback
        if (isSoundEnabled) {
            audioManager?.let { am ->
                am.playSoundEffect(AudioManager.FX_KEYPRESS_STANDARD, 0.5f)
            }
        }
    }
    
    private fun performSpecialKeyFeedback() {
        // Slightly different feedback for special keys (longer vibration)
        if (isHapticEnabled) {
            vibrator?.let { vib ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vib.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vib.vibrate(50)
                }
            }
        }
        
        // Sound feedback
        if (isSoundEnabled) {
            audioManager?.let { am ->
                am.playSoundEffect(AudioManager.FX_KEYPRESS_STANDARD, 0.7f)
            }
        }
    }
    
    private fun performSpaceFeedback() {
        // Different feedback for space key
        if (isHapticEnabled) {
            vibrator?.let { vib ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vib.vibrate(VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vib.vibrate(40)
                }
            }
        }
        
        if (isSoundEnabled) {
            audioManager?.let { am ->
                am.playSoundEffect(AudioManager.FX_KEYPRESS_SPACEBAR, 0.6f)
            }
        }
    }
    
    private fun performBackspaceFeedback() {
        // Different feedback for backspace
        if (isHapticEnabled) {
            vibrator?.let { vib ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vib.vibrate(VibrationEffect.createOneShot(35, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vib.vibrate(35)
                }
            }
        }
        
        if (isSoundEnabled) {
            audioManager?.let { am ->
                am.playSoundEffect(AudioManager.FX_KEYPRESS_DELETE, 0.6f)
            }
        }
    }
    
    private fun performReturnFeedback() {
        // Different feedback for enter/return key
        if (isHapticEnabled) {
            vibrator?.let { vib ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vib.vibrate(VibrationEffect.createOneShot(45, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vib.vibrate(45)
                }
            }
        }
        
        if (isSoundEnabled) {
            audioManager?.let { am ->
                am.playSoundEffect(AudioManager.FX_KEYPRESS_RETURN, 0.7f)
            }
        }
    }
    
    override fun onCreateInputView(): View {
        Log.d("SimpleKeyWise", "Creating input view")
        try {
        keyboardView = layoutInflater.inflate(R.layout.keyboard_layout, null)
            Log.d("SimpleKeyWise", "Layout inflated successfully")
        
            // Initialize layout references with error handling
            try {
        lettersLayout = keyboardView!!.findViewById(R.id.letters_layout)
        numbersLayout = keyboardView!!.findViewById(R.id.numbers_layout)
        symbolsLayout = keyboardView!!.findViewById(R.id.symbols_layout)
        emojisLayout = keyboardView!!.findViewById(R.id.emojis_layout)
        aiActionsContainer = keyboardView!!.findViewById(R.id.ai_actions_container)
                Log.d("SimpleKeyWise", "Layout references initialized")
            } catch (e: Exception) {
                Log.e("SimpleKeyWise", "Error initializing layout references: ${e.message}")
                return keyboardView!!
            }
            
            // Setup components with error handling
            try {
        setupAIButtons(keyboardView!!)
                Log.d("SimpleKeyWise", "AI buttons setup complete")
            } catch (e: Exception) {
                Log.e("SimpleKeyWise", "Error setting up AI buttons: ${e.message}")
            }
            
            try {
        setupAIToggle(keyboardView!!)
                Log.d("SimpleKeyWise", "AI toggle setup complete")
            } catch (e: Exception) {
                Log.e("SimpleKeyWise", "Error setting up AI toggle: ${e.message}")
            }
            
            try {
        setupKeyButtons(keyboardView!!)
                Log.d("SimpleKeyWise", "Key buttons setup complete")
            } catch (e: Exception) {
                Log.e("SimpleKeyWise", "Error setting up key buttons: ${e.message}")
            }
            
            try {
        setupModeButtons(keyboardView!!)
                Log.d("SimpleKeyWise", "Mode buttons setup complete")
            } catch (e: Exception) {
                Log.e("SimpleKeyWise", "Error setting up mode buttons: ${e.message}")
            }
            
            try {
        setupBackspaceButtons(keyboardView!!)
                Log.d("SimpleKeyWise", "Backspace buttons setup complete")
            } catch (e: Exception) {
                Log.e("SimpleKeyWise", "Error setting up backspace buttons: ${e.message}")
            }
            
            try {
                setupEmojiButtons(keyboardView!!)
                Log.d("SimpleKeyWise", "Emoji buttons setup complete")
            } catch (e: Exception) {
                Log.e("SimpleKeyWise", "Error setting up emoji buttons: ${e.message}")
            }
            
            try {
                setupUndoRedo(keyboardView!!)
                Log.d("SimpleKeyWise", "Undo/Redo setup complete")
            } catch (e: Exception) {
                Log.e("SimpleKeyWise", "Error setting up undo/redo: ${e.message}")
            }
            
            try {
                setupAutoSuggestions(keyboardView!!)
                Log.d("SimpleKeyWise", "Auto-suggestions setup complete")
            } catch (e: Exception) {
                Log.e("SimpleKeyWise", "Error setting up auto-suggestions: ${e.message}")
            }
        
        // Start with letters layout
            try {
        switchToMode(KeyboardMode.LETTERS)
                Log.d("SimpleKeyWise", "Initial mode set to LETTERS")
            } catch (e: Exception) {
                Log.e("SimpleKeyWise", "Error switching to letters mode: ${e.message}")
            }
            
            // Apply the selected theme
            try {
                applyThemeToKeyboard()
                applyKeyBackgroundStyles()
                applySuggestionThemeColors()
                Log.d("SimpleKeyWise", "Theme, key backgrounds, and suggestion colors applied successfully")
            } catch (e: Exception) {
                Log.e("SimpleKeyWise", "Error applying theme: ${e.message}")
            }
            
            // Also post a delayed theme application to ensure it takes effect
            try {
                Handler(Looper.getMainLooper()).postDelayed({
                    applyThemeToKeyboard()
                    applyKeyBackgroundStyles()
                    applySuggestionThemeColors()
                    Log.d("SimpleKeyWise", "Delayed theme, key background, and suggestion color application completed")
                }, 100)
            } catch (e: Exception) {
                Log.e("SimpleKeyWise", "Error with delayed theme application: ${e.message}")
            }
            
            Log.d("SimpleKeyWise", "Input view creation completed successfully")
            return keyboardView!!
            
        } catch (e: Exception) {
            Log.e("SimpleKeyWise", "Critical error in onCreateInputView: ${e.message}", e)
            // Return a minimal view to prevent complete crash
            return TextView(this).apply {
                text = "Keyboard Error"
                setTextColor(android.graphics.Color.WHITE)
                gravity = android.view.Gravity.CENTER
                setPadding(20, 20, 20, 20)
            }
        }
    }
    
    private fun setupAIButtons(view: View) {
        try {
            view.findViewById<Button>(R.id.btn_rewrite)?.setOnClickListener {
                handleAIAction(AIAction.REWRITE)
            }
            
            view.findViewById<Button>(R.id.btn_summarize)?.setOnClickListener {
                handleAIAction(AIAction.SUMMARIZE)
            }
            
            view.findViewById<Button>(R.id.btn_explain)?.setOnClickListener {
                handleAIAction(AIAction.EXPLAIN)
            }
            
            view.findViewById<Button>(R.id.btn_listify)?.setOnClickListener {
                handleAIAction(AIAction.LISTIFY)
            }
            
            view.findViewById<Button>(R.id.btn_emojify)?.setOnClickListener {
                handleAIAction(AIAction.EMOJIFY)
            }
            
            view.findViewById<Button>(R.id.btn_make_formal)?.setOnClickListener {
                handleAIAction(AIAction.MAKE_FORMAL)
            }
            
            view.findViewById<Button>(R.id.btn_tweetify)?.setOnClickListener {
                handleAIAction(AIAction.TWEETIFY)
            }
            
            view.findViewById<Button>(R.id.btn_promptify)?.setOnClickListener {
                handleAIAction(AIAction.PROMPTIFY)
            }
            
            view.findViewById<Button>(R.id.btn_translate)?.setOnClickListener {
                handleAIAction(AIAction.TRANSLATE)
            }
            
            view.findViewById<Button>(R.id.btn_creative)?.setOnClickListener {
                handleAIAction(AIAction.CREATIVE_WRITE)
            }
            
            // Chat button - opens chat tab in main app
            view.findViewById<Button>(R.id.btn_answer)?.setOnClickListener {
                openChatTab()
            }
            
            view.findViewById<Button>(R.id.btn_letter)?.setOnClickListener {
                handleAIAction(AIAction.LETTER)
            }
            
        } catch (e: Exception) {
            Log.e("SimpleKeyWise", "Error setting up AI buttons: ${e.message}")
        }
    }
    
    private fun setupAIToggle(view: View) {
        val toggleButton = view.findViewById<ImageButton>(R.id.btn_ai_toggle)
        val statusText = view.findViewById<TextView>(R.id.txt_ai_status)
        
        toggleButton?.setOnClickListener {
            toggleAISection()
        }
        
        // Set initial state
        updateAIToggleState(toggleButton, statusText)
    }
    
    private fun toggleAISection() {
        isAISectionVisible = !isAISectionVisible
        
        // Cancel any ongoing animations to prevent flickering
        aiActionsContainer.animate().cancel()
        
        // Animate the visibility change with improved logic
        if (isAISectionVisible) {
            // Show AI section
            aiActionsContainer.visibility = View.VISIBLE
            aiActionsContainer.alpha = 0f
            aiActionsContainer.animate()
                .alpha(1f)
                .setDuration(150)
                .setInterpolator(android.view.animation.DecelerateInterpolator())
                .start()
        } else {
            // Hide AI section
            aiActionsContainer.animate()
                .alpha(0f)
                .setDuration(150)
                .setInterpolator(android.view.animation.AccelerateInterpolator())
                .withEndAction {
                    if (!isAISectionVisible) { // Double check state
                        aiActionsContainer.visibility = View.GONE
                    }
                }
                .start()
        }
        
        // Update toggle button state
        val toggleButton = keyboardView?.findViewById<ImageButton>(R.id.btn_ai_toggle)
        val statusText = keyboardView?.findViewById<TextView>(R.id.txt_ai_status)
        updateAIToggleState(toggleButton, statusText)
        
        Log.d("SimpleKeyWise", "AI section toggled: $isAISectionVisible")
    }
    
    private fun updateAIToggleState(toggleButton: ImageButton?, statusText: TextView?) {
        toggleButton?.let { button ->
            if (isAISectionVisible) {
                button.setImageResource(R.drawable.ic_ai_robot)
            } else {
                button.setImageResource(R.drawable.ic_expand_up)
            }
        }
        
        statusText?.let { text ->
            if (isAISectionVisible) {
                text.text = "AI Actions"
            } else {
                text.text = "AI Actions (Hidden)"
            }
        }
    }
    
    private fun setupKeyButtons(view: View) {
        // Letter keys (these should exist based on the XML)
        setupLetterKey(view, R.id.key_q, "q")
        setupLetterKey(view, R.id.key_w, "w")
        setupLetterKey(view, R.id.key_e, "e")
        setupLetterKey(view, R.id.key_r, "r")
        setupLetterKey(view, R.id.key_t, "t")
        setupLetterKey(view, R.id.key_y, "y")
        setupLetterKey(view, R.id.key_u, "u")
        setupLetterKey(view, R.id.key_i, "i")
        setupLetterKey(view, R.id.key_o, "o")
        setupLetterKey(view, R.id.key_p, "p")
        
        setupLetterKey(view, R.id.key_a, "a")
        setupLetterKey(view, R.id.key_s, "s")
        setupLetterKey(view, R.id.key_d, "d")
        setupLetterKey(view, R.id.key_f, "f")
        setupLetterKey(view, R.id.key_g, "g")
        setupLetterKey(view, R.id.key_h, "h")
        setupLetterKey(view, R.id.key_j, "j")
        setupLetterKey(view, R.id.key_k, "k")
        setupLetterKey(view, R.id.key_l, "l")
        
        setupLetterKey(view, R.id.key_z, "z")
        setupLetterKey(view, R.id.key_x, "x")
        setupLetterKey(view, R.id.key_c, "c")
        setupLetterKey(view, R.id.key_v, "v")
        setupLetterKey(view, R.id.key_b, "b")
        setupLetterKey(view, R.id.key_n, "n")
        setupLetterKey(view, R.id.key_m, "m")
        
        // Number keys (these should exist)
        setupSimpleKey(view, R.id.key_1, "1")
        setupSimpleKey(view, R.id.key_2, "2")
        setupSimpleKey(view, R.id.key_3, "3")
        setupSimpleKey(view, R.id.key_4, "4")
        setupSimpleKey(view, R.id.key_5, "5")
        setupSimpleKey(view, R.id.key_6, "6")
        setupSimpleKey(view, R.id.key_7, "7")
        setupSimpleKey(view, R.id.key_8, "8")
        setupSimpleKey(view, R.id.key_9, "9")
        setupSimpleKey(view, R.id.key_0, "0")
        
        // Basic punctuation (only the ones that exist)
        setupSimpleKey(view, R.id.key_comma, ",")
        setupSimpleKey(view, R.id.key_period, ".")
        setupSimpleKey(view, R.id.key_period_num, ".") // Period in numbers layout
        setupSimpleKey(view, R.id.key_minus, "-")
        setupSimpleKey(view, R.id.key_colon, ":")
        setupSimpleKey(view, R.id.key_semicolon, ";")
        setupSimpleKey(view, R.id.key_dollar, "$")
        setupSimpleKey(view, R.id.key_ampersand, "&")
        setupSimpleKey(view, R.id.key_at, "@")
        setupSimpleKey(view, R.id.key_question, "?")
        setupSimpleKey(view, R.id.key_exclamation, "!")
        setupSimpleKey(view, R.id.key_apostrophe, "'")
        setupSimpleKey(view, R.id.key_plus, "+")
        setupSimpleKey(view, R.id.key_equals, "=")
        setupSimpleKey(view, R.id.key_percent, "%")
        setupSimpleKey(view, R.id.key_hash, "#")
        setupSimpleKey(view, R.id.key_caret, "^")
        setupSimpleKey(view, R.id.key_tilde, "~")
        setupSimpleKey(view, R.id.key_pipe, "|")
        setupSimpleKey(view, R.id.key_backslash, "\\")
        setupSimpleKey(view, R.id.key_underscore, "_")
        setupSimpleKey(view, R.id.key_left_paren, "(")
        
        // Additional missing symbol keys from symbols layout
        setupSimpleKey(view, R.id.key_asterisk, "*")
        setupSimpleKey(view, R.id.key_quotes, "\"")
        setupSimpleKey(view, R.id.key_grave, "`")
        
        // Special bracket keys that input paired brackets
        setupBracketKeys(view)
        
        // Special keys
        setupSpecialKeys(view)
    }
    
    private fun setupLetterKey(view: View, keyId: Int, letter: String) {
        try {
        view.findViewById<Button>(keyId)?.setOnClickListener {
                performKeyClickFeedback()
            val text = if (isShiftEnabled || isShiftLocked) {
                letter.uppercase()
            } else {
                letter.lowercase()
            }
            inputText(text)
            
            // Reset shift after single use (unless locked)
            if (isShiftEnabled && !isShiftLocked) {
                toggleShift()
            }
            }
        } catch (e: Exception) {
            Log.e("SimpleKeyWise", "Error setting up letter key $letter (ID: $keyId): ${e.message}")
        }
    }
    
    private fun setupSimpleKey(view: View, keyId: Int, text: String) {
        try {
        view.findViewById<Button>(keyId)?.setOnClickListener {
                performKeyClickFeedback()
            inputText(text)
            }
        } catch (e: Exception) {
            Log.e("SimpleKeyWise", "Error setting up simple key '$text' (ID: $keyId): ${e.message}")
        }
    }
    
    private fun setupBracketKeys(view: View) {
        // Parentheses () - input both and position cursor between them
        view.findViewById<Button>(R.id.key_parentheses)?.setOnClickListener {
            performKeyClickFeedback()
            val inputConnection = currentInputConnection
            inputConnection?.commitText("()", 1)
            // Move cursor back one position to be between the brackets
            inputConnection?.commitText("", -1)
        }
        
        // Square brackets [] 
        view.findViewById<Button>(R.id.key_brackets)?.setOnClickListener {
            performKeyClickFeedback()
            val inputConnection = currentInputConnection
            inputConnection?.commitText("[]", 1)
            // Move cursor back one position to be between the brackets
            inputConnection?.commitText("", -1)
        }
        
        // Curly brackets {}
        view.findViewById<Button>(R.id.key_curly_brackets)?.setOnClickListener {
            performKeyClickFeedback()
            val inputConnection = currentInputConnection
            inputConnection?.commitText("{}", 1)
            // Move cursor back one position to be between the brackets
            inputConnection?.commitText("", -1)
        }
    }
    
    private fun setupSpecialKeys(view: View) {
        // Shift key with double-tap for caps lock
        view.findViewById<Button>(R.id.key_shift)?.let { shiftButton ->
            var lastClickTime = 0L
            
            shiftButton.setOnClickListener {
                performSpecialKeyFeedback()
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastClickTime < 400) {
                    // Double tap - toggle caps lock
                    isShiftLocked = !isShiftLocked
                    isShiftEnabled = isShiftLocked
                    Log.d("SimpleKeyWise", "Caps lock toggled: $isShiftLocked")
                } else {
                    // Single tap - toggle shift
                    if (!isShiftLocked) {
                        isShiftEnabled = !isShiftEnabled
                        Log.d("SimpleKeyWise", "Shift toggled: $isShiftEnabled")
                    }
                }
                lastClickTime = currentTime
                updateShiftButtonState(shiftButton)
            }
            
            // Set initial state
            updateShiftButtonState(shiftButton)
        }
        
        // Space keys
        setupSpaceKey(view, R.id.key_space)
        setupSpaceKey(view, R.id.key_space_num)
        setupSpaceKey(view, R.id.key_space_sym)
        
        // Enter keys
        setupEnterKey(view, R.id.key_enter)
        setupEnterKey(view, R.id.key_enter_num)
        setupEnterKey(view, R.id.key_enter_sym)
    }
    
    private fun setupSpaceKey(view: View, keyId: Int) {
        view.findViewById<Button>(keyId)?.setOnClickListener {
            performSpaceFeedback()
            
            // Learn the current word before adding space
            learnCurrentWord()
            
            inputText(" ")
            
            // Hide suggestions when space is pressed (word completed)
            hideAutoSuggestions()
        }
    }
    
    private fun setupEnterKey(view: View, keyId: Int) {
        view.findViewById<Button>(keyId)?.setOnClickListener {
            performReturnFeedback()
            val inputConnection = currentInputConnection
            inputConnection?.sendKeyEvent(
                android.view.KeyEvent(
                    android.view.KeyEvent.ACTION_DOWN,
                    android.view.KeyEvent.KEYCODE_ENTER
                )
            )
            inputConnection?.sendKeyEvent(
                android.view.KeyEvent(
                    android.view.KeyEvent.ACTION_UP,
                    android.view.KeyEvent.KEYCODE_ENTER
                )
            )
        }
    }

    private fun setupModeButtons(view: View) {
        // Switch to numbers
        view.findViewById<Button>(R.id.key_numbers)?.setOnClickListener {
            performSpecialKeyFeedback()
            switchToMode(KeyboardMode.NUMBERS)
        }
        
        // Switch to symbols from numbers
        view.findViewById<Button>(R.id.key_symbols)?.setOnClickListener {
            performSpecialKeyFeedback()
            switchToMode(KeyboardMode.SYMBOLS)
        }
        
        // Switch to numbers from symbols
        view.findViewById<Button>(R.id.key_123)?.setOnClickListener {
            performSpecialKeyFeedback()
            switchToMode(KeyboardMode.NUMBERS)
        }
        
        // Switch back to letters
        view.findViewById<Button>(R.id.key_abc)?.setOnClickListener {
            performSpecialKeyFeedback()
            switchToMode(KeyboardMode.LETTERS)
        }
        view.findViewById<Button>(R.id.key_abc_sym)?.setOnClickListener {
            performSpecialKeyFeedback()
            switchToMode(KeyboardMode.LETTERS)
        }
        view.findViewById<Button>(R.id.key_abc_emoji)?.setOnClickListener {
            performSpecialKeyFeedback()
            switchToMode(KeyboardMode.LETTERS)
        }
        
        // Switch to emoji mode (only from letters and symbols, not numbers)
        view.findViewById<Button>(R.id.key_emoji)?.setOnClickListener {
            performSpecialKeyFeedback()
            switchToMode(KeyboardMode.EMOJIS)
        }
        view.findViewById<Button>(R.id.key_emoji_sym)?.setOnClickListener {
            performSpecialKeyFeedback()
            switchToMode(KeyboardMode.EMOJIS)
        }
        // Note: key_emoji_num button has been removed from numbers layout
    }

    private fun setupBackspaceButtons(view: View) {
        setupBackspaceButton(view, R.id.key_backspace)
        setupBackspaceButton(view, R.id.key_backspace_num)
        setupBackspaceButton(view, R.id.key_backspace_sym)
        setupBackspaceButton(view, R.id.key_backspace_emoji)
    }
    
    private fun setupBackspaceButton(view: View, keyId: Int) {
        view.findViewById<Button>(keyId)?.let { backspaceButton ->
            backspaceButton.setOnTouchListener { _, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        // Check if there's text to delete before providing feedback
                        if (hasTextToDelete()) {
                        // Immediate single backspace
                        performBackspace()
                        
                        // Start long press timer
                        longPressRunnable = object : Runnable {
                            override fun run() {
                                if (!isLongPressing) {
                                    isLongPressing = true
                                }
                                    if (hasTextToDelete()) {
                                performBackspace()
                                longPressHandler.postDelayed(this, REPEAT_DELAY)
                                    } else {
                                        isLongPressing = false
                                    }
                            }
                        }
                        longPressHandler.postDelayed(longPressRunnable!!, LONG_PRESS_DELAY)
                        } else {
                            // No text to delete - show visual feedback without haptic
                            showToast("Nothing to delete")
                        }
                        true
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        // Stop long press
                        longPressRunnable?.let { longPressHandler.removeCallbacks(it) }
                        isLongPressing = false
                        true
                    }
                    else -> false
                }
            }
        }
    }
    
    private fun hasTextToDelete(): Boolean {
        val inputConnection = currentInputConnection ?: return false
        
        // Check if there's any text before the cursor
        val extractedText = inputConnection.getExtractedText(
            android.view.inputmethod.ExtractedTextRequest(), 0
        ) ?: return false
        
        val currentText = extractedText.text?.toString() ?: ""
        val selectionStart = extractedText.selectionStart
        
        // Return true if there's text before cursor or selected text
        return currentText.isNotEmpty() && selectionStart > 0
    }
    
    private fun performBackspace() {
        // Only perform haptic feedback if there's text to delete
        if (hasTextToDelete()) {
            performBackspaceFeedback()
        }
        val inputConnection = currentInputConnection
        inputConnection?.deleteSurroundingText(1, 0)
        
        // Update auto-suggestions after backspace
        updateAutoSuggestions()
    }
    
    private fun toggleShift() {
        if (!isShiftLocked) {
            isShiftEnabled = !isShiftEnabled
        }
        updateShiftButtonState()
    }
    
    private fun updateShiftButtonState(shiftButton: Button? = null) {
        val button = shiftButton ?: keyboardView?.findViewById<Button>(R.id.key_shift)
        button?.let {
            when {
                isShiftLocked -> {
                    // Caps lock state - distinctive orange with inner border
                    it.setBackgroundResource(R.drawable.shift_caps_lock_improved)
                    it.text = "⇧⇧" // Double arrow for caps lock
                    Log.d("SimpleKeyWise", "Shift button set to CAPS LOCK state")
                }
                isShiftEnabled -> {
                    // Shift enabled - vibrant blue with glow
                    it.setBackgroundResource(R.drawable.shift_enabled_improved)
                    it.text = "⇧"
                    Log.d("SimpleKeyWise", "Shift button set to ENABLED state")
                }
                else -> {
                    // Normal state - modern gray with subtle highlight
                    it.setBackgroundResource(R.drawable.shift_normal_improved)
                    it.text = "⇧"
                    Log.d("SimpleKeyWise", "Shift button set to NORMAL state")
                }
            }
        }
    }

    private fun switchToMode(mode: KeyboardMode) {
        currentMode = mode
        
        lettersLayout.visibility = if (mode == KeyboardMode.LETTERS) View.VISIBLE else View.GONE
        numbersLayout.visibility = if (mode == KeyboardMode.NUMBERS) View.VISIBLE else View.GONE
        symbolsLayout.visibility = if (mode == KeyboardMode.SYMBOLS) View.VISIBLE else View.GONE
        emojisLayout.visibility = if (mode == KeyboardMode.EMOJIS) View.VISIBLE else View.GONE
        
        Log.d("SimpleKeyWise", "Switched to mode: $mode")
    }

    private fun inputText(text: String) {
        val inputConnection = currentInputConnection
                inputConnection?.commitText(text, 1)
        
        // Update auto-suggestions after text input
        updateAutoSuggestions()
        
        Log.d("SimpleKeyWise", "Input text: $text")
    }

    private fun setupEmojiButtons(view: View) {
        // Emoji category buttons
        view.findViewById<Button>(R.id.emoji_cat_smileys)?.setOnClickListener {
            switchEmojiCategory(EmojiCategory.SMILEYS)
        }
        view.findViewById<Button>(R.id.emoji_cat_people)?.setOnClickListener {
            switchEmojiCategory(EmojiCategory.PEOPLE)
        }
        view.findViewById<Button>(R.id.emoji_cat_nature)?.setOnClickListener {
            switchEmojiCategory(EmojiCategory.NATURE)
        }
        view.findViewById<Button>(R.id.emoji_cat_objects)?.setOnClickListener {
            switchEmojiCategory(EmojiCategory.OBJECTS)
        }
        view.findViewById<Button>(R.id.emoji_cat_symbols)?.setOnClickListener {
            switchEmojiCategory(EmojiCategory.SYMBOLS)
        }
        
        // All emoji keys - Smileys category
        setupEmojiKey(view, R.id.emoji_grinning, "😀")
        setupEmojiKey(view, R.id.emoji_beaming, "😁")
        setupEmojiKey(view, R.id.emoji_sweat_smile, "😅")
        setupEmojiKey(view, R.id.emoji_squinting, "😆")
        setupEmojiKey(view, R.id.emoji_disappointed, "😥")
        setupEmojiKey(view, R.id.emoji_kissing, "😘")
        setupEmojiKey(view, R.id.emoji_blush, "☺️")
        setupEmojiKey(view, R.id.emoji_relaxed, "😌")
        setupEmojiKey(view, R.id.emoji_thinking, "🤔")
        setupEmojiKey(view, R.id.emoji_cool, "😎")
        setupEmojiKey(view, R.id.emoji_laughing, "🤣")
        setupEmojiKey(view, R.id.emoji_party, "🥳")
        setupEmojiKey(view, R.id.emoji_smirk, "😏")
        setupEmojiKey(view, R.id.emoji_neutral, "😐")
        setupEmojiKey(view, R.id.emoji_unamused, "😒")
        setupEmojiKey(view, R.id.emoji_tired, "😴")
        setupEmojiKey(view, R.id.emoji_cry, "😢")
        setupEmojiKey(view, R.id.emoji_angry, "😡")
        setupEmojiKey(view, R.id.emoji_scared, "😱")
        setupEmojiKey(view, R.id.emoji_surprised, "😲")
        setupEmojiKey(view, R.id.emoji_dizzy, "😵")
        setupEmojiKey(view, R.id.emoji_sick, "🤒")
        setupEmojiKey(view, R.id.emoji_mask, "😷")
        setupEmojiKey(view, R.id.emoji_crazy, "🤪")
        setupEmojiKey(view, R.id.emoji_devil, "😈")
        setupEmojiKey(view, R.id.emoji_angel, "😇")
        setupEmojiKey(view, R.id.emoji_skull, "💀")
        setupEmojiKey(view, R.id.emoji_robot, "🤖")
        setupEmojiKey(view, R.id.emoji_alien, "👽")
        setupEmojiKey(view, R.id.emoji_ghost, "👻")
        setupEmojiKey(view, R.id.emoji_poop, "💩")
        setupEmojiKey(view, R.id.emoji_money_face, "🤑")
        
        // People category emojis
        setupEmojiKey(view, R.id.emoji_wave, "👋")
        setupEmojiKey(view, R.id.emoji_raised_hand, "✋")
        setupEmojiKey(view, R.id.emoji_peace, "✌️")
        setupEmojiKey(view, R.id.emoji_point_up, "☝️")
        setupEmojiKey(view, R.id.emoji_point_right, "👉")
        setupEmojiKey(view, R.id.emoji_point_left, "👈")
        setupEmojiKey(view, R.id.emoji_point_down, "👇")
        setupEmojiKey(view, R.id.emoji_fist, "✊")
        setupEmojiKey(view, R.id.emoji_eyes, "👀")
        setupEmojiKey(view, R.id.emoji_nose, "👃")
        setupEmojiKey(view, R.id.emoji_ear, "👂")
        setupEmojiKey(view, R.id.emoji_tongue, "👅")
        setupEmojiKey(view, R.id.emoji_lips, "👄")
        setupEmojiKey(view, R.id.emoji_baby, "👶")
        setupEmojiKey(view, R.id.emoji_child, "🧒")
        setupEmojiKey(view, R.id.emoji_person, "🧑")
        setupEmojiKey(view, R.id.emoji_pray, "🙏")
        setupEmojiKey(view, R.id.emoji_handshake, "🤝")
        setupEmojiKey(view, R.id.emoji_nail_polish, "💅")
        setupEmojiKey(view, R.id.emoji_selfie, "🤳")
        setupEmojiKey(view, R.id.emoji_flexed_bicep, "💪")
        setupEmojiKey(view, R.id.emoji_leg, "🦵")
        setupEmojiKey(view, R.id.emoji_foot, "🦶")
        setupEmojiKey(view, R.id.emoji_brain, "🧠")
        
        // Nature category emojis  
        setupEmojiKey(view, R.id.emoji_sun, "☀️")
        setupEmojiKey(view, R.id.emoji_moon, "🌙")
        setupEmojiKey(view, R.id.emoji_cloud, "☁️")
        setupEmojiKey(view, R.id.emoji_rain, "🌧️")
        setupEmojiKey(view, R.id.emoji_snow, "❄️")
        setupEmojiKey(view, R.id.emoji_lightning, "⚡")
        setupEmojiKey(view, R.id.emoji_rainbow, "🌈")
        setupEmojiKey(view, R.id.emoji_earth, "🌍")
        setupEmojiKey(view, R.id.emoji_tree, "🌳")
        setupEmojiKey(view, R.id.emoji_palm_tree, "🌴")
        setupEmojiKey(view, R.id.emoji_cactus, "🌵")
        setupEmojiKey(view, R.id.emoji_flower, "🌸")
        setupEmojiKey(view, R.id.emoji_rose, "🌹")
        setupEmojiKey(view, R.id.emoji_sunflower, "🌻")
        setupEmojiKey(view, R.id.emoji_tulip, "🌷")
        setupEmojiKey(view, R.id.emoji_leaf, "🍃")
        setupEmojiKey(view, R.id.emoji_dog, "🐶")
        setupEmojiKey(view, R.id.emoji_cat, "🐱")
        setupEmojiKey(view, R.id.emoji_rabbit, "🐰")
        setupEmojiKey(view, R.id.emoji_bear, "🐻")
        
        // Nature category emojis - Additional missing ones
        setupEmojiKey(view, R.id.emoji_panda, "🐼")
        setupEmojiKey(view, R.id.emoji_fox, "🦊")
        setupEmojiKey(view, R.id.emoji_lion, "🦁")
        setupEmojiKey(view, R.id.emoji_unicorn, "🦄")
        
        // Objects category emojis
        setupEmojiKey(view, R.id.emoji_fire, "🔥")
        setupEmojiKey(view, R.id.emoji_rocket, "🚀")
        setupEmojiKey(view, R.id.emoji_star, "⭐")
        setupEmojiKey(view, R.id.emoji_heart, "❤️")
        setupEmojiKey(view, R.id.emoji_thumbs_up, "👍")
        setupEmojiKey(view, R.id.emoji_clap, "👏")
        setupEmojiKey(view, R.id.emoji_muscle, "💪")
        setupEmojiKey(view, R.id.emoji_ok_hand, "👌")
        
        // Objects category emojis - Additional missing ones
        setupEmojiKey(view, R.id.emoji_phone, "📱")
        setupEmojiKey(view, R.id.emoji_computer, "💻")
        setupEmojiKey(view, R.id.emoji_camera, "📷")
        setupEmojiKey(view, R.id.emoji_video_camera, "📹")
        setupEmojiKey(view, R.id.emoji_headphones, "🎧")
        setupEmojiKey(view, R.id.emoji_microphone, "🎤")
        setupEmojiKey(view, R.id.emoji_speaker, "🔊")
        setupEmojiKey(view, R.id.emoji_tv, "📺")
        setupEmojiKey(view, R.id.emoji_car, "🚗")
        setupEmojiKey(view, R.id.emoji_taxi, "🚕")
        setupEmojiKey(view, R.id.emoji_bus, "🚌")
        setupEmojiKey(view, R.id.emoji_train, "🚂")
        setupEmojiKey(view, R.id.emoji_airplane, "✈️")
        setupEmojiKey(view, R.id.emoji_ship, "🚢")
        setupEmojiKey(view, R.id.emoji_bicycle, "🚴")
        setupEmojiKey(view, R.id.emoji_motorcycle, "🏍️")
        setupEmojiKey(view, R.id.emoji_pizza, "🍕")
        setupEmojiKey(view, R.id.emoji_burger, "🍔")
        setupEmojiKey(view, R.id.emoji_coffee, "☕")
        setupEmojiKey(view, R.id.emoji_beer, "🍺")
        setupEmojiKey(view, R.id.emoji_cake, "🎂")
        setupEmojiKey(view, R.id.emoji_ice_cream, "🍦")
        setupEmojiKey(view, R.id.emoji_donut, "🍩")
        setupEmojiKey(view, R.id.emoji_fruit, "🍎")
        
        // Symbols category emojis - Heart colors
        setupEmojiKey(view, R.id.emoji_red_heart, "❤️")
        setupEmojiKey(view, R.id.emoji_orange_heart, "🧡")
        setupEmojiKey(view, R.id.emoji_yellow_heart, "💛")
        setupEmojiKey(view, R.id.emoji_green_heart, "💚")
        setupEmojiKey(view, R.id.emoji_blue_heart, "💙")
        setupEmojiKey(view, R.id.emoji_purple_heart, "💜")
        setupEmojiKey(view, R.id.emoji_black_heart, "🖤")
        setupEmojiKey(view, R.id.emoji_broken_heart, "💔")
        
        // Emoji special keys
        setupSpaceKey(view, R.id.key_space_emoji)
        setupEnterKey(view, R.id.key_enter_emoji)
        setupBackspaceButton(view, R.id.key_backspace_emoji)
    }
    
    private fun setupEmojiKey(view: View, keyId: Int, emoji: String) {
        view.findViewById<Button>(keyId)?.setOnClickListener {
            performKeyClickFeedback()
            inputText(emoji)
        }
    }
    
    /*
    private fun setupSelectionOperations(view: View) {
        view.findViewById<Button>(R.id.btn_select_all)?.setOnClickListener {
            selectAllText()
        }
        
        view.findViewById<Button>(R.id.btn_cut)?.setOnClickListener {
            cutText()
        }
        
        view.findViewById<Button>(R.id.btn_copy)?.setOnClickListener {
            copyText()
        }
        
        view.findViewById<Button>(R.id.btn_paste)?.setOnClickListener {
            pasteText()
        }
        
        view.findViewById<Button>(R.id.btn_clear_selection)?.setOnClickListener {
            clearSelection()
        }
    }
    */
    
    private fun switchEmojiCategory(category: EmojiCategory) {
        currentEmojiCategory = category
        
        // Hide all category contents
        keyboardView?.findViewById<LinearLayout>(R.id.emoji_smileys_content)?.visibility = View.GONE
        keyboardView?.findViewById<LinearLayout>(R.id.emoji_people_content)?.visibility = View.GONE
        keyboardView?.findViewById<LinearLayout>(R.id.emoji_nature_content)?.visibility = View.GONE
        keyboardView?.findViewById<LinearLayout>(R.id.emoji_objects_content)?.visibility = View.GONE
        keyboardView?.findViewById<LinearLayout>(R.id.emoji_symbols_content)?.visibility = View.GONE
        
        // Show selected category content
        val contentId = when (category) {
            EmojiCategory.SMILEYS -> R.id.emoji_smileys_content
            EmojiCategory.PEOPLE -> R.id.emoji_people_content
            EmojiCategory.NATURE -> R.id.emoji_nature_content
            EmojiCategory.OBJECTS -> R.id.emoji_objects_content
            EmojiCategory.SYMBOLS -> R.id.emoji_symbols_content
        }
        
        keyboardView?.findViewById<LinearLayout>(contentId)?.visibility = View.VISIBLE
        
        // Update category button backgrounds
        updateEmojiCategoryButtons()
        
        Log.d("SimpleKeyWise", "Switched to emoji category: $category")
    }
    
    private fun updateEmojiCategoryButtons() {
        keyboardView?.let { view ->
            val buttons = listOf(
                view.findViewById<Button>(R.id.emoji_cat_smileys) to EmojiCategory.SMILEYS,
                view.findViewById<Button>(R.id.emoji_cat_people) to EmojiCategory.PEOPLE,
                view.findViewById<Button>(R.id.emoji_cat_nature) to EmojiCategory.NATURE,
                view.findViewById<Button>(R.id.emoji_cat_objects) to EmojiCategory.OBJECTS,
                view.findViewById<Button>(R.id.emoji_cat_symbols) to EmojiCategory.SYMBOLS
            )
            
            buttons.forEach { (button, category) ->
                button?.setBackgroundResource(
                    if (category == currentEmojiCategory) R.drawable.modern_ai_button 
                    else R.drawable.modern_key_button
                )
            }
        }
    }
    
    private fun setupUndoRedo(view: View) {
        view.findViewById<ImageButton>(R.id.btn_undo)?.setOnClickListener {
            performUndo()
        }
        
        // view.findViewById<ImageButton>(R.id.btn_redo)?.setOnClickListener {
        //     performRedo()
        // }
        
        // Initialize with current text
        saveCurrentTextToHistory()
    }
    
    private fun saveCurrentTextToHistory() {
        val inputConnection = currentInputConnection ?: return
        
        val extractedText = inputConnection.getExtractedText(
            android.view.inputmethod.ExtractedTextRequest(), 0
        )
        
        val currentText = extractedText?.text?.toString() ?: ""
        
        // Don't save if it's the same as the last entry
        if (textHistory.isNotEmpty() && textHistory.last() == currentText) {
            return
        }
        
        // Add to history
        textHistory.add(currentText)
        currentHistoryIndex = textHistory.size - 1
        
        // Limit history size
        if (textHistory.size > maxHistorySize) {
            textHistory.removeAt(0)
            currentHistoryIndex = textHistory.size - 1
        }
        
        Log.d("SimpleKeyWise", "Saved text to history: index=$currentHistoryIndex, size=${textHistory.size}")
    }
    
    private fun performUndo() {
        if (textHistory.isEmpty() || currentHistoryIndex <= 0) {
            showToast("Nothing to undo")
            return
        }
        
        currentHistoryIndex--
        val textToRestore = textHistory[currentHistoryIndex]
        
        replaceAllText(textToRestore)
        showToast("Undo: Restored previous text")
        
        Log.d("SimpleKeyWise", "Undo performed: index=$currentHistoryIndex")
    }
    
    private fun performRedo() {
        if (textHistory.isEmpty() || currentHistoryIndex >= textHistory.size - 1) {
            showToast("Nothing to redo")
            return
        }
        
        currentHistoryIndex++
        val textToRestore = textHistory[currentHistoryIndex]
        
        replaceAllText(textToRestore)
        showToast("Redo: Restored next text")
        
        Log.d("SimpleKeyWise", "Redo performed: index=$currentHistoryIndex")
    }
    
    private fun replaceAllText(newText: String) {
        val inputConnection = currentInputConnection ?: return
        
        // Get current text length
        val extractedText = inputConnection.getExtractedText(
            android.view.inputmethod.ExtractedTextRequest(), 0
        )
        val currentText = extractedText?.text?.toString() ?: ""
        
        // Select all and replace
        inputConnection.setSelection(0, currentText.length)
        inputConnection.commitText(newText, 1)
    }

    // Text selection operations
    private fun selectAllText() {
        val inputConnection = currentInputConnection ?: return
        inputConnection.performContextMenuAction(android.R.id.selectAll)
        showSelectionBar()
        Log.d("SimpleKeyWise", "Select all text")
    }
    
    private fun cutText() {
        val inputConnection = currentInputConnection ?: return
        inputConnection.performContextMenuAction(android.R.id.cut)
        hideSelectionBar()
        showToast("Text cut to clipboard")
        Log.d("SimpleKeyWise", "Cut text")
    }
    
    private fun copyText() {
        val inputConnection = currentInputConnection ?: return
        inputConnection.performContextMenuAction(android.R.id.copy)
        showToast("Text copied to clipboard")
        Log.d("SimpleKeyWise", "Copy text")
    }
    
    private fun pasteText() {
        val inputConnection = currentInputConnection ?: return
        inputConnection.performContextMenuAction(android.R.id.paste)
        hideSelectionBar()
        showToast("Text pasted")
        Log.d("SimpleKeyWise", "Paste text")
    }
    
    private fun clearSelection() {
        val inputConnection = currentInputConnection ?: return
        inputConnection.deleteSurroundingText(0, 0)
        hideSelectionBar()
        Log.d("SimpleKeyWise", "Clear selection")
    }
    
    private fun showSelectionBar() {
        isSelectionBarVisible = true
        // selectionOperationsBar.visibility = View.VISIBLE
        // selectionOperationsBar.alpha = 0f
        // selectionOperationsBar.animate()
        //     .alpha(1f)
        //     .setDuration(200)
        //     .start()
    }
    
    private fun hideSelectionBar() {
        isSelectionBarVisible = false
        // selectionOperationsBar.animate()
        //     .alpha(0f)
        //     .setDuration(200)
        //     .withEndAction {
        //         selectionOperationsBar.visibility = View.GONE
        //     }
        //     .start()
    }

    private fun handleAIAction(action: AIAction) {
        Log.d("SimpleKeyWise", "AI Action triggered: ${action.displayName}")
        
        // Prevent multiple simultaneous AI calls
        if (isAIProcessing) {
            showToast("AI is already processing, please wait...")
            return
        }
        
        // Save current text state before AI action (for undo functionality)
        saveCurrentTextToHistory()
        
        val inputConnection = currentInputConnection ?: return
        
        // Show loading state
        showAILoading(action.displayName)
        
        // Get current cursor position
        val extractedText = inputConnection.getExtractedText(
            android.view.inputmethod.ExtractedTextRequest(), 0
        )
        
        val currentText = extractedText?.text?.toString() ?: ""
        val selectionStart = extractedText?.selectionStart ?: 0
        val selectionEnd = extractedText?.selectionEnd ?: 0
        
        // For Answer action, handle differently - can work with questions or generate responses
        val (textToProcess, shouldReplace) = if (action == AIAction.ANSWER) {
            getContextualTextForAnswer(currentText, selectionStart, selectionEnd)
        } else {
            getContextualText(currentText, selectionStart, selectionEnd)
        }
        
        Log.d("SimpleKeyWise", "Text to process: '$textToProcess'")
        Log.d("SimpleKeyWise", "Should replace: $shouldReplace")
        
        if (textToProcess.isNotBlank() || action == AIAction.ANSWER) {
            keyboardScope.launch {
                try {
                    Log.d("SimpleKeyWise", "Starting AI action...")
                    val result = repository.executeAIAction(action, textToProcess, true)
                    result.onSuccess { processedText ->
                        Log.d("SimpleKeyWise", "AI action successful: $processedText")
                        showAIResult(processedText, shouldReplace, textToProcess)
                        hideAILoading()
                    }.onFailure { error ->
                        Log.e("SimpleKeyWise", "AI action failed: ${error.message}")
                        showToast("AI Error: ${error.message}")
                        hideAILoading()
                    }
                } catch (e: Exception) {
                    Log.e("SimpleKeyWise", "AI action exception: ${e.message}")
                    showToast("AI Error: ${e.message}")
                    hideAILoading()
                }
            }
        } else {
            hideAILoading()
            showToast("No text to process. Type something first!")
        }
    }
    
    private fun getContextualTextForAnswer(fullText: String, selectionStart: Int, selectionEnd: Int): Pair<String, Boolean> {
        return if (selectionStart != selectionEnd) {
            // User has selected text - treat as a question
            val selectedText = fullText.substring(selectionStart, selectionEnd)
            Pair(selectedText, false) // Don't replace, append answer
        } else if (fullText.isBlank()) {
            // No text at all - prompt for a question
            Pair("How can I help you today?", false)
        } else {
            // Get the current sentence or last few words as context
            val contextRadius = 200
            val start = maxOf(0, selectionStart - contextRadius)
            val end = minOf(fullText.length, selectionStart + contextRadius)
            val contextText = fullText.substring(start, end)
            
            // If it looks like a question, answer it; otherwise provide help
            if (contextText.contains("?") || contextText.lowercase().startsWith("what") || 
                contextText.lowercase().startsWith("how") || contextText.lowercase().startsWith("why") ||
                contextText.lowercase().startsWith("when") || contextText.lowercase().startsWith("where")) {
                Pair(contextText.trim(), false) // Answer the question
            } else {
                Pair("Please explain: $contextText", false) // Ask for explanation
            }
        }
    }
    
    private fun getContextualText(fullText: String, selectionStart: Int, selectionEnd: Int): Pair<String, Boolean> {
        return if (selectionStart != selectionEnd) {
            // User has selected text
            val selectedText = fullText.substring(selectionStart, selectionEnd)
            Pair(selectedText, true)
        } else {
            // No selection, get surrounding context
            val contextRadius = 500
            val start = maxOf(0, selectionStart - contextRadius)
            val end = minOf(fullText.length, selectionStart + contextRadius)
            val contextText = fullText.substring(start, end)
            
            // Find sentence boundaries
            val sentences = contextText.split(Regex("[.!?\\n]"))
            if (sentences.isNotEmpty()) {
                val middleSentence = sentences[sentences.size / 2].trim()
                if (middleSentence.isNotBlank()) {
                    Pair(middleSentence, true)
                } else {
                    Pair(contextText.trim(), false)
                }
            } else {
                Pair(contextText.trim(), false)
            }
        }
    }
    
    private fun showAIResult(result: String, shouldReplace: Boolean, originalText: String) {
        val inputConnection = currentInputConnection ?: return
        
        if (shouldReplace) {
            // Replace the original text with AI result
            val extractedText = inputConnection.getExtractedText(
                android.view.inputmethod.ExtractedTextRequest(), 0
            )
            val currentText = extractedText?.text?.toString() ?: ""
            val selectionStart = extractedText?.selectionStart ?: 0
            val selectionEnd = extractedText?.selectionEnd ?: 0
            
            if (selectionStart != selectionEnd) {
                // Replace selected text
                inputConnection.setSelection(selectionStart, selectionEnd)
                inputConnection.commitText(result, 1)
                } else {
                    // Find and replace the original text
                    val originalIndex = currentText.indexOf(originalText)
                    if (originalIndex != -1) {
                        inputConnection.setSelection(originalIndex, originalIndex + originalText.length)
                        inputConnection.commitText(result, 1)
                    } else {
                        // Fallback: just insert the result
                        inputConnection.commitText(result, 1)
                }
            }
        } else {
            // For Answer action, add the response nicely formatted
            val formattedResult = if (result.startsWith("Answer:") || result.startsWith("Response:")) {
                "\n\n$result"
            } else {
                "\n\nAnswer: $result"
            }
            inputConnection.commitText(formattedResult, 1)
        }
        
        Log.d("SimpleKeyWise", "AI result applied: $result")
        
        // Save the new text state to history (for redo functionality)
        saveCurrentTextToHistory()
    }
    
    private fun showToast(message: String) {
        // Show actual toast message to user
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        Log.d("SimpleKeyWise", "Toast: $message")
    }
    
    private fun showAILoading(actionName: String) {
        isAIProcessing = true
        
        // Show loading indicator
        keyboardView?.findViewById<ProgressBar>(R.id.ai_loading_indicator)?.visibility = View.VISIBLE
        
        // Update status text
        keyboardView?.findViewById<TextView>(R.id.txt_ai_status)?.text = "$actionName..."
        
        // Disable all AI buttons
        setAIButtonsEnabled(false)
        
        Log.d("SimpleKeyWise", "AI loading state shown for: $actionName")
    }
    
    private fun hideAILoading() {
        isAIProcessing = false
        
        // Hide loading indicator
        keyboardView?.findViewById<ProgressBar>(R.id.ai_loading_indicator)?.visibility = View.GONE
        
        // Restore status text
        val statusText = if (isAISectionVisible) "AI Actions" else "AI Actions (Hidden)"
        keyboardView?.findViewById<TextView>(R.id.txt_ai_status)?.text = statusText
        
        // Re-enable all AI buttons
        setAIButtonsEnabled(true)
        
        Log.d("SimpleKeyWise", "AI loading state hidden")
    }
    
    private fun setAIButtonsEnabled(enabled: Boolean) {
        try {
            keyboardView?.let { view ->
                val buttons = listOf(
                    view.findViewById<Button>(R.id.btn_rewrite),
                    view.findViewById<Button>(R.id.btn_summarize),
                    view.findViewById<Button>(R.id.btn_explain),
                    view.findViewById<Button>(R.id.btn_listify),
                    view.findViewById<Button>(R.id.btn_emojify),
                    view.findViewById<Button>(R.id.btn_make_formal),
                    view.findViewById<Button>(R.id.btn_tweetify),
                    view.findViewById<Button>(R.id.btn_promptify),
                    view.findViewById<Button>(R.id.btn_translate),
                    view.findViewById<Button>(R.id.btn_creative),
                    view.findViewById<Button>(R.id.btn_answer),
                    view.findViewById<Button>(R.id.btn_letter)
                )
                
                buttons.forEach { button ->
                    button?.isEnabled = enabled
                    button?.alpha = if (enabled) 1.0f else 0.5f
                }
            }
        } catch (e: Exception) {
            Log.e("SimpleKeyWise", "Error setting AI button states: ${e.message}")
        }
    }

    override fun onUpdateSelection(oldSelStart: Int, oldSelEnd: Int, newSelStart: Int, newSelEnd: Int, candidatesStart: Int, candidatesEnd: Int) {
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd)
        
        // Show selection bar if text is selected, hide if no selection
        if (newSelStart != newSelEnd && !isSelectionBarVisible) {
            showSelectionBar()
        } else if (newSelStart == newSelEnd && isSelectionBarVisible) {
            hideSelectionBar()
        }
        
        // Save text state periodically during editing (but not too frequently)
        // Only save when cursor position changes significantly or text length changes
        if (kotlin.math.abs(newSelStart - oldSelStart) > 5 || 
            kotlin.math.abs(newSelEnd - oldSelEnd) > 5 ||
            (oldSelStart == oldSelEnd && newSelStart == newSelEnd)) {
            saveCurrentTextToHistory()
        }
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        
        // Reload and apply theme and key background styles when keyboard is shown
        // This ensures theme changes from settings are applied immediately
        loadCurrentTheme()
        loadCurrentKeyBackgroundStyle()
        applyThemeToKeyboard()
        applyKeyBackgroundStyles()
        applySuggestionThemeColors()
        
        Log.d("SimpleKeyWise", "Keyboard started, theme, key style, and suggestion colors refreshed: ${currentTheme.name}, ${currentKeyBackgroundStyle.name}")
    }

    override fun onDestroy() {
        keyboardScope.cancel()
        longPressHandler.removeCallbacksAndMessages(null)
        suggestionsHideHandler.removeCallbacksAndMessages(null) // Clean up suggestions timer
        super.onDestroy()
    }

    // Theme management methods
    private fun loadCurrentTheme() {
        val themeId = sharedPreferences.getString("keyboard_theme", "white") ?: "white"
        currentTheme = KeyboardThemes.getThemeById(themeId)
        Log.d("SimpleKeyWise", "Loaded theme: ${currentTheme.name}")
    }
    
    private fun loadCurrentKeyBackgroundStyle() {
        val styleId = sharedPreferences.getString("key_background_style", "dark") ?: "dark"
        currentKeyBackgroundStyle = KeyBackgroundStyles.getStyleById(styleId)
        Log.d("SimpleKeyWise", "Loaded key background style: ${currentKeyBackgroundStyle.name}")
    }
    
    // Method to refresh styles when preferences change
    private fun refreshStylesFromPreferences() {
        loadCurrentTheme()
        loadCurrentKeyBackgroundStyle()
        keyboardView?.let {
            applyThemeToKeyboard()
            applyKeyBackgroundStyles()
        }
        Log.d("SimpleKeyWise", "Refreshed styles from preferences")
    }
    
    private fun applyThemeToKeyboard() {
        keyboardView?.let { view ->
            // Target the specific root container by ID
            val rootContainer = view.findViewById<LinearLayout>(R.id.keyboard_root_container)
            rootContainer?.let { container ->
                
                Log.d("SimpleKeyWise", "Found root container, applying theme: ${currentTheme.name}")
                
                // First try a simple color test
                try {
                    container.setBackgroundColor(Color.parseColor(currentTheme.startColor))
                    Log.d("SimpleKeyWise", "Simple color test applied: ${currentTheme.startColor}")
                } catch (e: Exception) {
                    Log.e("SimpleKeyWise", "Error with simple color: ${e.message}")
                }
                
                // Then try gradient
                try {
                    val gradientDrawable = GradientDrawable(
                        GradientDrawable.Orientation.TOP_BOTTOM,
                        intArrayOf(
                            Color.parseColor(currentTheme.startColor),
                            Color.parseColor(currentTheme.endColor)
                        )
                    )
                    
                    // Match the original design: top corners rounded, stroke border
                    gradientDrawable.cornerRadii = floatArrayOf(
                        60f, 60f,  // top-left radius in pixels (20dp = ~60px)
                        60f, 60f,  // top-right radius in pixels
                        0f, 0f,    // bottom-right radius in pixels  
                        0f, 0f     // bottom-left radius in pixels
                    )
                    
                    // Add stroke border like the original
                    gradientDrawable.setStroke(3, Color.parseColor("#2C2C2E"))
                    
                    // Force apply the new background - this will override the XML background
                    container.background = null  // Clear existing background first
                    container.background = gradientDrawable
                    
                    Log.d("SimpleKeyWise", "Gradient applied successfully: ${currentTheme.startColor} to ${currentTheme.endColor}")
            } catch (e: Exception) {
                    Log.e("SimpleKeyWise", "Error applying gradient: ${e.message}")
                }
                
            } ?: run {
                Log.e("SimpleKeyWise", "Root container with ID not found")
            }
        } ?: run {
            Log.e("SimpleKeyWise", "Keyboard view is null")
        }
    }
    
    private fun applyKeyBackgroundStyles() {
        keyboardView?.let { view ->
            Log.d("SimpleKeyWise", "Applying key background style: ${currentKeyBackgroundStyle.name}")
            
            // Get the drawable resource ID for the current style
            val drawableResId = KeyBackgroundStyles.getDrawableResourceId(this, currentKeyBackgroundStyle.id)
            
            if (drawableResId != 0) {
                // List of all key button IDs that should use the key background style
                val keyButtonIds = listOf(
                    // Letter keys
                    R.id.key_q, R.id.key_w, R.id.key_e, R.id.key_r, R.id.key_t,
                    R.id.key_y, R.id.key_u, R.id.key_i, R.id.key_o, R.id.key_p,
                    R.id.key_a, R.id.key_s, R.id.key_d, R.id.key_f, R.id.key_g,
                    R.id.key_h, R.id.key_j, R.id.key_k, R.id.key_l,
                    R.id.key_z, R.id.key_x, R.id.key_c, R.id.key_v, R.id.key_b,
                    R.id.key_n, R.id.key_m,
                    
                    // Number keys
                    R.id.key_1, R.id.key_2, R.id.key_3, R.id.key_4, R.id.key_5,
                    R.id.key_6, R.id.key_7, R.id.key_8, R.id.key_9, R.id.key_0,
                    
                    // Symbol keys (using actual IDs from the layout)
                    R.id.key_exclamation, R.id.key_at, R.id.key_hash, R.id.key_dollar,
                    R.id.key_percent, R.id.key_caret, R.id.key_ampersand, R.id.key_asterisk,
                    R.id.key_minus, R.id.key_equals, R.id.key_plus, R.id.key_underscore,
                    R.id.key_semicolon, R.id.key_colon, R.id.key_quotes, R.id.key_apostrophe,
                    R.id.key_comma, R.id.key_period, R.id.key_question, R.id.key_left_paren,
                    R.id.key_backslash, R.id.key_pipe, R.id.key_tilde, R.id.key_grave,
                    R.id.key_parentheses, R.id.key_brackets, R.id.key_curly_brackets,
                    
                    // Space keys
                    R.id.key_space, R.id.key_space_num, R.id.key_space_sym, R.id.key_space_emoji,
                    
                    // Other number/symbol layout keys
                    R.id.key_period_num
                )
                
                // Apply the background to each key button
                keyButtonIds.forEach { keyId ->
                    view.findViewById<Button>(keyId)?.let { button ->
                        try {
                            button.setBackgroundResource(drawableResId)
                            
                            // Adjust text color based on key background style
                            when (currentKeyBackgroundStyle.id) {
                                "light_white" -> {
                                    button.setTextColor(Color.parseColor("#333333")) // Dark text for light background
                                }
                                "light_transparent" -> {
                                    button.setTextColor(Color.parseColor("#444444")) // Slightly lighter dark text
                                }
                                else -> {
                                    button.setTextColor(Color.parseColor("#FFFFFF")) // White text for dark background
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("SimpleKeyWise", "Error applying background to key ${keyId}: ${e.message}")
                        }
                    }
                }
                
                Log.d("SimpleKeyWise", "Key background styles applied successfully")
            } else {
                Log.e("SimpleKeyWise", "Could not find drawable resource for style: ${currentKeyBackgroundStyle.id}")
            }
        } ?: run {
            Log.e("SimpleKeyWise", "Keyboard view is null, cannot apply key backgrounds")
        }
    }
    
    private fun applySuggestionThemeColors() {
        keyboardView?.let { view ->
            // Determine adaptive text color for suggestions and status
            val adaptiveTextColor = when (currentTheme.id) {
                "white" -> Color.parseColor("#333333") // Dark text for white theme
                else -> Color.parseColor("#FFFFFF") // White text for dark themes
            }
            
            // AI Action buttons always use white text for consistency
            val aiButtonTextColor = Color.parseColor("#FFFFFF")
            
            // Apply adaptive text color to suggestion TextViews
            view.findViewById<TextView>(R.id.suggestion_1)?.setTextColor(adaptiveTextColor)
            view.findViewById<TextView>(R.id.suggestion_2)?.setTextColor(adaptiveTextColor)
            view.findViewById<TextView>(R.id.suggestion_3)?.setTextColor(adaptiveTextColor)
            
            // Apply adaptive text color to AI Actions status text
            view.findViewById<TextView>(R.id.txt_ai_status)?.setTextColor(adaptiveTextColor)
            
            // Apply white text color to all AI Action buttons
            view.findViewById<Button>(R.id.btn_rewrite)?.setTextColor(aiButtonTextColor)
            view.findViewById<Button>(R.id.btn_summarize)?.setTextColor(aiButtonTextColor)
            view.findViewById<Button>(R.id.btn_explain)?.setTextColor(aiButtonTextColor)
            view.findViewById<Button>(R.id.btn_listify)?.setTextColor(aiButtonTextColor)
            view.findViewById<Button>(R.id.btn_emojify)?.setTextColor(aiButtonTextColor)
            view.findViewById<Button>(R.id.btn_make_formal)?.setTextColor(aiButtonTextColor)
            view.findViewById<Button>(R.id.btn_tweetify)?.setTextColor(aiButtonTextColor)
            view.findViewById<Button>(R.id.btn_promptify)?.setTextColor(aiButtonTextColor)
            view.findViewById<Button>(R.id.btn_translate)?.setTextColor(aiButtonTextColor)
            view.findViewById<Button>(R.id.btn_creative)?.setTextColor(aiButtonTextColor)
            view.findViewById<Button>(R.id.btn_answer)?.setTextColor(aiButtonTextColor)
            view.findViewById<Button>(R.id.btn_letter)?.setTextColor(aiButtonTextColor)
            
            Log.d("SimpleKeyWise", "Applied adaptive text colors (suggestions/status) and white AI button text for theme: ${currentTheme.name}")
        }
    }

    // Word suggestion engine
    inner class WordSuggestionEngine {
        val commonWords = mapOf(
            "a" to listOf("a", "an", "and", "are", "as", "at", "all", "any", "about", "after", "also", "always", "am", "another", "ask", "asked", "asking", "away", "again", "against", "able", "above", "across", "actually", "add", "added", "almost", "along", "already", "although", "amazing", "among", "around", "article", "available", "avoid", "appear", "approach", "argument", "answer", "anything", "anywhere", "anyone", "area", "arm", "arms", "army", "art", "article"),
            
            "b" to listOf("be", "been", "being", "but", "by", "back", "bad", "became", "because", "become", "before", "began", "begin", "beginning", "behind", "believe", "below", "best", "better", "between", "big", "black", "blue", "book", "both", "boy", "bring", "brought", "build", "built", "business", "buy", "beautiful", "became", "bedroom", "beside", "beyond", "billion", "birth", "board", "body", "born", "break", "brother", "brought", "brown", "building"),
            
            "c" to listOf("can", "could", "come", "came", "call", "called", "change", "changed", "children", "city", "close", "closed", "color", "common", "company", "complete", "consider", "continue", "control", "correct", "country", "couple", "course", "create", "created", "cut", "car", "care", "carry", "case", "catch", "caught", "cause", "certain", "certainly", "chair", "chance", "check", "child", "choose", "chose", "chosen", "church", "class", "clear", "clearly", "college", "community", "computer", "condition", "contain", "cost", "cover", "current", "customer"),
            
            "d" to listOf("do", "did", "does", "done", "doing", "don't", "down", "day", "days", "during", "data", "date", "dead", "deal", "death", "decide", "decided", "decision", "deep", "degree", "describe", "design", "detail", "determine", "develop", "development", "die", "died", "difference", "different", "difficult", "dinner", "direction", "directly", "discuss", "discussion", "door", "draw", "drew", "drive", "drove", "drop", "dropped", "drug", "duck", "due", "daughter", "deal", "dear", "decided", "definitely", "department", "despite", "detail", "developed", "difficult", "discover", "disease", "dollar", "domestic", "dozen", "dream", "dress", "drink", "drop"),
            
            "e" to listOf("each", "early", "earth", "easily", "east", "easy", "eat", "economic", "economy", "education", "effect", "eight", "either", "election", "else", "employee", "end", "energy", "enough", "entire", "environment", "especially", "establish", "even", "evening", "event", "ever", "every", "everyone", "everything", "evidence", "exactly", "example", "executive", "exist", "expect", "experience", "explain", "eye", "eyes", "edge", "effort", "either", "empty", "enter", "entire", "equal", "equipment", "error", "escape", "especially", "essential", "establish", "evaluate", "evening", "eventually", "examine", "excellent", "except", "exchange", "exciting", "exercise", "exist", "expand", "expect", "expensive", "expert", "express", "extra", "extreme"),
            
            "f" to listOf("for", "from", "first", "find", "found", "few", "far", "fast", "father", "face", "fact", "factor", "fail", "fall", "family", "famous", "fear", "federal", "feel", "feeling", "feet", "fell", "felt", "field", "fight", "figure", "file", "fill", "film", "final", "finally", "financial", "fire", "firm", "fish", "five", "floor", "fly", "focus", "follow", "food", "foot", "force", "foreign", "forget", "form", "former", "forward", "four", "free", "friend", "front", "full", "fund", "future", "fabric", "facility", "factor", "faculty", "familiar", "fantasy", "fashion", "feature", "federal", "fellow", "female", "festival", "fiction", "fifteen", "finger", "finish", "fitness", "flavor", "flight", "flower", "folder", "football", "foreign", "forever", "forget", "formal", "format", "formula", "fortune", "forward", "foundation", "frame", "freedom", "frequently", "fresh", "friday", "friendly", "function", "funny", "furniture"),
            
            "g" to listOf("get", "got", "give", "gave", "given", "go", "went", "gone", "going", "good", "great", "group", "grow", "grew", "grown", "game", "general", "girl", "glass", "government", "ground", "gun", "guy", "garden", "gas", "gather", "generation", "gentleman", "gift", "glad", "global", "goal", "gold", "golf", "grab", "grade", "grain", "grand", "grant", "grass", "grave", "gray", "green", "gross", "group", "grow", "growth", "guarantee", "guard", "guess", "guest", "guide", "guilty", "guitar"),
            
            "h" to listOf("have", "has", "had", "having", "he", "his", "him", "her", "here", "how", "house", "home", "however", "hand", "hands", "happen", "happened", "happy", "hard", "head", "health", "hear", "heard", "heart", "heat", "heavy", "help", "herself", "high", "himself", "history", "hit", "hold", "held", "hope", "hospital", "hot", "hotel", "hour", "hours", "huge", "human", "hundred", "husband", "hair", "half", "hall", "handle", "hang", "happen", "happiness", "harbor", "hardly", "harm", "hat", "hate", "headline", "healthy", "hearing", "height", "hell", "hero", "hide", "highway", "hill", "hip", "hire", "holiday", "holy", "honest", "honor", "horrible", "horse", "host", "hotel", "household", "housing", "huge", "human", "humor", "hungry", "hunt", "hunter", "hurry", "hurt", "husband"),
            
            "i" to listOf("I", "in", "is", "it", "its", "if", "into", "information", "important", "include", "including", "increase", "indeed", "individual", "industry", "inside", "instead", "interest", "international", "interview", "investment", "issue", "item", "idea", "identify", "image", "imagine", "impact", "improve", "indicate", "influence", "initial", "institution", "instruction", "instrument", "insurance", "intelligence", "intention", "internet", "iron", "island", "ice", "icon", "ideal", "identity", "ignore", "ill", "illegal", "illness", "immediate", "impact", "implement", "imply", "import", "impose", "impossible", "impress", "improve", "incident", "income", "indeed", "independent", "index", "indicate", "individual", "industrial", "industry", "inevitable", "infant", "infection", "inflation", "influence", "inform", "infrastructure", "initial", "initiative", "injury", "inner", "innocent", "input", "inquiry", "inside", "insight", "inspire", "install", "instance", "instant", "instead", "institute", "institution", "instruction", "instrument", "insurance", "intellectual", "intelligence", "intend", "intense", "intention", "interact", "interest", "interface", "internal", "international", "internet", "interpret", "interrupt", "interview", "introduce", "invasion", "invest", "investigate", "investment", "invite", "involve", "iron", "island", "issue", "item"),
            
            "j" to listOf("just", "job", "join", "joined", "jump", "jumped", "june", "july", "january", "jail", "jam", "jazz", "jealous", "jeans", "jet", "jewelry", "job", "join", "joke", "journal", "journey", "joy", "judge", "judgment", "juice", "jump", "junior", "jury", "justice", "justify"),
            
            "k" to listOf("keep", "kept", "key", "kid", "kids", "kill", "killed", "kind", "king", "kitchen", "knee", "knew", "knife", "knock", "know", "known", "knowledge", "kick", "kidney", "kill", "kind", "king", "kiss", "kit", "kitchen", "knee", "knife", "knock", "knot", "know", "knowledge"),
            
            "l" to listOf("last", "late", "later", "law", "lay", "lead", "led", "learn", "learned", "least", "leave", "left", "leg", "legal", "less", "let", "letter", "level", "lie", "life", "light", "like", "line", "list", "listen", "little", "live", "lived", "living", "local", "long", "look", "looked", "lose", "lost", "lot", "love", "low", "lunch", "lady", "land", "language", "large", "last", "late", "later", "laugh", "launch", "law", "lawyer", "lay", "layer", "lead", "leader", "leadership", "leading", "leaf", "league", "lean", "leap", "learn", "lease", "least", "leather", "leave", "lecture", "left", "leg", "legal", "legislation", "legitimate", "length", "less", "lesson", "letter", "level", "library", "license", "lid", "lie", "life", "lift", "light", "like", "likely", "limit", "limitation", "limited", "line", "link", "lion", "lip", "liquid", "list", "listen", "literally", "literature", "little", "live", "living", "loan", "lobby", "local", "location", "lock", "logic", "logical", "long", "look", "loop", "loose", "lord", "lose", "loss", "lost", "lot", "loud", "love", "lovely", "lover", "low", "lower", "luck", "lucky", "lunch", "lung"),
            
            "m" to listOf("make", "made", "making", "man", "many", "may", "me", "mean", "meant", "might", "mind", "minute", "miss", "missed", "money", "month", "months", "more", "morning", "most", "mother", "move", "moved", "much", "music", "must", "my", "myself", "machine", "magazine", "magic", "mail", "main", "maintain", "major", "majority", "male", "mall", "manage", "management", "manager", "manner", "manufacture", "manufacturer", "manufacturing", "march", "margin", "mark", "market", "marketing", "marriage", "married", "marry", "mask", "mass", "massive", "master", "match", "material", "mathematics", "matter", "maximum", "maybe", "mayor", "meal", "meaning", "meaningful", "means", "meanwhile", "measure", "measurement", "meat", "mechanism", "media", "medical", "medicine", "medium", "meet", "meeting", "member", "membership", "memory", "mental", "mention", "menu", "mere", "merely", "mess", "message", "metal", "meter", "method", "middle", "might", "military", "milk", "million", "mind", "mine", "minimum", "mining", "minister", "minor", "minority", "minute", "miracle", "mirror", "miss", "missile", "mission", "mistake", "mix", "mixture", "mobile", "mode", "model", "moderate", "modern", "modest", "modify", "mom", "moment", "monday", "money", "monitor", "month", "mood", "moon", "moral", "more", "moreover", "morning", "mortgage", "most", "mostly", "mother", "motion", "motivation", "motor", "mount", "mountain", "mouse", "mouth", "move", "movement", "movie", "multiple", "murder", "muscle", "museum", "music", "musical", "musician", "mutual", "mystery"),
            
            "n" to listOf("no", "not", "now", "new", "next", "night", "nothing", "number", "name", "named", "nation", "national", "nature", "natural", "near", "nearly", "necessary", "need", "needed", "network", "never", "news", "nice", "nine", "nobody", "nor", "north", "northern", "note", "noted", "notice", "notion", "novel", "nuclear", "nail", "narrative", "narrow", "nation", "native", "natural", "naturally", "nature", "navy", "near", "nearby", "nearly", "neat", "necessarily", "necessary", "neck", "need", "negative", "negotiate", "negotiation", "neighbor", "neighborhood", "neither", "nerve", "nervous", "nest", "net", "network", "neutral", "never", "nevertheless", "new", "newly", "news", "newspaper", "next", "nice", "night", "nine", "nineteen", "ninety", "ninth", "nobody", "nod", "noise", "nomination", "none", "nonetheless", "noon", "nor", "normal", "normally", "north", "northern", "nose", "not", "notable", "note", "nothing", "notice", "notion", "novel", "november", "now", "nowhere", "nuclear", "number", "numerous", "nurse", "nut"),
            
            "o" to listOf("of", "on", "or", "one", "only", "other", "others", "our", "ours", "out", "over", "own", "old", "open", "opened", "option", "order", "ordered", "organization", "original", "office", "officer", "official", "often", "oil", "okay", "once", "online", "operation", "opportunity", "outside", "obvious", "obviously", "occasion", "occasionally", "occur", "ocean", "october", "odd", "odds", "offense", "offer", "offering", "office", "officer", "official", "often", "oil", "okay", "old", "older", "olympic", "once", "ongoing", "online", "only", "onto", "open", "opening", "operate", "operating", "operation", "operator", "opinion", "opponent", "opportunity", "oppose", "opposite", "option", "orange", "order", "ordinary", "organic", "organization", "organize", "orientation", "origin", "original", "originally", "other", "otherwise", "ought", "outcome", "outdoor", "outer", "outline", "outlook", "output", "outrage", "outside", "outstanding", "oval", "over", "overall", "overcome", "overlook", "owe", "own", "owner", "ownership"),
            
            "p" to listOf("people", "person", "place", "point", "put", "public", "part", "party", "play", "played", "player", "please", "policy", "political", "politics", "popular", "population", "possible", "power", "practice", "present", "president", "pressure", "pretty", "prevent", "previous", "price", "private", "probably", "problem", "process", "produce", "product", "production", "professional", "program", "project", "property", "protect", "provide", "page", "pain", "paint", "painting", "pair", "pale", "palm", "pan", "panel", "paper", "parent", "park", "parking", "parliament", "part", "participant", "participate", "participation", "particular", "particularly", "partly", "partner", "partnership", "party", "pass", "passage", "passenger", "passion", "past", "patch", "path", "patient", "pattern", "pause", "pay", "payment", "peace", "peak", "peer", "penalty", "pension", "people", "pepper", "per", "perceive", "percent", "percentage", "perception", "perfect", "perfectly", "perform", "performance", "perhaps", "period", "permanent", "permission", "permit", "person", "personal", "personality", "personally", "personnel", "perspective", "phase", "phenomenon", "philosophy", "phone", "photo", "photograph", "photographer", "photography", "phrase", "physical", "physically", "physician", "piano", "pick", "picture", "piece", "pile", "pilot", "pin", "pink", "pipe", "pitch", "pizza", "place", "plan", "plane", "planet", "planning", "plant", "plastic", "plate", "platform", "play", "player", "playing", "plaza", "pleasant", "please", "pleasure", "plenty", "plot", "plus", "pocket", "poem", "poet", "poetry", "point", "pole", "police", "policy", "political", "politically", "politician", "politics", "poll", "pollution", "pool", "poor", "pop", "popular", "popularity", "population", "port", "portion", "portrait", "pose", "position", "positive", "possess", "possession", "possibility", "possible", "possibly", "post", "pot", "potato", "potential", "potentially", "pound", "pour", "poverty", "powder", "power", "powerful", "practical", "practice", "pray", "prayer", "precisely", "predict", "prefer", "preference", "pregnant", "premise", "premium", "preparation", "prepare", "prescription", "presence", "present", "presentation", "preserve", "president", "presidential", "press", "pressure", "pretend", "pretty", "prevent", "previous", "previously", "price", "pride", "priest", "primarily", "primary", "prime", "principal", "principle", "print", "prior", "priority", "prison", "prisoner", "privacy", "private", "probably", "problem", "procedure", "proceed", "process", "produce", "producer", "product", "production", "productive", "profession", "professional", "professor", "profile", "profit", "program", "project", "prominent", "promise", "promote", "prompt", "proof", "proper", "properly", "property", "proportion", "proposal", "propose", "proposed", "prosecution", "prosecutor", "prospect", "protect", "protection", "protein", "protest", "proud", "prove", "provide", "provider", "province", "provision", "psychology", "public", "publication", "publicity", "publicly", "publish", "publisher", "pull", "punishment", "purchase", "pure", "purpose", "pursue", "push", "put"),
            
            "q" to listOf("question", "quite", "quick", "quickly", "quiet", "quietly", "quality", "quarter", "queen", "query", "quest", "quote", "qualification", "qualify", "quantity", "quantum", "quarantine", "quarter", "quarterly", "queen", "question", "questionnaire", "quick", "quickly", "quiet", "quit", "quite", "quiz", "quota", "quote"),
            
            "r" to listOf("right", "really", "read", "reason", "remember", "report", "result", "return", "room", "run", "ran", "rather", "reach", "reached", "real", "realize", "receive", "recent", "recently", "recognize", "record", "red", "reduce", "refer", "reflect", "regard", "region", "relate", "relationship", "remain", "remove", "represent", "require", "research", "resource", "respond", "response", "responsibility", "rest", "race", "radio", "rail", "rain", "raise", "range", "rank", "rapid", "rapidly", "rare", "rarely", "rate", "rating", "ratio", "raw", "reach", "react", "reaction", "read", "reader", "reading", "ready", "real", "reality", "realize", "really", "reason", "reasonable", "reasonably", "recall", "receive", "recent", "recently", "recognize", "recommend", "recommendation", "record", "recover", "recovery", "red", "reduce", "reduction", "refer", "reference", "reflect", "reflection", "reform", "refugee", "refuse", "regard", "regarding", "regardless", "region", "regional", "register", "regular", "regularly", "regulation", "reject", "relate", "relation", "relationship", "relative", "relatively", "relax", "release", "relevant", "reliable", "relief", "religion", "religious", "reluctant", "rely", "remain", "remaining", "remarkable", "remember", "remind", "remote", "removal", "remove", "repeat", "repeatedly", "replace", "reply", "report", "represent", "representation", "representative", "reputation", "request", "require", "requirement", "rescue", "research", "researcher", "resemble", "reservation", "reserve", "resident", "resolve", "resource", "respect", "respond", "response", "responsibility", "responsible", "rest", "restaurant", "restore", "restriction", "result", "retain", "retire", "retirement", "return", "reveal", "revenue", "review", "revolution", "reward", "rich", "rid", "ride", "riding", "right", "ring", "rise", "rising", "risk", "risky", "rival", "river", "road", "rob", "robot", "rock", "role", "roll", "romantic", "roof", "room", "root", "rope", "rose", "rotate", "rough", "roughly", "round", "route", "routine", "row", "royal", "rub", "rule", "ruling", "rumor", "run", "running", "rural", "rush", "russian"),
            
            "s" to listOf("so", "some", "see", "saw", "seen", "she", "should", "say", "said", "same", "school", "set", "show", "showed", "shown", "since", "still", "such", "system", "state", "said", "sale", "save", "science", "second", "section", "security", "seem", "seemed", "sell", "send", "sent", "series", "serious", "serve", "service", "several", "share", "short", "side", "sign", "significant", "similar", "simple", "simply", "single", "sit", "sat", "site", "situation", "six", "size", "small", "social", "society", "sometimes", "something", "son", "soon", "sort", "sound", "source", "south", "southern", "space", "speak", "spoke", "spoken", "special", "specific", "spend", "spent", "staff", "stage", "stand", "stood", "standard", "start", "started", "station", "stay", "step", "stop", "stopped", "story", "street", "strong", "structure", "student", "study", "studied", "stuff", "style", "subject", "success", "successful", "suddenly", "suffer", "suggest", "summer", "sun", "sunday", "super", "support", "sure", "surface", "safe", "safety", "sake", "salary", "sale", "sales", "salt", "sample", "sand", "satellite", "satisfaction", "satisfy", "saturday", "sauce", "save", "saving", "scale", "scandal", "scenario", "scene", "schedule", "scheme", "scholar", "scholarship", "school", "science", "scientific", "scientist", "scope", "score", "scream", "screen", "script", "sea", "search", "season", "seat", "second", "secondary", "secret", "secretary", "section", "sector", "secure", "security", "see", "seed", "seek", "seem", "select", "selection", "self", "sell", "senate", "senator", "send", "senior", "sense", "sensitive", "sentence", "separate", "sequence", "series", "serious", "seriously", "serve", "service", "session", "set", "setting", "settle", "settlement", "setup", "seven", "seventeen", "seventh", "several", "severe", "sex", "sexual", "shade", "shadow", "shake", "shall", "shame", "shape", "share", "sharp", "she", "sheet", "shelf", "shell", "shelter", "shift", "shine", "ship", "shirt", "shock", "shoe", "shoot", "shooting", "shop", "shopping", "shore", "short", "shot", "should", "shoulder", "shout", "show", "shower", "shut", "sick", "side", "sight", "sign", "signal", "significant", "significantly", "silence", "silent", "silly", "silver", "similar", "similarly", "simple", "simply", "sin", "since", "sing", "singer", "singing", "single", "sink", "sir", "sister", "sit", "site", "sitting", "situation", "six", "sixteen", "sixth", "sixty", "size", "ski", "skill", "skin", "sky", "slave", "sleep", "slice", "slide", "slight", "slightly", "slip", "slow", "slowly", "small", "smart", "smell", "smile", "smoke", "smooth", "snap", "snow", "so", "soap", "soccer", "social", "society", "sock", "soft", "software", "soil", "solar", "sold", "sole", "solid", "solution", "solve", "some", "somebody", "somehow", "someone", "something", "sometimes", "somewhat", "somewhere", "son", "song", "soon", "sophisticated", "sort", "soul", "sound", "soup", "source", "south", "southern", "space", "spare", "speak", "speaker", "special", "specialist", "species", "specific", "specifically", "speech", "speed", "spend", "spending", "spin", "spirit", "spiritual", "split", "spokesman", "sport", "spot", "spread", "spring", "square", "squeeze", "stable", "staff", "stage", "stair", "stake", "stand", "standard", "standing", "star", "stare", "start", "state", "statement", "station", "statistics", "status", "stay", "steady", "steal", "steel", "step", "stick", "still", "stir", "stock", "stomach", "stone", "stop", "storage", "store", "storm", "story", "straight", "strange", "stranger", "strategy", "stream", "street", "strength", "stress", "stretch", "strike", "string", "strip", "stroke", "strong", "strongly", "structure", "struggle", "stuck", "student", "studio", "study", "stuff", "stupid", "style", "subject", "subsequent", "substance", "substantial", "succeed", "success", "successful", "successfully", "such", "sudden", "suddenly", "sue", "suffer", "sufficient", "sugar", "suggest", "suggestion", "suit", "summer", "summit", "sun", "sunday", "super", "superior", "supply", "support", "suppose", "sure", "surely", "surface", "surgery", "surprise", "surprised", "surprising", "surprisingly", "surround", "survey", "survival", "survive", "survivor", "suspect", "sustain", "swear", "sweep", "sweet", "swim", "swing", "switch", "symbol", "symptom", "system"),
            
            "t" to listOf("the", "to", "that", "this", "they", "them", "their", "there", "then", "than", "think", "thought", "through", "three", "time", "times", "today", "together", "told", "too", "took", "top", "toward", "town", "try", "tried", "turn", "turned", "two", "type", "table", "take", "taken", "taking", "talk", "talked", "talking", "tall", "tape", "target", "task", "tax", "tea", "teach", "teacher", "teaching", "team", "tear", "technology", "telephone", "television", "tell", "telling", "ten", "tend", "tennis", "tension", "tent", "term", "terms", "terrible", "territory", "terror", "terrorism", "terrorist", "test", "testing", "text", "thank", "thanks", "theater", "theatre", "theft", "themselves", "theory", "therapy", "therefore", "these", "thick", "thin", "thing", "things", "thinking", "third", "thirteen", "thirty", "those", "though", "thousand", "threat", "threaten", "threw", "throw", "thrown", "thumb", "thus", "ticket", "tie", "tight", "till", "tip", "tire", "tired", "tissue", "title", "tobacco", "today", "toe", "together", "tomato", "tomorrow", "tone", "tongue", "tonight", "tool", "tooth", "topic", "total", "totally", "touch", "tough", "tour", "tourist", "tournament", "toward", "towards", "tower", "town", "toy", "track", "trade", "tradition", "traditional", "traffic", "trail", "train", "training", "transfer", "transform", "transformation", "transition", "translate", "transportation", "trap", "travel", "treat", "treatment", "tree", "tremendous", "trend", "trial", "tribe", "trick", "trip", "troop", "trouble", "truck", "true", "truly", "trust", "truth", "try", "trying", "tube", "tuesday", "tune", "tunnel", "turkey", "turn", "twelve", "twenty", "twice", "twin", "twist", "type", "typical", "typically"),
            
            "u" to listOf("up", "us", "use", "used", "using", "under", "understand", "understood", "until", "upon", "university", "unless", "unlike", "up", "update", "upon", "upper", "urban", "urge", "urgent", "usage", "useful", "user", "usual", "usually", "utility", "ultimate", "ultimately", "unable", "uncle", "uncomfortable", "unconscious", "under", "undergo", "underlying", "understand", "understanding", "unemployment", "unexpected", "unfortunately", "uniform", "union", "unique", "unit", "united", "unity", "universal", "universe", "university", "unknown", "unless", "unlike", "unlikely", "unlock", "unnecessary", "unpleasant", "until", "unusual", "unwilling", "upon", "upper", "upset", "urban", "urge", "urgent", "usage", "useful", "user", "usual", "usually", "utility"),
            
            "v" to listOf("very", "value", "various", "video", "view", "voice", "vote", "visit", "visited", "via", "victim", "victory", "village", "violence", "violent", "virtual", "virtue", "virus", "visible", "vision", "visual", "vital", "volume", "volunteer", "vacation", "valley", "valuable", "van", "variable", "variation", "variety", "vary", "vast", "vegetable", "vehicle", "venture", "version", "versus", "vessel", "veteran", "via", "victim", "video", "view", "viewer", "village", "violation", "violence", "violent", "virgin", "virtual", "virtually", "virtue", "virus", "visible", "vision", "visit", "visitor", "visual", "vital", "vitamin", "vocabulary", "vocal", "voice", "volume", "volunteer", "vote", "voter", "vulnerable"),
            
            "w" to listOf("we", "was", "were", "will", "would", "with", "what", "when", "where", "who", "why", "which", "while", "white", "way", "want", "wanted", "war", "warm", "warn", "warning", "wash", "waste", "watch", "water", "wave", "weak", "wealth", "weapon", "wear", "wearing", "weather", "web", "website", "wedding", "wednesday", "week", "weekend", "weekly", "weight", "welcome", "welfare", "well", "west", "western", "wet", "wheel", "whereas", "whether", "wide", "widely", "wife", "wild", "wildlife", "win", "wind", "window", "wine", "wing", "winner", "winning", "winter", "wire", "wise", "wish", "within", "without", "witness", "woman", "women", "won", "wonder", "wonderful", "wood", "wooden", "wool", "word", "work", "worked", "worker", "working", "workplace", "workshop", "world", "worldwide", "worry", "worried", "worse", "worst", "worth", "would", "wound", "wrap", "write", "writer", "writing", "written", "wrote", "wrong", "yard", "yeah", "year", "yellow", "yes", "yesterday", "yet", "yield", "young", "younger", "your", "yours", "yourself", "youth", "wait", "wake", "walk", "walking", "wall", "wander", "want", "war", "ward", "warm", "warn", "warning", "wash", "waste", "watch", "water", "wave", "way", "weak", "wealth", "weapon", "wear", "weather", "web", "wedding", "week", "weekend", "weight", "weird", "welcome", "welfare", "west", "western", "wet", "whatever", "wheat", "wheel", "whenever", "whereas", "whereby", "whether", "whoever", "whole", "whom", "whose", "wide", "widely", "widow", "wife", "wild", "will", "willing", "win", "wind", "window", "wine", "wing", "winner", "winter", "wire", "wisdom", "wise", "wish", "withdraw", "within", "without", "witness", "woman", "wonder", "wonderful", "wood", "wooden", "word", "work", "worker", "working", "workplace", "workshop", "world", "worldwide", "worn", "worried", "worry", "worse", "worship", "worth", "would", "wound", "wrap", "write", "writer", "writing", "written", "wrong"),
            
            "x" to listOf("x-ray", "xenophobia", "xerox", "xylem", "xylophone"),
            
            "y" to listOf("you", "your", "yours", "yourself", "year", "years", "yes", "yet", "yesterday", "yield", "young", "younger", "youngest", "youth", "yard", "yarn", "yawn", "yeah", "yellow", "yen", "yoga", "yogurt", "yolk", "zone"),
            
            "z" to listOf("zero", "zone", "zoo", "zoom", "zeal", "zebra", "zinc", "zip", "zodiac", "zombie", "zoning")
        )
        
        fun getSuggestions(word: String): List<String> {
            if (word.isBlank()) return emptyList()
            
            val lowerWord = word.lowercase()
            val suggestions = mutableListOf<String>()
            
            // First, check personal dictionary for matches (highest priority)
            val personalMatches = personalDictionary.filter { it.lowercase().startsWith(lowerWord) }
                .sortedBy { it.length } // Shorter words first
                .take(3)
            suggestions.addAll(personalMatches)
            
            // If we still need more suggestions, get from common words
            if (suggestions.size < 3) {
                val firstLetter = lowerWord.first().toString()
                val wordsForLetter = commonWords[firstLetter] ?: emptyList()
                
                // Find words that start with the typed word (excluding already added personal words)
                wordsForLetter.filter { 
                    it.startsWith(lowerWord) && !suggestions.contains(it) 
                }
                .take(3 - suggestions.size)
                .forEach { suggestions.add(it) }
            }
            
            // If we still don't have enough suggestions, add some common words
            if (suggestions.size < 3) {
                val commonSuggestions = listOf("the", "and", "for", "you", "with", "that", "this", "have", "will", "can")
                commonSuggestions.filter { 
                    it.startsWith(lowerWord) && !suggestions.contains(it) 
                }
                .take(3 - suggestions.size)
                .forEach { suggestions.add(it) }
            }
            
            return suggestions.take(3)
        }
    }

    private fun setupAutoSuggestions(view: View) {
        // Setup suggestion buttons
        view.findViewById<TextView>(R.id.suggestion_1)?.setOnClickListener {
            if (currentSuggestions.isNotEmpty()) {
                applySuggestion(currentSuggestions[0])
            }
        }
        
        view.findViewById<TextView>(R.id.suggestion_2)?.setOnClickListener {
            if (currentSuggestions.size > 1) {
                applySuggestion(currentSuggestions[1])
            }
        }
        
        view.findViewById<TextView>(R.id.suggestion_3)?.setOnClickListener {
            if (currentSuggestions.size > 2) {
                applySuggestion(currentSuggestions[2])
            }
        }
        
        Log.d("SimpleKeyWise", "Auto-suggestions click handlers setup")
    }
    
    private fun updateAutoSuggestions() {
        val inputConnection = currentInputConnection ?: return
        
        // Cancel any pending hide timer since user is actively typing
        cancelSuggestionsHideTimer()
        
        try {
            // Get current text and cursor position
            val extractedText = inputConnection.getExtractedText(
                android.view.inputmethod.ExtractedTextRequest(), 0
            )
            
            val currentText = extractedText?.text?.toString() ?: ""
            val cursorPos = extractedText?.selectionStart ?: 0
            
            // Find the current word being typed
            currentWord = getCurrentWord(currentText, cursorPos)
            
            if (currentWord.length >= 2) {
                // Get suggestions for the current word
                currentSuggestions = suggestionEngine.getSuggestions(currentWord)
                showAutoSuggestions(currentSuggestions)
                
                // Start auto-hide timer after suggestions are shown
                startSuggestionsHideTimer()
            } else {
                // Hide suggestions if word is too short
                hideAutoSuggestions()
            }
            
        } catch (e: Exception) {
            Log.e("SimpleKeyWise", "Error updating auto-suggestions: ${e.message}")
        }
    }
    
    private fun getCurrentWord(text: String, cursorPos: Int): String {
        if (text.isEmpty() || cursorPos <= 0) return ""
        
        val textBeforeCursor = text.substring(0, minOf(cursorPos, text.length))
        val words = textBeforeCursor.split(Regex("\\s+"))
        
        return if (words.isNotEmpty()) {
            val lastWord = words.last()
            // Only return word if it contains only letters (no punctuation)
            if (lastWord.matches(Regex("[a-zA-Z]+"))) lastWord else ""
        } else {
            ""
        }
    }
    
    private fun showAutoSuggestions(suggestions: List<String>) {
        keyboardView?.let { view ->
            val suggestionContainer = view.findViewById<LinearLayout>(R.id.auto_complete_suggestions)
            val topButtonsContainer = view.findViewById<LinearLayout>(R.id.top_buttons_container)
            
            if (suggestions.isNotEmpty()) {
                // Update suggestion texts
                view.findViewById<TextView>(R.id.suggestion_1)?.apply {
                    text = suggestions.getOrNull(0) ?: ""
                    visibility = if (suggestions.size > 0) View.VISIBLE else View.GONE
                }
                view.findViewById<TextView>(R.id.suggestion_2)?.apply {
                    text = suggestions.getOrNull(1) ?: ""
                    visibility = if (suggestions.size > 1) View.VISIBLE else View.GONE
                }
                view.findViewById<TextView>(R.id.suggestion_3)?.apply {
                    text = suggestions.getOrNull(2) ?: ""
                    visibility = if (suggestions.size > 2) View.VISIBLE else View.GONE
                }
                
                // Apply theme-based text colors to suggestions
                applySuggestionThemeColors()
                
                // Cancel any ongoing animations
                suggestionContainer?.animate()?.cancel()
                topButtonsContainer?.animate()?.cancel()
                
                // Show suggestions with smooth fade in animation
                suggestionContainer?.let { container ->
                    container.visibility = View.VISIBLE
                    container.alpha = 0f
                    container.animate()
                        .alpha(1f)
                        .setDuration(200)
                        .setInterpolator(android.view.animation.DecelerateInterpolator())
                        .start()
                }
                
                // Hide top buttons with smooth fade out animation
                topButtonsContainer?.let { container ->
                    container.animate()
                        .alpha(0f)
                        .setDuration(150)
                        .setInterpolator(android.view.animation.AccelerateInterpolator())
                        .withEndAction {
                            container.visibility = View.GONE
                        }
                        .start()
                }
                
                isAutoSuggestionsVisible = true
                
                Log.d("SimpleKeyWise", "Showing suggestions with animation: ${suggestions.joinToString(", ")}")
            }
        }
    }
    
    private fun hideAutoSuggestions() {
        // Cancel auto-hide timer since we're manually hiding
        cancelSuggestionsHideTimer()
        
        keyboardView?.let { view ->
            val suggestionContainer = view.findViewById<LinearLayout>(R.id.auto_complete_suggestions)
            val topButtonsContainer = view.findViewById<LinearLayout>(R.id.top_buttons_container)
            
            // Cancel any ongoing animations
            suggestionContainer?.animate()?.cancel()
            topButtonsContainer?.animate()?.cancel()
            
            // Hide suggestions with smooth fade out animation
            suggestionContainer?.let { container ->
                container.animate()
                    .alpha(0f)
                    .setDuration(150)
                    .setInterpolator(android.view.animation.AccelerateInterpolator())
                    .withEndAction {
                        if (!isAutoSuggestionsVisible) { // Double check state
                            container.visibility = View.GONE
                        }
                    }
                    .start()
            }
            
            // Show top buttons with smooth fade in animation
            topButtonsContainer?.let { container ->
                container.visibility = View.VISIBLE
                container.alpha = 0f
                container.animate()
                    .alpha(1f)
                    .setDuration(200)
                    .setInterpolator(android.view.animation.DecelerateInterpolator())
                    .start()
            }
            
            isAutoSuggestionsVisible = false
            
            Log.d("SimpleKeyWise", "Auto-suggestions hidden with animation")
        }
    }
    
    private fun applySuggestion(suggestion: String) {
        val inputConnection = currentInputConnection ?: return
        
        // Cancel auto-hide timer since user is interacting with suggestions
        cancelSuggestionsHideTimer()
        
        try {
            // Delete the current partial word
            if (currentWord.isNotEmpty()) {
                inputConnection.deleteSurroundingText(currentWord.length, 0)
            }
            
            // Insert the complete suggestion followed by a space for natural typing flow
            inputConnection.commitText("$suggestion ", 1)
            
            // Hide suggestions after selection with animation
            hideAutoSuggestions()
            
            // Provide haptic feedback
            performKeyClickFeedback()
            
            Log.d("SimpleKeyWise", "Applied suggestion with auto-space: '$suggestion '")
            
        } catch (e: Exception) {
            Log.e("SimpleKeyWise", "Error applying suggestion: ${e.message}")
        }
    }
    
    // Auto-hide timer management for suggestions
    private fun startSuggestionsHideTimer() {
        // Cancel any existing timer
        cancelSuggestionsHideTimer()
        
        // Create new timer
        suggestionsHideRunnable = Runnable {
            if (isAutoSuggestionsVisible) {
                hideAutoSuggestions()
                Log.d("SimpleKeyWise", "Auto-hide suggestions triggered after ${SUGGESTIONS_HIDE_DELAY}ms")
            }
        }
        
        // Start the timer
        suggestionsHideHandler.postDelayed(suggestionsHideRunnable!!, SUGGESTIONS_HIDE_DELAY)
        Log.d("SimpleKeyWise", "Auto-hide timer started for suggestions")
    }
    
    private fun cancelSuggestionsHideTimer() {
        suggestionsHideRunnable?.let { runnable ->
            suggestionsHideHandler.removeCallbacks(runnable)
            suggestionsHideRunnable = null
            Log.d("SimpleKeyWise", "Auto-hide timer cancelled")
        }
    }
    
    // Personal Dictionary Management
    private fun loadPersonalDictionary() {
        try {
            val savedWords = sharedPreferences.getStringSet(PERSONAL_DICT_KEY, emptySet()) ?: emptySet()
            personalDictionary.clear()
            personalDictionary.addAll(savedWords)
            Log.d("SimpleKeyWise", "Loaded ${personalDictionary.size} words from personal dictionary")
        } catch (e: Exception) {
            Log.e("SimpleKeyWise", "Error loading personal dictionary: ${e.message}")
        }
    }
    
    private fun savePersonalDictionary() {
        try {
            sharedPreferences.edit()
                .putStringSet(PERSONAL_DICT_KEY, personalDictionary.toSet())
                .apply()
            Log.d("SimpleKeyWise", "Saved ${personalDictionary.size} words to personal dictionary")
        } catch (e: Exception) {
            Log.e("SimpleKeyWise", "Error saving personal dictionary: ${e.message}")
        }
    }
    
    private fun learnCurrentWord() {
        val inputConnection = currentInputConnection ?: return
        
        try {
            // Get the current word being typed
            val extractedText = inputConnection.getExtractedText(
                android.view.inputmethod.ExtractedTextRequest(), 0
            )
            
            val currentText = extractedText?.text?.toString() ?: ""
            val cursorPos = extractedText?.selectionStart ?: 0
            
            val wordToLearn = getCurrentWord(currentText, cursorPos)
            
            // Only learn words that are:
            // 1. At least 2 characters long
            // 2. Contain only letters (no numbers or symbols)
            // 3. Not already in our dictionaries
            if (wordToLearn.length >= 2 && 
                wordToLearn.matches(Regex("[a-zA-Z]+")) &&
                !personalDictionary.contains(wordToLearn.lowercase()) &&
                !isCommonWord(wordToLearn)) {
                
                personalDictionary.add(wordToLearn.lowercase())
                savePersonalDictionary()
                
                Log.d("SimpleKeyWise", "Learned new word: '$wordToLearn'")
            }
            
        } catch (e: Exception) {
            Log.e("SimpleKeyWise", "Error learning current word: ${e.message}")
        }
    }
    
    private fun isCommonWord(word: String): Boolean {
        val lowerWord = word.lowercase()
        val firstLetter = lowerWord.firstOrNull()?.toString() ?: return false
        val wordsForLetter = suggestionEngine.commonWords[firstLetter] ?: emptyList()
        return wordsForLetter.contains(lowerWord)
    }

    private fun openChatTab() {
        try {
            val intent = Intent(this, MainActivity::class.java)
            intent.action = Intent.ACTION_MAIN
            intent.addCategory(Intent.CATEGORY_LAUNCHER)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            intent.putExtra("open_chat", true)
            startActivity(intent)
            
            // Provide feedback to user
            showToast("Opening Chat...")
            Log.d("SimpleKeyWise", "Chat tab opened successfully")
            
        } catch (e: Exception) {
            Log.e("SimpleKeyWise", "Error opening chat tab: ${e.message}")
            showToast("Error opening chat")
        }
    }
}