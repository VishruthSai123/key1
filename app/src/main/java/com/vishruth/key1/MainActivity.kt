package com.vishruth.key1

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.VectorPainter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector.Builder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.vishruth.key1.repository.AIRepository
import com.vishruth.key1.repository.ChatRepository
import com.vishruth.key1.ui.theme.Key1Theme
import com.vishruth.key1.data.KeyboardTheme
import com.vishruth.key1.data.KeyboardThemes
import com.vishruth.key1.data.KeyBackgroundStyle
import com.vishruth.key1.data.KeyBackgroundStyles
import com.vishruth.key1.data.ChatMessage as NewChatMessage
import com.vishruth.key1.data.ChatConversation
import com.vishruth.key1.data.MessageType
import com.vishruth.key1.ui.enable.EnableKeyboardScreen
import com.vishruth.key1.ui.enable.SelectKeyboardScreen
import com.vishruth.key1.ui.enable.SetupCompleteScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import android.content.ClipboardManager
import android.content.ClipData
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons as MaterialIcons
import androidx.compose.material.icons.filled.*
import java.text.SimpleDateFormat
import java.util.*
import android.util.Log

// Legacy chat message data class for UI compatibility
data class ChatMessage(
    val content: String,
    val isFromUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val messageType: MessageType = MessageType.TEXT
)

class MainActivity : ComponentActivity() {
    private var selectedTab: MutableState<AppTab>? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Check if we should open chat directly
        val openChat = intent.getBooleanExtra("open_chat", false)
        
        // Check keyboard setup status for onboarding logic
        val isKeyboardFullySetup = isKeyboardEnabled(this) && isKeyboardSelected(this)
        
        setContent {
            Key1Theme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = colorResource(R.color.keywise_background)
                ) {
                    if (!isKeyboardFullySetup && !openChat) {
                        // Show onboarding enable screens if keyboard not setup
                        EnableScreensOnboarding()
                    } else {
                        // Show main app if keyboard is setup or chat requested
                        SendRightSetupScreen(initialTab = if (openChat) AppTab.CHAT else AppTab.HOME) { tab ->
                            selectedTab = tab
                        }
                    }
                }
            }
        }
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        
        // Check if the new intent wants to open chat
        val openChat = intent.getBooleanExtra("open_chat", false)
        if (openChat) {
            selectedTab?.value = AppTab.CHAT
        }
    }
}

// Tab definitions - Main app sections
enum class AppTab(val title: String, val icon: ImageVector) {
    HOME("Home", Icons.Default.Home),
    CHAT("Chat", Icons.Default.Chat),
    SETTINGS("Settings", Icons.Default.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SendRightSetupScreen(initialTab: AppTab = AppTab.HOME, onTabSelected: ((MutableState<AppTab>) -> Unit)? = null) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val aiRepository = remember { AIRepository(context) }
    
    // Current selected tab
    val selectedTab = remember { mutableStateOf(initialTab) }
    
    // Notify parent about tab state if callback provided
    LaunchedEffect(selectedTab) {
        onTabSelected?.invoke(selectedTab)
    }

    val isKeyboardEnabled = remember { 
        mutableStateOf(isKeyboardEnabled(context))
    }
    
    val isKeyboardSelected = remember {
        mutableStateOf(isKeyboardSelected(context))
    }
    
    // Track restart status
    val hasRestartedAfterSetup = remember {
        mutableStateOf(hasUserRestartedAfterSetup(context))
    }
    
    // Track if restart popup has been shown for first-time completion
    val showRestartPopup = remember { mutableStateOf(false) }
    val hasShownPopup = remember { mutableStateOf(getHasShownRestartPopup(context)) }
    
    // Check if setup just completed and popup hasn't been shown yet
    LaunchedEffect(isKeyboardEnabled.value, isKeyboardSelected.value, hasShownPopup.value) {
        if (isKeyboardEnabled.value && isKeyboardSelected.value && !hasShownPopup.value) {
            showRestartPopup.value = true
        }
    }
    
    // Function to refresh status
    val refreshStatus = {
        isKeyboardEnabled.value = isKeyboardEnabled(context)
        isKeyboardSelected.value = isKeyboardSelected(context)
        hasRestartedAfterSetup.value = hasUserRestartedAfterSetup(context)
    }
    
    // Check status when lifecycle changes (user returns from settings)
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
    
    // Check status periodically for real-time updates
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(1000) // Check every 1 second for better responsiveness
            refreshStatus()
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(R.color.keywise_background)) // Add proper background
    ) {
        // Header
        AppHeader()
        
        // Main Content (takes remaining space above bottom navigation)
        Box(
            modifier = Modifier
                .weight(1f)
                .background(colorResource(R.color.keywise_background)) // Add background to content area
        ) {
            when (selectedTab.value) {
                AppTab.HOME -> HomeTabContent()
                AppTab.CHAT -> ChatTabContent()
                AppTab.SETTINGS -> SettingsTabContent(context)
            }
        }
        
        // Bottom Navigation
        BottomNavigationBar(
            selectedTab = selectedTab.value,
            onTabSelected = { tab ->
                selectedTab.value = tab
            }
        )
    }
    
    // First-time setup completion popup
    if (showRestartPopup.value) {
        RestartRecommendationDialog(
            onDismiss = {
                showRestartPopup.value = false
                setHasShownRestartPopup(context, true)
                hasShownPopup.value = true
            },
            onRestart = {
                showRestartPopup.value = false
                setHasShownRestartPopup(context, true)
                hasShownPopup.value = true
                // Note: Actual restart requires user action through power menu
            }
        )
    }
}

// Custom SVG Menu Icon
@Composable
fun CustomMenuIcon() {
    Canvas(modifier = Modifier.size(40.dp)) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        
        // Draw background circle
        drawCircle(
            color = Color(0x54648465), // #648465 with 33% opacity
            radius = canvasWidth / 2f,
            center = androidx.compose.ui.geometry.Offset(canvasWidth / 2f, canvasHeight / 2f)
        )
        
        // Draw three dots
        val dotRadius = 4f
        val centerX = canvasWidth / 2f
        
        // Top dot
        drawCircle(
            color = Color.Black,
            radius = dotRadius,
            center = androidx.compose.ui.geometry.Offset(centerX, canvasHeight * 0.325f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 8f)
        )
        
        // Middle dot
        drawCircle(
            color = Color.Black,
            radius = dotRadius,
            center = androidx.compose.ui.geometry.Offset(centerX, canvasHeight * 0.5f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 8f)
        )
        
        // Bottom dot
        drawCircle(
            color = Color.Black,
            radius = dotRadius,
            center = androidx.compose.ui.geometry.Offset(centerX, canvasHeight * 0.675f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 8f)
        )
    }
}

