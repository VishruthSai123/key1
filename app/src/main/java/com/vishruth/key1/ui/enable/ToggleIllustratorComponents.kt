package com.vishruth.key1.ui.enable

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 3D Toggle Switch Illustration - OFF State
 * Recreates the exact look from the screenshot assets with enhanced prominence
 */
@Composable
fun ToggleSwitchOffIllustration() {
    var showAnimation by remember { mutableStateOf(false) }
    val animationScale by animateFloatAsState(
        targetValue = if (showAnimation) 1f else 0.9f,
        animationSpec = tween(durationMillis = 1000),
        label = "toggle_scale"
    )
    
    LaunchedEffect(Unit) {
        showAnimation = true
    }
    
    Box(
        modifier = Modifier
            .size(220.dp, 110.dp) // Larger size for better prominence
            .scale(animationScale),
        contentAlignment = Alignment.Center
    ) {
        // Toggle Track Background (Enhanced 3D Effect)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .shadow(
                    elevation = 20.dp, // Increased shadow for more depth
                    shape = RoundedCornerShape(55.dp),
                    clip = false
                )
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF8BC34A), // Bright lime green top
                            Color(0xFF7CB342), // Medium lime green
                            Color(0xFF689F38), // Medium dark lime green
                            Color(0xFF558B2F)  // Dark lime green bottom
                        )
                    ),
                    shape = RoundedCornerShape(55.dp)
                )
        )
        
        // Toggle Track Inner Shadow (Enhanced depth effect)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF388E3C), // Dark green for depth
                            Color(0xFF4CAF50), // Medium green
                            Color(0xFF66BB6A)  // Lighter green
                        )
                    ),
                    shape = RoundedCornerShape(47.dp)
                )
        )
        
        // Enhanced grain/texture effect
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.15f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.08f)
                        ),
                        radius = 200f
                    ),
                    shape = RoundedCornerShape(47.dp)
                )
        )
        
        // Toggle Circle (OFF Position - Right Side) - Enhanced
        Box(
            modifier = Modifier
                .size(85.dp)
                .offset(x = 65.dp) // Positioned to the right for OFF state
                .shadow(
                    elevation = 16.dp, // Increased shadow
                    shape = CircleShape,
                    clip = false
                )
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White,
                            Color(0xFFF8F8F8),
                            Color(0xFFEEEEEE),
                            Color(0xFFE0E0E0)
                        ),
                        radius = 60f
                    ),
                    shape = CircleShape
                )
        ) {
            // Inner highlight for enhanced 3D effect
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(3.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.8f),
                                Color.Transparent
                            ),
                            radius = 35f
                        ),
                        shape = CircleShape
                    )
            )
        }
    }
}

/**
 * 3D Keyboard Icon Illustration - Enhanced to exactly match PNG reference
 * Rounded rectangular green shape with 3 white circular keys, more prominent
 */
