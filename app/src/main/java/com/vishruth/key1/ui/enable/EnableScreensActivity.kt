package com.vishruth.key1.ui.enable

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.vishruth.key1.R
import com.vishruth.key1.ui.theme.Key1Theme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * New Enable Screens Activity - Redesigned UI while maintaining existing functionality
 * This activity provides a modern enable flow based on provided image references
 */
class EnableScreensActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            Key1Theme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = colorResource(R.color.keywise_background)
                ) {
                    EnableScreensFlow()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnableScreensFlow() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    // Existing functionality preserved - keyboard status tracking
    val isKeyboardEnabled = remember { 
        mutableStateOf(isKeyboardEnabled(context))
    }
    
    val isKeyboardSelected = remember {
        mutableStateOf(isKeyboardSelected(context))
    }
    
    // Current screen state for navigation flow
    var currentScreen by remember { 
        mutableStateOf(
            when {
                !isKeyboardEnabled.value -> EnableScreen.ENABLE_KEYBOARD
                !isKeyboardSelected.value -> EnableScreen.SELECT_KEYBOARD
                else -> EnableScreen.SETUP_COMPLETE
            }
        )
    }
    
    // Function to refresh status - maintaining existing logic
    val refreshStatus = {
        val wasEnabled = isKeyboardEnabled.value
        val wasSelected = isKeyboardSelected.value
        
        isKeyboardEnabled.value = isKeyboardEnabled(context)
        isKeyboardSelected.value = isKeyboardSelected(context)
        
        // Auto-advance screens based on status
        when {
            !isKeyboardEnabled.value -> currentScreen = EnableScreen.ENABLE_KEYBOARD
            !isKeyboardSelected.value -> currentScreen = EnableScreen.SELECT_KEYBOARD
            isKeyboardEnabled.value && isKeyboardSelected.value -> currentScreen = EnableScreen.SETUP_COMPLETE
        }
    }
    
    // Lifecycle observer - maintaining existing functionality
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshStatus()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    
    // Periodic status check - maintaining existing functionality
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            refreshStatus()
        }
    }
    
    // New UI Layout based on image references
    when (currentScreen) {
        EnableScreen.ENABLE_KEYBOARD -> {
            EnableKeyboardScreen(
                onEnableClick = {
                    val intent = Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)
                    context.startActivity(intent)
                    CoroutineScope(Dispatchers.Main).launch {
                        delay(500)
                        refreshStatus()
                    }
                },
                onSkipClick = {
                    // Navigate to main app
                    val intent = Intent(context, com.vishruth.key1.MainActivity::class.java)
                    context.startActivity(intent)
                    if (context is ComponentActivity) {
                        context.finish()
                    }
                }
            )
        }
        
        EnableScreen.SELECT_KEYBOARD -> {
            SelectKeyboardScreen(
                onSelectClick = {
                    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.showInputMethodPicker()
                    CoroutineScope(Dispatchers.Main).launch {
                        delay(500)
                        refreshStatus()
                    }
                },
                onSkipClick = {
                    // Navigate to main app
                    val intent = Intent(context, com.vishruth.key1.MainActivity::class.java)
                    context.startActivity(intent)
                    if (context is ComponentActivity) {
                        context.finish()
                    }
                }
            )
        }
        
        EnableScreen.SETUP_COMPLETE -> {
            SetupCompleteScreen(
                onContinueClick = {
                    // Navigate to main app
                    val intent = Intent(context, com.vishruth.key1.MainActivity::class.java)
                    context.startActivity(intent)
                    if (context is ComponentActivity) {
                        context.finish()
                    }
                }
            )
        }
    }
}

enum class EnableScreen {
    ENABLE_KEYBOARD,
    SELECT_KEYBOARD,
    SETUP_COMPLETE
}

// Helper functions maintained from existing codebase
fun isKeyboardEnabled(context: Context): Boolean {
    val inputMethodManager = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    val enabledInputMethods = inputMethodManager.enabledInputMethodList
    return enabledInputMethods.any { it.packageName == context.packageName }
}

fun isKeyboardSelected(context: Context): Boolean {
    val inputMethodManager = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    val currentInputMethod = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.DEFAULT_INPUT_METHOD
    )
    return currentInputMethod?.contains(context.packageName) == true
}