@Composable
fun AppHeader() {
    val context = LocalContext.current
    
    // Load logo bitmap - moved outside composable scope
    val logoBitmap = remember {
        try {
            val inputStream = context.assets.open("logo.jpg")
            val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            bitmap
        } catch (e: Exception) {
            null
        }
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(0.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(top = 40.dp, bottom = 20.dp, start = 16.dp, end = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left side - Logo and App branding
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // App Logo
                    if (logoBitmap != null) {
                        Image(
                            bitmap = logoBitmap.asImageBitmap(),
                            contentDescription = "SendRight Logo",
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                    } else {
                        // Fallback icon if logo fails to load
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(
                                    Color.Gray.copy(alpha = 0.2f), // Changed to gray for light theme
                                    RoundedCornerShape(8.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Keyboard,
                                contentDescription = "Logo",
                                tint = Color.Black,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    
                    // App name
                    Text(
                        text = "SendRight",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }
                
                // Right side - Report Button
                IconButton(
                    onClick = {
                        val intent = Intent(context, com.vishruth.key1.ui.report.ReportActivity::class.java)
                        context.startActivity(intent)
                    },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_report_black),
                        contentDescription = "Report Content",
                        tint = Color.Unspecified,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun BottomNavigationBar(
    selectedTab: AppTab,
    onTabSelected: (AppTab) -> Unit
) {
    NavigationBar(
        modifier = Modifier.fillMaxWidth(),
        containerColor = colorResource(R.color.keywise_card_background),
        tonalElevation = 8.dp
    ) {
        NavigationBarItem(
            icon = {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = "Home",
                    modifier = Modifier.size(24.dp)
                )
            },
            label = { Text("Home") },
            selected = selectedTab == AppTab.HOME,
            onClick = { onTabSelected(AppTab.HOME) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = colorResource(R.color.keywise_primary),
                selectedTextColor = colorResource(R.color.keywise_primary),
                indicatorColor = colorResource(R.color.keywise_primary).copy(alpha = 0.1f),
                unselectedIconColor = colorResource(R.color.keywise_text_secondary),
                unselectedTextColor = colorResource(R.color.keywise_text_secondary)
            )
        )
        
        NavigationBarItem(
            icon = {
                Icon(
                    imageVector = Icons.Default.Chat,
                    contentDescription = "Chat",
                    modifier = Modifier.size(24.dp)
                )
            },
            label = { Text("Chat") },
            selected = selectedTab == AppTab.CHAT,
            onClick = { onTabSelected(AppTab.CHAT) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = colorResource(R.color.keywise_primary),
                selectedTextColor = colorResource(R.color.keywise_primary),
                indicatorColor = colorResource(R.color.keywise_primary).copy(alpha = 0.1f),
                unselectedIconColor = colorResource(R.color.keywise_text_secondary),
                unselectedTextColor = colorResource(R.color.keywise_text_secondary)
            )
        )
        
        NavigationBarItem(
            icon = {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    modifier = Modifier.size(24.dp)
                )
            },
            label = { Text("Settings") },
            selected = selectedTab == AppTab.SETTINGS,
            onClick = { onTabSelected(AppTab.SETTINGS) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = colorResource(R.color.keywise_primary),
                selectedTextColor = colorResource(R.color.keywise_primary),
                indicatorColor = colorResource(R.color.keywise_primary).copy(alpha = 0.1f),
                unselectedIconColor = colorResource(R.color.keywise_text_secondary),
                unselectedTextColor = colorResource(R.color.keywise_text_secondary)
            )
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatMessageBubble(message: ChatMessage) {
    val context = LocalContext.current
    val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isFromUser) Arrangement.End else Arrangement.Start
    ) {
        Card(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .let { if (message.isFromUser) it else it.fillMaxWidth(0.85f) }
                .combinedClickable(
                    onClick = { },
                    onLongClick = {
                        val clip = ClipData.newPlainText("Chat message", message.content)
                        clipboardManager.setPrimaryClip(clip)
                        Toast.makeText(context, "Message copied!", Toast.LENGTH_SHORT).show()
                    }
                ),
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (message.isFromUser) 16.dp else 4.dp,
                bottomEnd = if (message.isFromUser) 4.dp else 16.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = if (message.isFromUser) {
                    colorResource(R.color.keywise_gradient_end)
                } else {
                    Color(0xFF2A2D3E) // Darker background for better contrast
                }
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = message.content,
                    color = Color.White,
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.Normal
                )
                
                // Copy functionality row
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Long press to copy",
                        color = Color.Gray.copy(alpha = 0.6f), // Changed to gray for light theme
                        fontSize = 10.sp,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                    
                    IconButton(
                        onClick = {
                            val clip = ClipData.newPlainText("Chat message", message.content)
                            clipboardManager.setPrimaryClip(clip)
                            Toast.makeText(context, "Copied!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = MaterialIcons.Default.ContentCopy,
                            contentDescription = "Copy message",
                            tint = Color.Gray.copy(alpha = 0.7f), // Changed to gray for light theme
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EnhancedChatMessageBubble(message: ChatMessage) {
    val context = LocalContext.current
    val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isFromUser) Arrangement.End else Arrangement.Start
    ) {
        // Add spacing for user messages to push them right
        if (message.isFromUser) {
            Spacer(modifier = Modifier.width(44.dp)) // Space to balance AI avatar
        }
        
        // Add avatar for AI messages (only for non-user messages)
        if (!message.isFromUser) {
            Card(
                modifier = Modifier
                    .size(36.dp)
                    .align(Alignment.Bottom),
                shape = CircleShape,
                colors = CardDefaults.cardColors(
                    containerColor = when (message.messageType) {
                        MessageType.SUMMARY -> Color(0xFF4A90E2)
                        MessageType.SYSTEM -> colorResource(R.color.keywise_gradient_start)
                        else -> Color(0xFF3A3A3C)
                    }
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp) // Remove gray border
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    val icon = when (message.messageType) {
                        MessageType.SUMMARY -> "S"
                        MessageType.SYSTEM -> "AI"
                        else -> "AI"
                    }
                    Text(
                        text = icon,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(8.dp))
        }
        
        Card(
            modifier = Modifier
                .widthIn(max = if (message.isFromUser) 280.dp else 320.dp)
                .let { 
                    if (message.isFromUser) {
                        it // User messages: fixed width, will be aligned right by Row arrangement
                    } else {
                        it.fillMaxWidth(0.85f) // AI messages: take most of the width
                    }
                }
                .combinedClickable(
                    onClick = { },
                    onLongClick = {
                        val clip = ClipData.newPlainText("Chat message", message.content)
                        clipboardManager.setPrimaryClip(clip)
                        Toast.makeText(context, "Message copied!", Toast.LENGTH_SHORT).show()
                    }
                ),
            shape = RoundedCornerShape(
                topStart = if (message.isFromUser) 20.dp else 6.dp,
                topEnd = if (message.isFromUser) 6.dp else 20.dp,
                bottomStart = 20.dp,
                bottomEnd = 20.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = when {
                    message.isFromUser -> Color.Gray.copy(alpha = 0.2f) // Improved transparent light gray for user messages
                    message.messageType == MessageType.SUMMARY -> colorResource(R.color.keywise_primary).copy(alpha = 0.15f) // Light green for summaries
                    message.messageType == MessageType.SYSTEM -> Color(0xFFFF9800).copy(alpha = 0.15f) // Light orange for system
                    else -> Color.Gray.copy(alpha = 0.12f) // Improved transparent light gray for AI messages
                }
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 0.dp // Remove gray border by eliminating elevation
            )
        ) {
            Column(
                modifier = Modifier.padding(
                    horizontal = 16.dp,
                    vertical = 12.dp
                )
            ) {
                // Message type indicator for special messages
                if (message.messageType != MessageType.TEXT) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        val (icon, label, color) = when (message.messageType) {
                            MessageType.SUMMARY -> Triple("S", "Summary", Color(0xFF4A90E2))
                            MessageType.SYSTEM -> Triple("SYS", "System", Color(0xFF888888))
                            else -> Triple("MSG", "Message", Color.White)
                        }
                        
                        Text(
                            text = icon,
                            fontSize = 12.sp
                        )
                        Text(
                            text = label,
                            color = color,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                
                Text(
                    text = message.content,
                    color = Color.Black, // Changed to black for better readability
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                    fontWeight = when {
                        message.messageType == MessageType.SUMMARY -> FontWeight.Medium
                        message.isFromUser -> FontWeight.Normal
                        else -> FontWeight.Normal
                    },
                    textAlign = if (message.isFromUser) androidx.compose.ui.text.style.TextAlign.End else androidx.compose.ui.text.style.TextAlign.Start,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (message.isFromUser) Arrangement.End else Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Show "AI" label only for AI messages on the left
                    if (!message.isFromUser && message.messageType == MessageType.TEXT) {
                        Text(
                            text = "AI",
                            color = Color.Gray, // Changed to gray for better contrast
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    
                    // Show "You" label for user messages on the right
                    if (message.isFromUser) {
                        Text(
                            text = "You",
                            color = Color.Gray.copy(alpha = 0.6f), // Changed to gray for light theme
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    
                    Text(
                        text = SimpleDateFormat("HH:mm", Locale.getDefault())
                            .format(Date(message.timestamp)),
                        color = Color.Gray.copy(alpha = 0.7f), // Changed to gray for light theme
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
fun ChatLoadingBubble() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        // AI Avatar
        Card(
            modifier = Modifier
                .size(36.dp)
                .align(Alignment.Bottom),
            shape = CircleShape,
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF3A3A3C)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "AI",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Card(
            modifier = Modifier.widthIn(max = 120.dp),
            shape = RoundedCornerShape(
                topStart = 6.dp,
                topEnd = 20.dp,
                bottomStart = 20.dp,
                bottomEnd = 20.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF2C2C2E) // Matching enhanced background
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(3) { index ->
                    val alpha by animateFloatAsState(
                        targetValue = if ((System.currentTimeMillis() / 500) % 3 == index.toLong()) 1f else 0.4f,
                        animationSpec = tween(500)
                    )
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(
                                Color.Gray.copy(alpha = alpha), // Changed to gray for light theme
                                CircleShape
                            )
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatInputArea(
    message: String,
    onMessageChange: (String) -> Unit,
    isLoading: Boolean,
    onSendMessage: () -> Unit,
    onNewChat: () -> Unit = {},
    onSummarize: () -> Unit = {},
    showExtraActions: Boolean = false
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White // Changed to white to match main background
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column {
            // Extra actions row (minimalistic)
            if (showExtraActions) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // New Chat button
                    OutlinedButton(
                        onClick = onNewChat,
                        modifier = Modifier.height(36.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.Gray // Changed to gray for white background
                        ),
                        border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.3f)), // Changed border color
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Icon(
                            imageVector = MaterialIcons.Default.Add,
                            contentDescription = "New Chat",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "New",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    
                    // Summarize button
                    OutlinedButton(
                        onClick = onSummarize,
                        modifier = Modifier.height(36.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.Gray // Changed to gray for white background
                        ),
                        border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.3f)), // Changed border color
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Icon(
                            imageVector = MaterialIcons.Default.List,
                            contentDescription = "Summarize",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Summary",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                
                Divider(
                    color = Color.White.copy(alpha = 0.1f),
                    thickness = 0.5.dp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
            
            // Main input area with improved styling
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .background(
                    Color.White, // Clean white background
                    RoundedCornerShape(28.dp)
                )
                .padding(6.dp), // Inner padding for the background
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Enhanced text input with minimalistic design
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White // White background as requested
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            OutlinedTextField(
                value = message,
                onValueChange = onMessageChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp),
                placeholder = {
                    Text(
                                "Message...",
                                color = Color.Black.copy(alpha = 0.6f), // Black placeholder text as requested
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Normal
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.Black, // Black text for contrast
                    unfocusedTextColor = Color.Black, // Black text for contrast
                            focusedBorderColor = Color.Black.copy(alpha = 0.8f), // Black outline as requested
                            unfocusedBorderColor = Color.Black.copy(alpha = 0.5f), // Black outline when unfocused
                    cursorColor = colorResource(R.color.keywise_gradient_start),
                    focusedContainerColor = Color.White, // White background
                    unfocusedContainerColor = Color.White // White background
                ),
                keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Send,
                            capitalization = KeyboardCapitalization.Sentences
                ),
                keyboardActions = KeyboardActions(
                    onSend = { onSendMessage() }
                ),
                maxLines = 4,
                        shape = RoundedCornerShape(20.dp),
                textStyle = androidx.compose.ui.text.TextStyle(
                            fontSize = 16.sp,
                            lineHeight = 22.sp,
                            fontWeight = FontWeight.Normal
                )
            )
                }
            
                // Minimalistic send button
                Box(
                modifier = Modifier.size(48.dp),
                    contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = colorResource(R.color.keywise_gradient_start),
                        strokeWidth = 2.dp
                    )
                } else {
                        Card(
                            modifier = Modifier.size(48.dp), // Slightly larger for better touch target
                            shape = CircleShape,
                            colors = CardDefaults.cardColors(
                                containerColor = if (message.trim().isNotEmpty()) {
                                    colorResource(R.color.keywise_primary) // Use primary green color
                                } else {
                                    Color.Gray.copy(alpha = 0.3f) // Lighter disabled state
                                }
                            ),
                            elevation = CardDefaults.cardElevation(
                                defaultElevation = 0.dp // Remove gray border by eliminating elevation
                            )
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                    Icon(
                                    imageVector = MaterialIcons.Default.Send,
                        contentDescription = "Send",
                        tint = if (message.trim().isNotEmpty()) Color.White else Color.Gray, // Dynamic icon color
                                    modifier = Modifier
                                        .size(22.dp) // Slightly larger icon
                                        .clickable(
                                            enabled = message.trim().isNotEmpty() && !isLoading
                                        ) { onSendMessage() }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ModernHeader() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(dimensionResource(R.dimen.card_corner_radius)),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            colorResource(R.color.keywise_gradient_start),
                            colorResource(R.color.keywise_gradient_end)
                        )
                    ),
                    shape = RoundedCornerShape(dimensionResource(R.dimen.card_corner_radius))
                )
                .padding(dimensionResource(R.dimen.card_padding))
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // App Icon
                Box(
                    modifier = Modifier
                        .size(dimensionResource(R.dimen.extra_large_icon_size))
                        .background(
                            Color.White.copy(alpha = 0.2f),
                            RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🧠",
                        fontSize = 32.sp
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "SendRight",
                    fontSize = dimensionResource(R.dimen.header_font_size).value.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                
                Text(
                    text = "Powered by Neonix AI",
                    fontSize = dimensionResource(R.dimen.subtitle_font_size).value.sp,
                    color = Color.White.copy(alpha = 0.9f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp)
                )
                
                Text(
                    text = "The AI Keyboard That Thinks With You",
                    fontSize = dimensionResource(R.dimen.body_font_size).value.sp,
                    color = Color.White.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

@Composable
fun ProgressIndicator(isKeyboardEnabled: Boolean, isKeyboardSelected: Boolean) {
    val progress = when {
        isKeyboardEnabled && isKeyboardSelected -> 1f
        isKeyboardEnabled -> 0.5f
        else -> 0f
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(dimensionResource(R.dimen.card_corner_radius)),
        colors = CardDefaults.cardColors(containerColor = colorResource(R.color.keywise_card_background)),
        elevation = CardDefaults.cardElevation(defaultElevation = dimensionResource(R.dimen.elevation_card))
    ) {
        Column(
            modifier = Modifier.padding(dimensionResource(R.dimen.card_padding))
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Setup Progress",
                    fontSize = dimensionResource(R.dimen.title_font_size).value.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorResource(R.color.keywise_text_primary)
                )
                
                Text(
                    text = "${(progress * 100).toInt()}%",
                    fontSize = dimensionResource(R.dimen.body_font_size).value.sp,
                    fontWeight = FontWeight.Medium,
                    color = colorResource(R.color.keywise_primary)
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = colorResource(R.color.keywise_primary),
                trackColor = colorResource(R.color.keywise_border)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = when {
                    isKeyboardEnabled && isKeyboardSelected -> "🎉 Setup Complete! Ready to use SendRight AI"
                    isKeyboardEnabled -> "Almost there! Select SendRight as your default keyboard"
                    else -> "Let's get started with the setup"
                },
                fontSize = dimensionResource(R.dimen.small_font_size).value.sp,
                color = colorResource(R.color.keywise_text_secondary)
            )
        }
    }
}

@Composable
fun EnhancedSetupStepCard(
    stepNumber: Int,
    title: String,
    description: String,
    isCompleted: Boolean,
    action: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(dimensionResource(R.dimen.card_corner_radius)),
        colors = CardDefaults.cardColors(
            containerColor = if (isCompleted) 
                colorResource(R.color.keywise_success_light) 
            else 
                colorResource(R.color.keywise_card_background)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = dimensionResource(R.dimen.elevation_card)),
        border = if (!isCompleted) androidx.compose.foundation.BorderStroke(
            1.dp, 
            colorResource(R.color.keywise_border)
        ) else null
    ) {
        Column(
            modifier = Modifier.padding(dimensionResource(R.dimen.card_padding))
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Enhanced Step Indicator
                Box(
                    modifier = Modifier.size(dimensionResource(R.dimen.step_indicator_size)),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        shape = androidx.compose.foundation.shape.CircleShape,
                        colors = CardDefaults.cardColors(
                            containerColor = if (isCompleted) 
                                colorResource(R.color.keywise_success) 
                            else 
                                colorResource(R.color.keywise_primary)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = dimensionResource(R.dimen.elevation_button))
                    ) {
                        Box(
                            modifier = Modifier.size(dimensionResource(R.dimen.step_indicator_size)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isCompleted) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(dimensionResource(R.dimen.icon_size))
                                )
                            } else {
                                Text(
                                    text = stepNumber.toString(),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = dimensionResource(R.dimen.title_font_size).value.sp
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.width(dimensionResource(R.dimen.button_spacing)))
                
                // Content
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = title,
                        fontSize = dimensionResource(R.dimen.title_font_size).value.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorResource(R.color.keywise_text_primary)
                    )
                    Text(
                        text = description,
                        fontSize = dimensionResource(R.dimen.body_font_size).value.sp,
                        color = colorResource(R.color.keywise_text_secondary),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.button_spacing)))
            
            // Action button with full width
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.CenterEnd
            ) {
                action()
            }
        }
    }
}

@Composable
fun ModernActionButton(
    text: String,
    isCompleted: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .height(dimensionResource(R.dimen.button_height))
            .widthIn(min = dimensionResource(R.dimen.button_min_width)),
        shape = RoundedCornerShape(dimensionResource(R.dimen.button_corner_radius)),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isCompleted) 
                colorResource(R.color.keywise_success) 
            else 
                Color.White
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = dimensionResource(R.dimen.elevation_button))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = if (isCompleted) Icons.Default.Check else Icons.Default.ArrowForward,
                contentDescription = null,
                modifier = Modifier.size(dimensionResource(R.dimen.icon_size)),
                tint = if (isCompleted) Color.White else Color(0xFF4CAF50)
            )
            Spacer(modifier = Modifier.width(dimensionResource(R.dimen.button_spacing)))
            Text(
                text = text,
                fontSize = dimensionResource(R.dimen.body_font_size).value.sp,
                fontWeight = FontWeight.Medium,
                color = if (isCompleted) Color.White else Color(0xFF4CAF50)
            )
        }
    }
}

@Composable
fun AIFeaturesCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(dimensionResource(R.dimen.card_corner_radius)),
        colors = CardDefaults.cardColors(containerColor = colorResource(R.color.keywise_card_background)),
        elevation = CardDefaults.cardElevation(defaultElevation = dimensionResource(R.dimen.elevation_card))
    ) {
        Column(
            modifier = Modifier.padding(dimensionResource(R.dimen.card_padding))
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = colorResource(R.color.keywise_primary),
                    modifier = Modifier.size(dimensionResource(R.dimen.large_icon_size))
                )
                Spacer(modifier = Modifier.width(dimensionResource(R.dimen.button_spacing)))
                Text(
                    text = "Neonix AI Features",
                    fontSize = dimensionResource(R.dimen.title_font_size).value.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorResource(R.color.keywise_text_primary)
                )
            }
            
            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.feature_item_spacing)))
            
            val features = listOf(
                Pair("✍️", "Smart Rewrite - Improve your text instantly"),
                Pair("📚", "Summarize - Condense long messages"),
                Pair("🧠", "Explain - Simplify complex topics"),
                Pair("🗒️", "Listify - Convert text to bullet points"),
                Pair("💬", "Emoji-fy - Add fun emojis"),
                Pair("📢", "Make Formal - Professional tone"),
                Pair("🐦", "Tweetify - Shorten to tweet length"),
                Pair("⚡", "Prompt-fy - Create AI prompts"),
                Pair("🌍", "Translate - Multi-language support"),
                Pair("🎨", "Creative Writing - Enhanced by Neonix AI")
            )
            
            features.chunked(2).forEach { rowFeatures ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.button_spacing))
                ) {
                    rowFeatures.forEach { (emoji, feature) ->
                        FeatureItem(
                            emoji = emoji,
                            text = feature,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (rowFeatures.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
                Spacer(modifier = Modifier.height(dimensionResource(R.dimen.button_spacing)))
            }
        }
    }
}

@Composable
fun FeatureItem(
    emoji: String,
    text: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = emoji,
            fontSize = dimensionResource(R.dimen.body_font_size).value.sp
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text.substringAfter(" - "),
            fontSize = dimensionResource(R.dimen.small_font_size).value.sp,
            color = colorResource(R.color.keywise_text_secondary),
            maxLines = 1
        )
    }
}

@Composable
fun AIModelSelectionCard(context: Context) {
    val isGPT5Enabled = remember { 
        mutableStateOf(getIsGPT5Enabled(context))
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(dimensionResource(R.dimen.card_corner_radius)),
        colors = CardDefaults.cardColors(containerColor = colorResource(R.color.keywise_card_background)),
        elevation = CardDefaults.cardElevation(defaultElevation = dimensionResource(R.dimen.elevation_card))
    ) {
        Column(
            modifier = Modifier.padding(dimensionResource(R.dimen.card_padding))
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Psychology,
                    contentDescription = "AI Model",
                    tint = colorResource(R.color.keywise_primary),
                    modifier = Modifier.size(24.dp)
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "AI Model Selection",
                        fontSize = dimensionResource(R.dimen.title_font_size).value.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorResource(R.color.keywise_text_primary)
                    )
                    Text(
                        text = "Toggle to switch between ChatGPT-5 and Gemini for AI actions & chat",
                        fontSize = dimensionResource(R.dimen.body_font_size).value.sp,
                        color = colorResource(R.color.keywise_text_secondary)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Model selection toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (isGPT5Enabled.value) "ChatGPT-5 (AI/ML API)" else "Gemini (Google AI)",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = colorResource(R.color.keywise_text_primary)
                    )
                    Text(
                        text = if (isGPT5Enabled.value) "Advanced reasoning & latest features" else "Fast & reliable responses",
                        fontSize = 14.sp,
                        color = colorResource(R.color.keywise_text_secondary)
                    )
                }
                
                Switch(
                    checked = isGPT5Enabled.value,
                    onCheckedChange = { enabled ->
                        isGPT5Enabled.value = enabled
                        setIsGPT5Enabled(context, enabled)
                        
                        // Show toast to inform user about the change
                        val modelName = if (enabled) "ChatGPT-5" else "Gemini"
                        Toast.makeText(context, "Switched to $modelName for AI actions & chat", Toast.LENGTH_SHORT).show()
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = colorResource(R.color.keywise_primary),
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = Color.Gray.copy(alpha = 0.5f)
                    )
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Model information card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isGPT5Enabled.value) 
                        colorResource(R.color.keywise_primary).copy(alpha = 0.1f)
                    else 
                        Color.Blue.copy(alpha = 0.1f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isGPT5Enabled.value) Icons.Default.Star else Icons.Default.Speed,
                        contentDescription = null,
                        tint = if (isGPT5Enabled.value) 
                            colorResource(R.color.keywise_primary)
                        else 
                            Color.Blue,
                        modifier = Modifier.size(20.dp)
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Text(
                        text = if (isGPT5Enabled.value) 
                            "Using latest ChatGPT-5 model for enhanced AI actions & chat capabilities"
                        else 
                            "Using Google Gemini for fast and reliable AI actions & chat responses",
                        fontSize = 13.sp,
                        color = colorResource(R.color.keywise_text_primary),
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

@Composable
fun AIResponseSettingsCard(context: Context) {
    val responseMode = remember { 
        mutableStateOf(getResponseMode(context))
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(dimensionResource(R.dimen.card_corner_radius)),
        colors = CardDefaults.cardColors(containerColor = colorResource(R.color.keywise_card_background)),
        elevation = CardDefaults.cardElevation(defaultElevation = dimensionResource(R.dimen.elevation_card))
    ) {
        Column(
            modifier = Modifier.padding(dimensionResource(R.dimen.card_padding))
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    tint = colorResource(R.color.keywise_primary),
                    modifier = Modifier.size(dimensionResource(R.dimen.large_icon_size))
                )
                Spacer(modifier = Modifier.width(dimensionResource(R.dimen.button_spacing)))
                Text(
                    text = "AI Response Settings",
                    fontSize = dimensionResource(R.dimen.title_font_size).value.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorResource(R.color.keywise_text_primary)
                )
            }
            
            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.feature_item_spacing)))
            
            // Response Mode Options - Vertical Layout
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Normal Mode (Default)
                ResponseModeRadioOption(
                    title = "Normal",
                    isSelected = responseMode.value == ResponseMode.NORMAL,
                    onClick = {
                        responseMode.value = ResponseMode.NORMAL
                        setResponseMode(context, ResponseMode.NORMAL)
                    }
                )
                
                // Detailed Mode
                ResponseModeRadioOption(
                    title = "Detailed",
                    isSelected = responseMode.value == ResponseMode.CONCISE,
                    onClick = {
                        responseMode.value = ResponseMode.CONCISE
                        setResponseMode(context, ResponseMode.CONCISE)
                    }
                )
            }
        }
    }
}

@Composable
fun ResponseModeRadioOption(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(
                selectedColor = colorResource(R.color.keywise_primary),
                unselectedColor = colorResource(R.color.keywise_text_secondary)
            )
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Text(
            text = title,
            fontSize = dimensionResource(R.dimen.body_font_size).value.sp,
            fontWeight = FontWeight.Medium,
            color = colorResource(R.color.keywise_text_primary)
        )
    }
}

@Composable
fun UsageInfoCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(dimensionResource(R.dimen.card_corner_radius)),
        colors = CardDefaults.cardColors(containerColor = colorResource(R.color.keywise_success_light)),
        elevation = CardDefaults.cardElevation(defaultElevation = dimensionResource(R.dimen.elevation_card))
    ) {
        Row(
            modifier = Modifier.padding(dimensionResource(R.dimen.card_padding)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = colorResource(R.color.keywise_success),
                modifier = Modifier.size(dimensionResource(R.dimen.large_icon_size))
            )
            
            Spacer(modifier = Modifier.width(dimensionResource(R.dimen.button_spacing)))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Unlimited Free Usage",
                    fontWeight = FontWeight.Bold,
                    color = colorResource(R.color.keywise_success),
                    fontSize = dimensionResource(R.dimen.title_font_size).value.sp
                )
                Text(
                    text = "Unlimited Neonix AI actions - Completely Free!",
                    fontSize = dimensionResource(R.dimen.body_font_size).value.sp,
                    color = colorResource(R.color.keywise_text_secondary),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
fun ReadyToUseCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(dimensionResource(R.dimen.card_corner_radius)),
        colors = CardDefaults.cardColors(containerColor = colorResource(R.color.keywise_primary)),
        elevation = CardDefaults.cardElevation(defaultElevation = dimensionResource(R.dimen.elevation_card))
    ) {
        Column(
            modifier = Modifier.padding(dimensionResource(R.dimen.card_padding)),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Celebration,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(dimensionResource(R.dimen.extra_large_icon_size))
            )
            
            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.button_spacing)))
            
            Text(
                text = "🎉 Ready to Use!",
                fontSize = dimensionResource(R.dimen.title_font_size).value.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            
            Text(
                text = "SendRight AI is now active. Open any app and start typing to experience the power of Neonix AI!",
                fontSize = dimensionResource(R.dimen.body_font_size).value.sp,
                color = Color.White.copy(alpha = 0.9f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
fun SafetyRestartCard(onRestartCompleted: () -> Unit = {}) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(dimensionResource(R.dimen.card_corner_radius)),
        colors = CardDefaults.cardColors(containerColor = colorResource(R.color.keywise_card_background)),
        elevation = CardDefaults.cardElevation(defaultElevation = dimensionResource(R.dimen.elevation_card)),
        border = androidx.compose.foundation.BorderStroke(2.dp, colorResource(R.color.keywise_primary))
    ) {
        Column(
            modifier = Modifier.padding(dimensionResource(R.dimen.card_padding))
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = colorResource(R.color.keywise_primary),
                    modifier = Modifier.size(dimensionResource(R.dimen.large_icon_size))
                )
                Spacer(modifier = Modifier.width(dimensionResource(R.dimen.button_spacing)))
                Text(
                    text = "🔒 Important Safety Steps",
                    fontSize = dimensionResource(R.dimen.title_font_size).value.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorResource(R.color.keywise_text_primary)
                )
            }
            
            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.feature_item_spacing)))
            
            // Restart Instructions
            SafetyStepItem(
                stepNumber = "1",
                title = "Restart Your Phone (Recommended)",
                description = "For optimal performance and to ensure all keyboard features work properly, restart your device now.",
                isImportant = true
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            SafetyStepItem(
                stepNumber = "2", 
                title = "Test in Safe Apps First",
                description = "Try SendRight in Notes, Messages, or Email apps before using in sensitive applications.",
                isImportant = false
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            SafetyStepItem(
                stepNumber = "3",
                title = "Keep Default Keyboard Available", 
                description = "Always keep your original keyboard enabled as a backup option in device settings.",
                isImportant = false
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Restart Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                                 Button(
                     onClick = {
                         // Mark restart as completed and hide this card
                         onRestartCompleted()
                     },
                    modifier = Modifier.height(dimensionResource(R.dimen.button_height)),
                    shape = RoundedCornerShape(dimensionResource(R.dimen.button_corner_radius)),
                    colors = ButtonDefaults.buttonColors(containerColor = colorResource(R.color.keywise_primary)),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = dimensionResource(R.dimen.elevation_button))
                ) {
                    Icon(
                        imageVector = Icons.Default.RestartAlt,
                        contentDescription = null,
                        modifier = Modifier.size(dimensionResource(R.dimen.icon_size)),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(dimensionResource(R.dimen.button_spacing)))
                                         Text(
                         text = "I've Restarted My Device",
                         fontSize = dimensionResource(R.dimen.body_font_size).value.sp,
                         fontWeight = FontWeight.Medium,
                         color = Color.White
                     )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "💡 Hold Power + Volume Down buttons to restart most Android devices",
                fontSize = dimensionResource(R.dimen.small_font_size).value.sp,
                color = colorResource(R.color.keywise_text_secondary),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun GeneralSafetyCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(dimensionResource(R.dimen.card_corner_radius)),
        colors = CardDefaults.cardColors(containerColor = colorResource(R.color.keywise_card_background)),
        elevation = CardDefaults.cardElevation(defaultElevation = dimensionResource(R.dimen.elevation_card)),
        border = androidx.compose.foundation.BorderStroke(1.dp, colorResource(R.color.keywise_border))
    ) {
        Column(
            modifier = Modifier.padding(dimensionResource(R.dimen.card_padding))
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = colorResource(R.color.keywise_primary),
                    modifier = Modifier.size(dimensionResource(R.dimen.large_icon_size))
                )
                Spacer(modifier = Modifier.width(dimensionResource(R.dimen.button_spacing)))
                Text(
                    text = "Safety Information",
                    fontSize = dimensionResource(R.dimen.title_font_size).value.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorResource(R.color.keywise_text_primary)
                )
            }
            
            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.feature_item_spacing)))
            
            val safetyTips = listOf(
                "Your data stays private - SendRight processes text locally when possible",
                "Keep your original keyboard enabled as backup",
                "Restart your device after setup for best performance", 
                "Test in safe apps (Notes, Messages) before sensitive use",
                "You can disable SendRight anytime in Settings > Languages & Input"
            )
            
            safetyTips.forEach { tip ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = tip.substring(0, 2), // Emoji
                        fontSize = dimensionResource(R.dimen.body_font_size).value.sp,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text = tip.substring(3), // Rest of text
                        fontSize = dimensionResource(R.dimen.small_font_size).value.sp,
                        color = colorResource(R.color.keywise_text_secondary),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun SafetyStepItem(
    stepNumber: String,
    title: String,
    description: String,
    isImportant: Boolean
) {
    Row(
        verticalAlignment = Alignment.Top
    ) {
        // Step number indicator
        Box(
            modifier = Modifier.size(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = androidx.compose.foundation.shape.CircleShape,
                colors = CardDefaults.cardColors(
                    containerColor = if (isImportant) 
                        colorResource(R.color.keywise_primary) 
                    else 
                        colorResource(R.color.keywise_border)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Box(
                    modifier = Modifier.size(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stepNumber,
                        color = if (isImportant) Color.White else colorResource(R.color.keywise_text_primary),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // Content
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                fontSize = dimensionResource(R.dimen.body_font_size).value.sp,
                fontWeight = if (isImportant) FontWeight.Bold else FontWeight.Medium,
                color = if (isImportant) colorResource(R.color.keywise_primary) else colorResource(R.color.keywise_text_primary)
            )
            Text(
                text = description,
                fontSize = dimensionResource(R.dimen.small_font_size).value.sp,
                color = colorResource(R.color.keywise_text_secondary),
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

fun isKeyboardEnabled(context: Context): Boolean {
    val inputMethodManager = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    val enabledInputMethods = inputMethodManager.enabledInputMethodList
    return enabledInputMethods.any { 
        it.packageName == context.packageName 
    }
}

fun isKeyboardSelected(context: Context): Boolean {
    val inputMethodManager = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    val currentInputMethod = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.DEFAULT_INPUT_METHOD
    )
    return currentInputMethod?.contains(context.packageName) == true
}

fun hasUserRestartedAfterSetup(context: Context): Boolean {
    val prefs = context.getSharedPreferences("sendright_setup", Context.MODE_PRIVATE)
    val setupCompleted = isKeyboardEnabled(context) && isKeyboardSelected(context)
    
    if (!setupCompleted) {
        // If setup isn't complete, no need to track restart
        return false
    }
    
    // Check if restart has been marked as completed
    val restartCompleted = prefs.getBoolean("restart_completed", false)
    
    // Also check device uptime - if it's been restarted recently, consider it done
    val lastSetupTime = prefs.getLong("setup_completion_time", 0)
    val currentTime = System.currentTimeMillis()
    val deviceUptime = android.os.SystemClock.elapsedRealtime()
    
    // If device uptime is less than time since setup completion, device was restarted
    val wasDeviceRestarted = deviceUptime < (currentTime - lastSetupTime)
    
    if (setupCompleted && lastSetupTime == 0L) {
        // First time setup is complete, record the time
        prefs.edit().putLong("setup_completion_time", currentTime).apply()
        return false
    }
    
    if (wasDeviceRestarted && !restartCompleted) {
        // Device was restarted, mark it as completed automatically
        prefs.edit().putBoolean("restart_completed", true).apply()
        return true
    }
    
    return restartCompleted
}

fun markRestartCompleted(context: Context) {
    val prefs = context.getSharedPreferences("sendright_setup", Context.MODE_PRIVATE)
    prefs.edit().putBoolean("restart_completed", true).apply()
}

enum class ResponseMode(val displayName: String, val value: String) {
    NORMAL("Normal", "normal"),
    CONCISE("Detailed", "concise")
}

fun getResponseMode(context: Context): ResponseMode {
    val prefs = context.getSharedPreferences("sendright_settings", Context.MODE_PRIVATE)
    val modeValue = prefs.getString("response_mode", ResponseMode.NORMAL.value)
    return ResponseMode.values().find { it.value == modeValue } ?: ResponseMode.NORMAL
}

fun setResponseMode(context: Context, mode: ResponseMode) {
    val prefs = context.getSharedPreferences("sendright_settings", Context.MODE_PRIVATE)
    prefs.edit().putString("response_mode", mode.value).apply()
}

// GPT-5 Model Selection Functions
fun getIsGPT5Enabled(context: Context): Boolean {
    val prefs = context.getSharedPreferences("sendright_settings", Context.MODE_PRIVATE)
    return prefs.getBoolean("is_gpt5_enabled", false) // Default to Gemini (false)
}

fun setIsGPT5Enabled(context: Context, enabled: Boolean) {
    val prefs = context.getSharedPreferences("sendright_settings", Context.MODE_PRIVATE)
    prefs.edit().putBoolean("is_gpt5_enabled", enabled).apply()
    android.util.Log.d("AIModelSelection", "AI model changed to: ${if (enabled) "ChatGPT-5" else "Gemini"}")
}

// Theme Management Functions
fun getSelectedTheme(context: Context): String {
    val prefs = context.getSharedPreferences("sendright_settings", Context.MODE_PRIVATE)
    return prefs.getString("keyboard_theme", "white") ?: "white"
}

fun setSelectedTheme(context: Context, themeId: String) {
    val prefs = context.getSharedPreferences("sendright_settings", Context.MODE_PRIVATE)
    prefs.edit().putString("keyboard_theme", themeId).apply()
    
    // Log the theme change for debugging
    android.util.Log.d("ThemeSelection", "Theme changed to: $themeId")
}

// Key Background Style Management Functions
fun getSelectedKeyBackgroundStyle(context: Context): String {
    val prefs = context.getSharedPreferences("sendright_settings", Context.MODE_PRIVATE)
    return prefs.getString("key_background_style", "light_white") ?: "light_white"
}

fun setSelectedKeyBackgroundStyle(context: Context, styleId: String) {
    val prefs = context.getSharedPreferences("sendright_settings", Context.MODE_PRIVATE)
    prefs.edit().putString("key_background_style", styleId).apply()
    
    // Log the style change for debugging
    android.util.Log.d("KeyBackgroundStyle", "Key background style changed to: $styleId")
}

@Composable
fun CurlyBraceInstructionsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(dimensionResource(R.dimen.card_corner_radius)),
        colors = CardDefaults.cardColors(containerColor = colorResource(R.color.keywise_card_background)),
        elevation = CardDefaults.cardElevation(defaultElevation = dimensionResource(R.dimen.elevation_card))
    ) {
        Column(
            modifier = Modifier.padding(dimensionResource(R.dimen.card_padding))
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🎯",
                    fontSize = dimensionResource(R.dimen.large_icon_size).value.sp,
                    modifier = Modifier.padding(end = dimensionResource(R.dimen.button_spacing))
                )
                Text(
                    text = "Curly Brace Instructions",
                    fontSize = dimensionResource(R.dimen.title_font_size).value.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorResource(R.color.keywise_text_primary)
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = "Add custom instructions directly in your text using curly braces {}",
                fontSize = dimensionResource(R.dimen.body_font_size).value.sp,
                color = colorResource(R.color.keywise_text_secondary),
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            val examples = listOf(
                "\"Nucleus {Short explanation under 200 letters}\"",
                "\"Climate change {focus on solutions} {keep under 100 words}\"",
                "\"Story about cats {make it funny with dialogue}\""
            )
            
            examples.forEach { example ->
                Row(
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Text(
                        text = "• ",
                        fontSize = dimensionResource(R.dimen.body_font_size).value.sp,
                        color = colorResource(R.color.keywise_primary)
                    )
                    Text(
                        text = example,
                        fontSize = dimensionResource(R.dimen.small_font_size).value.sp,
                        color = colorResource(R.color.keywise_text_secondary),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun KeyboardThemesFeatureCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(dimensionResource(R.dimen.card_corner_radius)),
        colors = CardDefaults.cardColors(containerColor = colorResource(R.color.keywise_card_background)),
        elevation = CardDefaults.cardElevation(defaultElevation = dimensionResource(R.dimen.elevation_card))
    ) {
        Column(
            modifier = Modifier.padding(dimensionResource(R.dimen.card_padding))
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🎨",
                    fontSize = dimensionResource(R.dimen.large_icon_size).value.sp,
                    modifier = Modifier.padding(end = dimensionResource(R.dimen.button_spacing))
                )
                Text(
                    text = "Keyboard Themes",
                    fontSize = dimensionResource(R.dimen.title_font_size).value.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorResource(R.color.keywise_text_primary)
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = "Personalize your typing experience with beautiful gradient themes",
                fontSize = dimensionResource(R.dimen.body_font_size).value.sp,
                color = colorResource(R.color.keywise_text_secondary),
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            val themes = listOf(
                "Ocean Blue - Deep sea gradients",
                "Sunset Orange - Warm evening colors",
                "Forest Green - Nature-inspired tones",
                "Royal Purple - Elegant sophistication",
                "Fire Red - Bold and energetic",
                "Gold Amber - Luxurious metallics",
                "Cyber Pink - Modern neon vibes",
                "Arctic Blue - Cool refreshing shades",
                "Cosmic Purple - Space-age aesthetics",
                "Mint Green - Fresh and vibrant"
            )
            
            themes.take(5).forEach { theme ->
                Row(
                    modifier = Modifier.padding(vertical = 2.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = theme,
                        fontSize = dimensionResource(R.dimen.small_font_size).value.sp,
                        color = colorResource(R.color.keywise_text_secondary)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "💡 Change themes instantly in Settings → Keyboard Themes",
                fontSize = dimensionResource(R.dimen.small_font_size).value.sp,
                color = colorResource(R.color.keywise_primary),
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun AIProcessingLogicCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(dimensionResource(R.dimen.card_corner_radius)),
        colors = CardDefaults.cardColors(containerColor = colorResource(R.color.keywise_card_background)),
        elevation = CardDefaults.cardElevation(defaultElevation = dimensionResource(R.dimen.elevation_card))
    ) {
        Column(
            modifier = Modifier.padding(dimensionResource(R.dimen.card_padding))
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Psychology,
                    contentDescription = null,
                    tint = colorResource(R.color.keywise_primary),
                    modifier = Modifier.size(dimensionResource(R.dimen.large_icon_size))
                )
                Spacer(modifier = Modifier.width(dimensionResource(R.dimen.button_spacing)))
                Text(
                    text = "AI Processing Logic",
                    fontSize = dimensionResource(R.dimen.title_font_size).value.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorResource(R.color.keywise_text_primary)
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            val steps = listOf(
                "1. Text Selection → Smart context detection",
                "2. Action Selection → Choose AI operation",
                "3. Prompt Enhancement → Add custom instructions",
                "4. AI Processing → Gemini generation",
                "5. Result Cleaning → Remove unwanted prefixes",
                "6. Text Replacement → Apply changes seamlessly"
            )
            
            steps.forEach { step ->
                Row(
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Text(
                        text = step,
                        fontSize = dimensionResource(R.dimen.small_font_size).value.sp,
                        color = colorResource(R.color.keywise_text_secondary)
                    )
                }
            }
        }
    }
}

@Composable
fun CurlyBraceLogicCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(dimensionResource(R.dimen.card_corner_radius)),
        colors = CardDefaults.cardColors(containerColor = colorResource(R.color.keywise_card_background)),
        elevation = CardDefaults.cardElevation(defaultElevation = dimensionResource(R.dimen.elevation_card))
    ) {
        Column(
            modifier = Modifier.padding(dimensionResource(R.dimen.card_padding))
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🎯",
                    fontSize = dimensionResource(R.dimen.large_icon_size).value.sp,
                    modifier = Modifier.padding(end = dimensionResource(R.dimen.button_spacing))
                )
                Text(
                    text = "Curly Brace Processing",
                    fontSize = dimensionResource(R.dimen.title_font_size).value.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorResource(R.color.keywise_text_primary)
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = "How custom instructions are parsed and applied:",
                fontSize = dimensionResource(R.dimen.body_font_size).value.sp,
                color = colorResource(R.color.keywise_text_secondary),
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            val logic = listOf(
                "1. Regex Detection → Find all {instruction} patterns",
                "2. Text Extraction → Separate content from instructions",
                "3. Instruction Combination → Merge multiple instructions",
                "4. Prompt Enhancement → Add to base AI prompt",
                "5. Priority Handling → Custom instructions override defaults"
            )
            
            logic.forEach { step ->
                Row(
                    modifier = Modifier.padding(vertical = 3.dp)
                ) {
                    Text(
                        text = step,
                        fontSize = dimensionResource(R.dimen.small_font_size).value.sp,
                        color = colorResource(R.color.keywise_text_secondary)
                    )
                }
            }
        }
    }
}

@Composable
fun ResponseModeLogicCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(dimensionResource(R.dimen.card_corner_radius)),
        colors = CardDefaults.cardColors(containerColor = colorResource(R.color.keywise_card_background)),
        elevation = CardDefaults.cardElevation(defaultElevation = dimensionResource(R.dimen.elevation_card))
    ) {
        Column(
            modifier = Modifier.padding(dimensionResource(R.dimen.card_padding))
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "⚙️",
                    fontSize = dimensionResource(R.dimen.large_icon_size).value.sp,
                    modifier = Modifier.padding(end = dimensionResource(R.dimen.button_spacing))
                )
                Text(
                    text = "Response Mode Logic",
                    fontSize = dimensionResource(R.dimen.title_font_size).value.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorResource(R.color.keywise_text_primary)
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = "How response modes optimize performance:",
                fontSize = dimensionResource(R.dimen.body_font_size).value.sp,
                color = colorResource(R.color.keywise_text_secondary),
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            val modes = listOf(
                "Normal Mode: 256 tokens, concise responses, faster processing",
                "Detailed Mode: 512 tokens, comprehensive output, full capability",
                "Auto Prompt Adjustment: Adds conciseness instructions for Normal",
                "Persistent Settings: Choice remembered across sessions",
                "Real-time Application: Changes apply immediately"
            )
            
            modes.forEach { mode ->
                Row(
                    modifier = Modifier.padding(vertical = 3.dp)
                ) {
                    Text(
                        text = "• ",
                        fontSize = dimensionResource(R.dimen.body_font_size).value.sp,
                        color = colorResource(R.color.keywise_primary)
                    )
                    Text(
                        text = mode,
                        fontSize = dimensionResource(R.dimen.small_font_size).value.sp,
                        color = colorResource(R.color.keywise_text_secondary),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun KeyboardThemeSettingsCard(context: Context) {
    val selectedThemeId = remember { 
        mutableStateOf(getSelectedTheme(context))
    }
    
    // Expandable state
    val isExpanded = remember { mutableStateOf(false) }
    
    // Update theme when preference changes
    LaunchedEffect(selectedThemeId.value) {
        // This ensures the UI updates when theme changes
    }
    
    val selectedTheme = KeyboardThemes.getThemeById(selectedThemeId.value)
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(dimensionResource(R.dimen.card_corner_radius)),
        colors = CardDefaults.cardColors(containerColor = colorResource(R.color.keywise_card_background)),
        elevation = CardDefaults.cardElevation(defaultElevation = dimensionResource(R.dimen.elevation_card))
    ) {
        Column(
            modifier = Modifier
                .padding(dimensionResource(R.dimen.card_padding))
                .animateContentSize()
        ) {
            // Header with expand/collapse button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded.value = !isExpanded.value },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = null,
                        tint = colorResource(R.color.keywise_primary),
                        modifier = Modifier.size(dimensionResource(R.dimen.large_icon_size))
                    )
                    Spacer(modifier = Modifier.width(dimensionResource(R.dimen.button_spacing)))
                    Text(
                        text = "Keyboard Themes",
                        fontSize = dimensionResource(R.dimen.title_font_size).value.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorResource(R.color.keywise_text_primary)
                    )
                }
                
                // Expand/Collapse Icon
                Icon(
                    imageVector = if (isExpanded.value) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded.value) "Collapse" else "Expand",
                    tint = colorResource(R.color.keywise_primary),
                    modifier = Modifier.size(24.dp)
                )
            }
            
            // Show selected theme info when collapsed
            if (!isExpanded.value) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Current: ${selectedTheme.name}",
                    fontSize = dimensionResource(R.dimen.small_font_size).value.sp,
                    color = colorResource(R.color.keywise_text_secondary)
                )
            }
            
            // Expandable content
            if (isExpanded.value) {
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = "Choose your keyboard color theme",
                    fontSize = dimensionResource(R.dimen.body_font_size).value.sp,
                    color = colorResource(R.color.keywise_text_secondary),
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                // Current Selected Theme Preview
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                                    colors = listOf(
                                        selectedTheme.getStartColor(),
                                        selectedTheme.getEndColor()
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Current: ${selectedTheme.name}",
                            color = selectedTheme.getTextColor(),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Theme Grid
                val themes = KeyboardThemes.getAllThemes()
                val rows = themes.chunked(2)
                
                rows.forEach { rowThemes ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowThemes.forEach { theme ->
                                                     ThemeOptionCard(
                                 theme = theme,
                                 isSelected = selectedThemeId.value == theme.id,
                                 onClick = {
                                     selectedThemeId.value = theme.id
                                     setSelectedTheme(context, theme.id)
                                     // Force refresh the preview
                                 },
                                 modifier = Modifier.weight(1f)
                             )
                        }
                        if (rowThemes.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun ThemeOptionCard(
    theme: KeyboardTheme,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(80.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 6.dp else 2.dp),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(
            2.dp, 
            colorResource(R.color.keywise_primary)
        ) else null
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                        colors = listOf(
                            theme.getStartColor(),
                            theme.getEndColor()
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = theme.name,
                    color = theme.getTextColor(),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                
                if (isSelected) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "✓",
                        color = theme.getTextColor(),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun KeyBackgroundStyleSettingsCard(context: Context) {
    val selectedStyleId = remember { 
        mutableStateOf(getSelectedKeyBackgroundStyle(context))
    }
    
    // Expandable state
    val isExpanded = remember { mutableStateOf(false) }
    
    val selectedStyle = KeyBackgroundStyles.getStyleById(selectedStyleId.value)
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(dimensionResource(R.dimen.card_corner_radius)),
        colors = CardDefaults.cardColors(containerColor = colorResource(R.color.keywise_card_background)),
        elevation = CardDefaults.cardElevation(defaultElevation = dimensionResource(R.dimen.elevation_card))
    ) {
        Column(
            modifier = Modifier
                .padding(dimensionResource(R.dimen.card_padding))
                .animateContentSize()
        ) {
            // Header with expand/collapse button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded.value = !isExpanded.value },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Keyboard,
                        contentDescription = null,
                        tint = colorResource(R.color.keywise_primary),
                        modifier = Modifier.size(dimensionResource(R.dimen.large_icon_size))
                    )
                    Spacer(modifier = Modifier.width(dimensionResource(R.dimen.button_spacing)))
                    Text(
                        text = "Key Background Style",
                        fontSize = dimensionResource(R.dimen.title_font_size).value.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorResource(R.color.keywise_text_primary)
                    )
                }
                
                // Expand/Collapse Icon
                Icon(
                    imageVector = if (isExpanded.value) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded.value) "Collapse" else "Expand",
                    tint = colorResource(R.color.keywise_primary),
                    modifier = Modifier.size(24.dp)
                )
            }
            
            // Show selected style info when collapsed
            if (!isExpanded.value) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Current: ${selectedStyle.name}",
                    fontSize = dimensionResource(R.dimen.small_font_size).value.sp,
                    color = colorResource(R.color.keywise_text_secondary)
                )
            }
            
            // Expandable content
            if (isExpanded.value) {
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = "Customize the appearance of individual keys (separate from keyboard theme)",
                    fontSize = dimensionResource(R.dimen.body_font_size).value.sp,
                    color = colorResource(R.color.keywise_text_secondary),
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                // Style Options
                val styles = KeyBackgroundStyles.getAllStyles()
                
                styles.forEach { style ->
                    KeyBackgroundStyleOption(
                        style = style,
                        isSelected = selectedStyleId.value == style.id,
                        onClick = {
                            selectedStyleId.value = style.id
                            setSelectedKeyBackgroundStyle(context, style.id)
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun KeyBackgroundStyleOption(
    style: KeyBackgroundStyle,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(
            2.dp, 
            colorResource(R.color.keywise_primary)
        ) else null,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                colorResource(R.color.keywise_primary).copy(alpha = 0.1f) 
            else 
                colorResource(R.color.keywise_card_background)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Preview box showing the style
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = when (style.id) {
                            "light_white" -> Color.White
                            "light_transparent" -> Color.White.copy(alpha = 0.5f)
                            else -> Color(0xFF58585A) // Dark
                        },
                        shape = RoundedCornerShape(6.dp)
                    )
                    .then(
                        if (style.id != "dark") {
                            Modifier.background(
                                Color.Gray.copy(alpha = 0.3f),
                                RoundedCornerShape(6.dp)
                            )
                        } else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "A",
                    color = when (style.id) {
                        "light_white" -> Color.Black
                        "light_transparent" -> Color.DarkGray
                        else -> Color.White
                    },
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Style info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = style.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = colorResource(R.color.keywise_text_primary)
                )
                Text(
                    text = style.description,
                    fontSize = 14.sp,
                    color = colorResource(R.color.keywise_text_secondary)
                )
            }
            
            // Selection indicator
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = colorResource(R.color.keywise_primary),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun AdditionalSettingsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(dimensionResource(R.dimen.card_corner_radius)),
        colors = CardDefaults.cardColors(containerColor = colorResource(R.color.keywise_card_background)),
        elevation = CardDefaults.cardElevation(defaultElevation = dimensionResource(R.dimen.elevation_card))
    ) {
        Column(
            modifier = Modifier.padding(dimensionResource(R.dimen.card_padding))
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = null,
                    tint = colorResource(R.color.keywise_primary),
                    modifier = Modifier.size(dimensionResource(R.dimen.large_icon_size))
                )
                Spacer(modifier = Modifier.width(dimensionResource(R.dimen.button_spacing)))
                Text(
                    text = "Additional Settings",
                    fontSize = dimensionResource(R.dimen.title_font_size).value.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorResource(R.color.keywise_text_primary)
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = "More customization options coming soon:",
                fontSize = dimensionResource(R.dimen.body_font_size).value.sp,
                color = colorResource(R.color.keywise_text_secondary),
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            val futureSettings = listOf(
                "Theme customization",
                "Keyboard layout options", 
                "Language preferences",
                "Usage analytics",
                "Notification settings"
            )
            
            futureSettings.forEach { setting ->
                Row(
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Text(
                        text = setting,
                        fontSize = dimensionResource(R.dimen.small_font_size).value.sp,
                        color = colorResource(R.color.keywise_text_secondary)
                    )
                }
            }
        }
    }
}

@Composable
fun SetupTabContent(
    isKeyboardEnabled: Boolean,
    isKeyboardSelected: Boolean,
    hasRestartedAfterSetup: Boolean,
    refreshStatus: () -> Unit,
    onRestartCompleted: () -> Unit,
    context: Context
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Progress Indicator
        ProgressIndicator(
            isKeyboardEnabled = isKeyboardEnabled,
            isKeyboardSelected = isKeyboardSelected
        )
        
        // Setup Steps
        EnhancedSetupStepCard(
            stepNumber = 1,
            title = "Enable SendRight Keyboard",
            description = "Add SendRight to your device's enabled keyboards",
            isCompleted = isKeyboardEnabled,
            action = {
                ModernActionButton(
                    text = if (isKeyboardEnabled) "Enabled" else "Open Settings",
                    isCompleted = isKeyboardEnabled,
                    onClick = {
                        val intent = Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)
                        context.startActivity(intent)
                        CoroutineScope(Dispatchers.Main).launch {
                            kotlinx.coroutines.delay(500)
                            refreshStatus()
                        }
                    }
                )
            }
        )
        
        EnhancedSetupStepCard(
            stepNumber = 2,
            title = "Select SendRight as Default",
            description = "Choose SendRight as your active keyboard",
            isCompleted = isKeyboardSelected,
            action = {
                ModernActionButton(
                    text = if (isKeyboardSelected) "Selected" else "Choose Keyboard",
                    isCompleted = isKeyboardSelected,
                    onClick = {
                        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                        imm.showInputMethodPicker()
                        CoroutineScope(Dispatchers.Main).launch {
                            kotlinx.coroutines.delay(500)
                            refreshStatus()
                        }
                    }
                )
            }
        )
        
        // Usage Information
        UsageInfoCard()
        
        // Ready to Use Card
        if (isKeyboardEnabled && isKeyboardSelected) {
            ReadyToUseCard()
            
            // Safety and Restart Instructions (only if not restarted yet)
            if (!hasRestartedAfterSetup) {
                SafetyRestartCard(onRestartCompleted = onRestartCompleted)
            }
        } else {
            // General Safety Information (during setup)
            GeneralSafetyCard()
        }
    }
}

@Composable
fun HomeTabContent() {
    var testText by remember { mutableStateOf("") }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(R.color.keywise_background)) // Add consistent background
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top section with input field
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Test input field with black text
            OutlinedTextField(
                value = testText,
                onValueChange = { testText = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                label = { Text("Test your typing here...") },
                placeholder = { Text("Start typing to test SendRight keyboard") },
                minLines = 3,
                maxLines = 5,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colorResource(R.color.keywise_primary),
                    focusedLabelColor = colorResource(R.color.keywise_primary),
                    cursorColor = colorResource(R.color.keywise_primary)
                ),
                textStyle = androidx.compose.ui.text.TextStyle(color = Color.Black), // Black text for better contrast
                shape = RoundedCornerShape(12.dp)
            )
            
            // Grammar correction demo instruction
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = colorResource(R.color.keywise_success_light)
                ),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            tint = colorResource(R.color.keywise_primary), // Green tint
                            modifier = Modifier
                                .size(20.dp)
                                .padding(end = 8.dp)
                        )
                        Text(
                            text = "Grammar Correction Demo",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = colorResource(R.color.keywise_text_primary)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "Try typing: \"I has a good day today\"",
                        fontSize = 14.sp,
                        color = colorResource(R.color.keywise_text_secondary),
                        fontStyle = FontStyle.Italic
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = "Then hit the rewrite button and see the magic! ✨",
                        fontSize = 14.sp,
                        color = colorResource(R.color.keywise_primary),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            // Instructions
            Text(
                text = "Use this area to test SendRight keyboard features like AI assistance, smart suggestions, and grammar correction!",
                fontSize = 14.sp,
                color = colorResource(R.color.keywise_text_secondary),
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}

@Composable
fun SettingsTabContent(context: Context) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(R.color.keywise_background)) // Add proper background
            .padding(16.dp) // Keep content padding
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // AI Model Selection (GPT-5 vs Gemini) - NEW: Added at the top
        AIModelSelectionCard(context)
        
        // AI Response Settings (moved from main screen)
        AIResponseSettingsCard(context)
        
        // Keyboard Theme Settings
        KeyboardThemeSettingsCard(context)
        
        // Key Background Style Settings
        KeyBackgroundStyleSettingsCard(context)
        
        // Additional Settings
        AdditionalSettingsCard()
    }
}

@Composable
fun EnableScreensTabContent(context: Context) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header Section
        Text(
            text = "SendRight Visual Setup",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = colorResource(R.color.keywise_text_primary),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Text(
            text = "Beautiful green-themed setup experience with 3D toggle switches and modern design.",
            fontSize = 16.sp,
            color = colorResource(R.color.keywise_text_secondary),
            lineHeight = 24.sp
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Launch Enable Screens Button
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = colorResource(R.color.keywise_card_background)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.TouchApp,
                        contentDescription = null,
                        tint = colorResource(R.color.keywise_primary),
                        modifier = Modifier.size(32.dp)
                    )
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Visual Setup Experience",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colorResource(R.color.keywise_text_primary)
                        )
                        
                        Text(
                            text = "Modern step-by-step setup process",
                            fontSize = 14.sp,
                            color = colorResource(R.color.keywise_text_secondary)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = {
                        val intent = Intent(context, com.vishruth.key1.ui.enable.EnableScreensActivity::class.java)
                        context.startActivity(intent)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorResource(R.color.keywise_primary)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Launch Visual Setup",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
        
        // Feature Preview Cards
        EnableScreensFeaturePreview()
    }
}

@Composable
fun EnableScreensFeaturePreview() {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "What's New in Visual Setup",
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = colorResource(R.color.keywise_text_primary),
            modifier = Modifier.padding(vertical = 8.dp)
        )
        
        // Feature cards
        val features = listOf(
            Triple("Interactive Animations", "Smooth transitions and visual feedback", Icons.Default.Animation),
            Triple("Toggle Illustrations", "Clear visual indicators for each step", Icons.Default.ToggleOn),
            Triple("Modern Design", "Updated UI with improved accessibility", Icons.Default.Palette),
            Triple("Step Progress", "Clear indication of setup progress", Icons.Default.Timeline)
        )
        
        features.forEach { (title, description, icon) ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = colorResource(R.color.keywise_card_background)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = colorResource(R.color.keywise_primary),
                        modifier = Modifier.size(24.dp)
                    )
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Column {
                        Text(
                            text = title,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = colorResource(R.color.keywise_text_primary)
                        )
                        
                        Text(
                            text = description,
                            fontSize = 14.sp,
                            color = colorResource(R.color.keywise_text_secondary)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChatTabContent() {
    val context = LocalContext.current
    val chatRepository = remember { ChatRepository(context) }
    val scope = rememberCoroutineScope()
    
    // Enhanced chat state with conversation memory
    var chatMessages by remember { mutableStateOf(listOf<ChatMessage>()) }
    var currentMessage by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var currentConversation by remember { mutableStateOf<ChatConversation?>(null) }
    var showExtraActions by remember { mutableStateOf(false) }
    
    // Scroll state for auto-scrolling
    val listState = rememberLazyListState()
    
    // Initialize conversation
    LaunchedEffect(Unit) {
        val conversation = chatRepository.getCurrentConversation() ?: chatRepository.createNewConversation()
        currentConversation = conversation
        
        // Convert to UI-compatible ChatMessage format
        chatMessages = conversation.messages.map { msg ->
                ChatMessage(
                content = msg.content,
                isFromUser = msg.isFromUser,
                timestamp = msg.timestamp,
                messageType = msg.messageType
            )
        }
        
        // Show extra actions if conversation has messages
        showExtraActions = conversation.messages.size > 1
    }
    
    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(chatMessages.size) {
        if (chatMessages.isNotEmpty()) {
            delay(100) // Small delay to ensure proper rendering
            listState.animateScrollToItem(chatMessages.size - 1)
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(R.color.keywise_background)) // Use consistent background color
    ) {
        // Minimalistic header with conversation info - HIDDEN to save space
        // The time-based chat memory display has been removed as requested
        
        // Chat messages list with improved spacing
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            items(chatMessages) { message ->
                EnhancedChatMessageBubble(message = message)
            }
            
            // Loading indicator
            if (isLoading) {
                item {
                    ChatLoadingBubble()
                }
            }
        }
        
        // Enhanced input area with conversation management
        ChatInputArea(
            message = currentMessage,
            onMessageChange = { currentMessage = it },
            isLoading = isLoading,
            showExtraActions = showExtraActions,
            onNewChat = {
                scope.launch {
                    try {
                        val newConversation = chatRepository.createNewConversation()
                        currentConversation = newConversation
                        chatMessages = newConversation.messages.map { msg ->
                            ChatMessage(
                                content = msg.content,
                                isFromUser = msg.isFromUser,
                                timestamp = msg.timestamp,
                                messageType = msg.messageType
                            )
                        }
                        showExtraActions = false
                        Log.d("ChatTab", "Started new conversation")
                    } catch (e: Exception) {
                        Log.e("ChatTab", "Error creating new conversation", e)
                    }
                }
            },
            onSummarize = {
                currentConversation?.let { conversation ->
                    scope.launch {
                        try {
                            isLoading = true
                            Log.d("ChatTab", "Starting conversation summarization")
                            val result = chatRepository.summarizeConversation(conversation)
                            
                            result.onSuccess { summary ->
                                // Refresh the conversation to show the summary
                                val updatedConversation = chatRepository.getCurrentConversation()
                                if (updatedConversation != null) {
                                    currentConversation = updatedConversation
                                    chatMessages = updatedConversation.messages.map { msg ->
                                        ChatMessage(
                                            content = msg.content,
                                            isFromUser = msg.isFromUser,
                                            timestamp = msg.timestamp,
                                            messageType = msg.messageType
                                        )
                                    }
                                }
                                Log.d("ChatTab", "Conversation summarized successfully")
                            }.onFailure { error ->
                                Log.e("ChatTab", "Summarization failed: ${error.message}")
                            }
                        } catch (e: Exception) {
                            Log.e("ChatTab", "Error during summarization", e)
                        } finally {
                            isLoading = false
                        }
                    }
                }
            },
            onSendMessage = {
                if (currentMessage.trim().isNotEmpty() && !isLoading) {
                    val messageToSend = currentMessage.trim()
                    currentMessage = ""
                    isLoading = true
                    showExtraActions = true
                    
                    scope.launch {
                        try {
                            Log.d("ChatTab", "Sending message with context: ${messageToSend.take(50)}...")
                            val startTime = System.currentTimeMillis()
                            
                            // Use ChatRepository for context-aware messaging
                            val result = chatRepository.sendMessageWithContext(messageToSend)
                            
                            val endTime = System.currentTimeMillis()
                            val duration = endTime - startTime
                            
                            result.onSuccess { response ->
                                // Refresh conversation to get latest messages
                                val updatedConversation = chatRepository.getCurrentConversation()
                                if (updatedConversation != null) {
                                    currentConversation = updatedConversation
                                    chatMessages = updatedConversation.messages.map { msg ->
                                        ChatMessage(
                                            content = msg.content,
                                            isFromUser = msg.isFromUser,
                                            timestamp = msg.timestamp,
                                            messageType = msg.messageType
                                        )
                                    }
                                }
                                Log.d("ChatTab", "Context-aware response received in ${duration}ms")
                            }.onFailure { error ->
                                Log.e("ChatTab", "Context-aware response failed after ${duration}ms: ${error.message}")
                                val errorMessage = ChatMessage(
                                    content = error.message ?: "Sorry, I couldn't process your message. Please try again.",
                                    isFromUser = false,
                                    messageType = MessageType.SYSTEM
                                )
                                chatMessages = chatMessages + errorMessage
                            }
                            
                        } catch (e: Exception) {
                            Log.e("ChatTab", "Unexpected error in enhanced chat: ${e.message}")
                            val errorMessage = ChatMessage(
                                content = "An unexpected error occurred. Please try again.",
                                isFromUser = false,
                                messageType = MessageType.SYSTEM
                            )
                            chatMessages = chatMessages + errorMessage
                        } finally {
                            isLoading = false
                        }
                    }
                }
            }
        )
    }
}

/**
 * Onboarding Enable Screens - Step-by-step onboarding flow
 * Shows Enable → Select → Complete sequence as onboarding screens
 * Acts as a wall until keyboard setup is fully completed
 */
@Composable
fun EnableScreensOnboarding() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    // Real-time keyboard status tracking
    val isKeyboardEnabled = remember { 
        mutableStateOf(isKeyboardEnabled(context))
    }
    
    val isKeyboardSelected = remember {
        mutableStateOf(isKeyboardSelected(context))
    }
    
    // Current onboarding screen state - Always start with ENABLE_KEYBOARD for onboarding
    var currentScreen by remember { 
        mutableStateOf(OnboardingScreen.ENABLE_KEYBOARD)
    }
    
    // Function to refresh status and auto-advance screens
    val refreshStatus = {
        isKeyboardEnabled.value = isKeyboardEnabled(context)
        isKeyboardSelected.value = isKeyboardSelected(context)
        
        // Auto-advance based on completion
        if (isKeyboardEnabled.value && isKeyboardSelected.value && currentScreen != OnboardingScreen.SETUP_COMPLETE) {
            currentScreen = OnboardingScreen.SETUP_COMPLETE
            // Auto-navigate to main app after 2 seconds
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                kotlinx.coroutines.delay(2000)
                navigateToMainApp(context)
            }
        }
    }
    
    // Lifecycle observer for when user returns from settings
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
    
    // Periodic status check for real-time updates
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(1000)
            refreshStatus()
        }
    }
    
    // Render current onboarding screen
    when (currentScreen) {
        OnboardingScreen.ENABLE_KEYBOARD -> {
            EnableKeyboardScreen(
                onEnableClick = {
                    val intent = Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)
                    context.startActivity(intent)
                    // Move to next screen after opening settings
                    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                        kotlinx.coroutines.delay(500)
                        currentScreen = OnboardingScreen.SELECT_KEYBOARD
                        refreshStatus()
                    }
                },
                onSkipClick = {
                    // Move to next screen anyway (onboarding progression)
                    currentScreen = OnboardingScreen.SELECT_KEYBOARD
                }
            )
        }
        
        OnboardingScreen.SELECT_KEYBOARD -> {
            SelectKeyboardScreen(
                onSelectClick = {
                    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.showInputMethodPicker()
                    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                        kotlinx.coroutines.delay(500)
                        refreshStatus()
                    }
                },
                onSkipClick = {
                    // Move to completion screen anyway (onboarding progression)
                    currentScreen = OnboardingScreen.SETUP_COMPLETE
                }
            )
        }
        
        OnboardingScreen.SETUP_COMPLETE -> {
            SetupCompleteScreen(
                onContinueClick = {
                    navigateToMainApp(context)
                }
            )
        }
    }
}

/**
 * Onboarding Screen Enum for step-by-step progression
 */
enum class OnboardingScreen {
    ENABLE_KEYBOARD,
    SELECT_KEYBOARD,
    SETUP_COMPLETE
}

/**
 * Navigate to main app after onboarding completion
 */
fun navigateToMainApp(context: Context) {
    if (context is ComponentActivity) {
        context.recreate() // Recreate activity to trigger onCreate with new setup status
    }
}

@Preview(showBackground = true)
@Composable
fun SendRightSetupScreenPreview() {
    Key1Theme {
        SendRightSetupScreen()
    }
}

// First-time restart recommendation dialog
@Composable
fun RestartRecommendationDialog(
    onDismiss: () -> Unit,
    onRestart: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            // Only restart button, no skip
            Button(
                onClick = onRestart,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorResource(R.color.keywise_gradient_start)
                )
            ) {
                Text("Restart Later", color = Color.White)
            }
        },
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = colorResource(R.color.keywise_gradient_start),
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    "Setup Complete!",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "🎉 SendRight is now ready to use!",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black
                )
                
                Text(
                    "For optimal performance and to ensure all features work properly, we recommend restarting your device.",
                    fontSize = 14.sp,
                    color = Color.Black,
                    lineHeight = 20.sp
                )
                
                Text(
                    "💡 You can restart anytime from your power menu.",
                    fontSize = 12.sp,
                    color = Color.Black,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
            }
        },
        containerColor = colorResource(R.color.keywise_card_background)
    )
}

// Helper functions for tracking restart popup
fun getHasShownRestartPopup(context: Context): Boolean {
    val sharedPref = context.getSharedPreferences("sendright_prefs", Context.MODE_PRIVATE)
    return sharedPref.getBoolean("has_shown_restart_popup", false)
}

fun setHasShownRestartPopup(context: Context, hasShown: Boolean) {
    val sharedPref = context.getSharedPreferences("sendright_prefs", Context.MODE_PRIVATE)
    with(sharedPref.edit()) {
        putBoolean("has_shown_restart_popup", hasShown)
        apply()
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    Key1Theme {
        SendRightSetupScreen()
    }
}