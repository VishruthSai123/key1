package com.vishruth.key1.ui.enable

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vishruth.key1.R

/**
 * Enable Keyboard Screen - Exact match to screenshot design
 * Features green gradient background and 3D toggle switch
 */
@Composable
fun EnableKeyboardScreen(
    onEnableClick: () -> Unit,
    onSkipClick: () -> Unit
) {
    var showAnimation by remember { mutableStateOf(false) }
    val animationScale by animateFloatAsState(
        targetValue = if (showAnimation) 1f else 0.8f,
        animationSpec = tween(durationMillis = 800),
        label = "enable_animation"
    )
    
    LaunchedEffect(Unit) {
        showAnimation = true
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1B4D3E), // Dark green top
                        Color(0xFF2D5A3D), // Medium dark green
                        Color(0xFF4CAF50), // Bright green
                        Color(0xFF66BB6A)  // Light green bottom
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top spacer for centering
            Spacer(modifier = Modifier.weight(0.3f))
            
            // 3D Toggle Switch Illustration - OFF State (centered)
            Box(
                modifier = Modifier.scale(animationScale),
                contentAlignment = Alignment.Center
            ) {
                ToggleSwitchOffIllustration()
            }
            
            // Spacer between icon and text
            Spacer(modifier = Modifier.height(80.dp))
            
            // Title and Description Text Section
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.scale(animationScale)
            ) {
                // Title Text - Exact match to screenshot
                Text(
                    text = "Enable SendRight Keyboard",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    lineHeight = 34.sp
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Subtitle Text - Exact match to screenshot
                Text(
                    text = "Add SendRight To Your Devices Enabled\nKeyboards",
                    fontSize = 16.sp,
                    color = Color(0xFFB8E6B8), // Light green text
                    textAlign = TextAlign.Center,
                    lineHeight = 24.sp
                )
            }
            
            // Spacer to push button down (closer to text)
            Spacer(modifier = Modifier.height(60.dp))
            
            // Action Button - Bright green to match screenshot exactly
            Button(
                onClick = onEnableClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .scale(animationScale),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White // White background
                ),
                shape = RoundedCornerShape(28.dp)
            ) {
                Text(
                    text = "Open Settings",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF4CAF50)
                )
            }
            
            // Bottom spacer
            Spacer(modifier = Modifier.weight(0.2f))
        }
    }
}

/**
 * Select Keyboard Screen - Exact match to screenshot design
 * Features green gradient background and 3D keyboard icon
 */
@Composable
fun SelectKeyboardScreen(
    onSelectClick: () -> Unit,
    onSkipClick: () -> Unit
) {
    var showAnimation by remember { mutableStateOf(false) }
    val animationScale by animateFloatAsState(
        targetValue = if (showAnimation) 1f else 0.8f,
        animationSpec = tween(durationMillis = 800),
        label = "select_animation"
    )
    
    LaunchedEffect(Unit) {
        showAnimation = true
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1B4D3E), // Dark green top
                        Color(0xFF2D5A3D), // Medium dark green
                        Color(0xFF4CAF50), // Bright green
                        Color(0xFF66BB6A)  // Light green bottom
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top spacer for centering
            Spacer(modifier = Modifier.weight(0.3f))
            
            // 3D Keyboard Icon Illustration - Exact match to PNG (centered)
            Box(
                modifier = Modifier.scale(animationScale),
                contentAlignment = Alignment.Center
            ) {
                KeyboardIconIllustration()
            }
            
            // Spacer between icon and text
            Spacer(modifier = Modifier.height(80.dp))
            
            // Title and Description Text Section
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.scale(animationScale)
            ) {
                // Title Text - Exact match to screenshot
                Text(
                    text = "Select SendRight as Default",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    lineHeight = 34.sp
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Subtitle Text - Exact match to screenshot
                Text(
                    text = "Choose SendRight As Your Active\nKeyboards",
                    fontSize = 16.sp,
                    color = Color(0xFFB8E6B8), // Light green text
                    textAlign = TextAlign.Center,
                    lineHeight = 24.sp
                )
            }
            
            // Spacer to push button down (closer to text)
            Spacer(modifier = Modifier.height(60.dp))
            
            // Action Button - Bright green to match screenshot exactly
            Button(
                onClick = onSelectClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .scale(animationScale),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White // White background
                ),
                shape = RoundedCornerShape(28.dp)
            ) {
                Text(
                    text = "Choose Keyboard",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF4CAF50)
                )
            }
            
            // Bottom spacer
            Spacer(modifier = Modifier.weight(0.2f))
        }
    }
}

/**
 * Setup Complete Screen - Success screen with green theme
 */
@Composable
fun SetupCompleteScreen(
    onContinueClick: () -> Unit
) {
    var showAnimation by remember { mutableStateOf(false) }
    val animationScale by animateFloatAsState(
        targetValue = if (showAnimation) 1f else 0.8f,
        animationSpec = tween(durationMillis = 800),
        label = "complete_animation"
    )
    
    LaunchedEffect(Unit) {
        showAnimation = true
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1B4D3E), // Dark green top
                        Color(0xFF2D5A3D), // Medium dark green
                        Color(0xFF4CAF50), // Bright green
                        Color(0xFF66BB6A)  // Light green bottom
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Success Animation
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .scale(animationScale)
                    .background(
                        color = Color(0xFF4CAF50), // Green success color
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Success",
                    tint = Color.White,
                    modifier = Modifier.size(60.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(40.dp))
            
            Text(
                text = "Setup Complete!",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.scale(animationScale)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "SendRight is ready to use.\nEnjoy your AI-powered keyboard experience!",
                fontSize = 16.sp,
                color = Color(0xFFB8E6B8), // Light green text
                textAlign = TextAlign.Center,
                lineHeight = 24.sp,
                modifier = Modifier.scale(animationScale)
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            Button(
                onClick = onContinueClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .scale(animationScale),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White // White background
                ),
                shape = RoundedCornerShape(28.dp)
            ) {
                Text(
                    text = "Continue to App",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF4CAF50)
                )
            }
        }
    }
}