@Composable
fun KeyboardIconIllustration() {
    var showAnimation by remember { mutableStateOf(false) }
    val animationScale by animateFloatAsState(
        targetValue = if (showAnimation) 1f else 0.9f,
        animationSpec = tween(durationMillis = 1000),
        label = "keyboard_scale"
    )
    
    LaunchedEffect(Unit) {
        showAnimation = true
    }
    
    Box(
        modifier = Modifier
            .size(260.dp, 160.dp) // Larger size for better prominence
            .scale(animationScale),
        contentAlignment = Alignment.Center
    ) {
        // Keyboard Base - Enhanced organic shape matching PNG exactly
        Box(
            modifier = Modifier
                .fillMaxSize()
                .shadow(
                    elevation = 24.dp, // Increased shadow for more depth
                    shape = RoundedCornerShape(
                        topStart = 65.dp,
                        topEnd = 65.dp,
                        bottomStart = 65.dp,
                        bottomEnd = 65.dp
                    ),
                    clip = false
                )
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF9CCC65), // Brightest lime green top
                            Color(0xFF8BC34A), // Bright lime green
                            Color(0xFF7CB342), // Medium lime green
                            Color(0xFF689F38), // Darker lime green
                            Color(0xFF558B2F)  // Dark lime green bottom
                        )
                    ),
                    shape = RoundedCornerShape(
                        topStart = 65.dp,
                        topEnd = 65.dp,
                        bottomStart = 65.dp,
                        bottomEnd = 65.dp
                    )
                )
        )
        
        // Enhanced texture/grain effect for realistic appearance
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.2f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.1f)
                        ),
                        radius = 350f
                    ),
                    shape = RoundedCornerShape(
                        topStart = 65.dp,
                        topEnd = 65.dp,
                        bottomStart = 65.dp,
                        bottomEnd = 65.dp
                    )
                )
        )
        
        // Additional subtle gradient overlay for depth
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.15f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.05f)
                        )
                    ),
                    shape = RoundedCornerShape(
                        topStart = 61.dp,
                        topEnd = 61.dp,
                        bottomStart = 61.dp,
                        bottomEnd = 61.dp
                    )
                )
        )
        
        // 3 White Circular Keys - Enhanced positioning and shadows
        Row(
            modifier = Modifier.padding(horizontal = 36.dp),
            horizontalArrangement = Arrangement.spacedBy(32.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(3) { index ->
                Box(
                    modifier = Modifier
                        .size(40.dp) // Slightly larger keys
                        .shadow(
                            elevation = 16.dp, // Increased shadow
                            shape = CircleShape,
                            clip = false
                        )
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color.White,
                                    Color(0xFFFAFAFA),
                                    Color(0xFFF0F0F0),
                                    Color(0xFFE8E8E8),
                                    Color(0xFFDDDDDD)
                                ),
                                radius = 55f
                            ),
                            shape = CircleShape
                        )
                ) {
                    // Enhanced inner highlight for 3D effect
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(2.dp)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.9f),
                                        Color.Transparent
                                    ),
                                    radius = 20f
                                ),
                                shape = CircleShape
                            )
                    )
                    
                    // Subtle inner shadow for depth
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(3.dp)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.03f)
                                    ),
                                    radius = 35f
                                ),
                                shape = CircleShape
                            )
                    )
                }
            }
        }
    }
}

/**
 * Toggle Switch ON State (for future use)
 */
@Composable
fun ToggleSwitchOnIllustration() {
    var showAnimation by remember { mutableStateOf(false) }
    val animationScale by animateFloatAsState(
        targetValue = if (showAnimation) 1f else 0.9f,
        animationSpec = tween(durationMillis = 1000),
        label = "toggle_on_scale"
    )
    
    LaunchedEffect(Unit) {
        showAnimation = true
    }
    
    Box(
        modifier = Modifier
            .size(200.dp, 100.dp)
            .scale(animationScale),
        contentAlignment = Alignment.Center
    ) {
        // Toggle Track Background (3D Effect) - Brighter for ON state
        Box(
            modifier = Modifier
                .fillMaxSize()
                .shadow(
                    elevation = 16.dp,
                    shape = RoundedCornerShape(50.dp),
                    clip = false
                )
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF81C784), // Brighter light green
                            Color(0xFF66BB6A), // Bright green
                            Color(0xFF4CAF50)  // Medium green
                        )
                    ),
                    shape = RoundedCornerShape(50.dp)
                )
        )
        
        // Toggle Track Inner (Active state)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF4CAF50), // Bright green for active
                            Color(0xFF66BB6A), // Medium green
                            Color(0xFF81C784)  // Light green
                        )
                    ),
                    shape = RoundedCornerShape(44.dp)
                )
        )
        
        // Toggle Circle (ON Position - Left Side)
        Box(
            modifier = Modifier
                .size(80.dp)
                .offset(x = (-60).dp) // Positioned to the left for ON state
                .shadow(
                    elevation = 12.dp,
                    shape = CircleShape,
                    clip = false
                )
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White,
                            Color(0xFFF5F5F5),
                            Color(0xFFEEEEEE)
                        )
                    ),
                    shape = CircleShape
                )
        )
    }
}
