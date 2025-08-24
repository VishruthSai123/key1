package com.vishruth.key1.ui.report

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vishruth.key1.R
import com.vishruth.key1.ui.theme.Key1Theme

/**
 * Report Activity for AI-Generated Content policy compliance
 * Allows users to report offensive or inappropriate AI-generated content
 */
class ReportActivity : ComponentActivity() {
    
    companion object {
        const val SUPPORT_EMAIL = "vishruthsait@gmail.com"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            Key1Theme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = colorResource(R.color.keywise_background)
                ) {
                    ReportScreen(
                        onBackPressed = { finish() },
                        onSubmitReport = { issueType, description ->
                            sendReportEmail(issueType, description)
                        }
                    )
                }
            }
        }
    }
    
    private fun sendReportEmail(issueType: String, description: String) {
        val subject = "SendRight Report: $issueType"
        val body = "I'm facing: $description\n\n" +
                "Issue Type: $issueType\n" +
                "App Version: 2.1\n" +
                "Device: ${android.os.Build.MODEL}\n" +
                "Android Version: ${android.os.Build.VERSION.RELEASE}\n\n" +
                "Please investigate this issue with the AI-generated content."
        
        val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:")
            putExtra(Intent.EXTRA_EMAIL, arrayOf(SUPPORT_EMAIL))
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
        }
        
        try {
            startActivity(Intent.createChooser(emailIntent, "Send Report Email"))
            finish() // Close report screen after sending
        } catch (e: Exception) {
            // Handle case where no email app is available
            e.printStackTrace()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(
    onBackPressed: () -> Unit,
    onSubmitReport: (String, String) -> Unit
) {
    val context = LocalContext.current
    var selectedIssue by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    
    val issueTypes = listOf(
        "Inappropriate Content",
        "Offensive Language", 
        "Harmful AI Suggestions",
        "Misleading Information",
        "Privacy Concerns",
        "Spam or Repetitive Content",
        "Technical Issues",
        "Other"
    )
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(R.color.keywise_background))
    ) {
        // Header with back button
        TopAppBar(
            title = {
                Text(
                    text = "Report Content",
                    color = colorResource(R.color.keywise_text_primary),
                    fontWeight = FontWeight.Bold
                )
            },
            navigationIcon = {
                IconButton(onClick = onBackPressed) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = colorResource(R.color.keywise_text_primary)
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = colorResource(R.color.keywise_card_background)
            )
        )
        
        // Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Information Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = colorResource(R.color.keywise_card_background)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                border = null
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Text(
                        text = "Report AI-Generated Content",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorResource(R.color.black)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Help us improve SendRight by reporting inappropriate or offensive AI-generated content. Your feedback helps us maintain a safe and respectful experience for all users.",
                        fontSize = 14.sp,
                        color = colorResource(R.color.keywise_text_secondary),
                        lineHeight = 20.sp
                    )
                }
            }
            
            // Issue Type Selection
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = colorResource(R.color.keywise_card_background)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                border = null
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Text(
                        text = "Issue Type *",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorResource(R.color.black)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded }
                    ) {
                        OutlinedTextField(
                            value = selectedIssue,
                            onValueChange = { },
                            readOnly = true,
                            placeholder = { 
                                Text(
                                    "Select issue type",
                                    color = colorResource(R.color.black).copy(alpha = 0.6f)
                                ) 
                            },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = colorResource(R.color.keywise_primary),
                                focusedLabelColor = colorResource(R.color.keywise_primary),
                                cursorColor = colorResource(R.color.keywise_primary),
                                focusedTextColor = colorResource(R.color.black),
                                unfocusedTextColor = colorResource(R.color.black)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                        
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = colorResource(R.color.black),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Column {
                                    issueTypes.forEach { issue ->
                                        DropdownMenuItem(
                                            text = { 
                                                Text(
                                                    issue,
                                                    color = colorResource(R.color.white)
                                                ) 
                                            },
                                            onClick = {
                                                selectedIssue = issue
                                                expanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            // Description Input
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = colorResource(R.color.keywise_card_background)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                border = null
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Text(
                        text = "Description *",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorResource(R.color.black)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { 
                            Text(
                                "Please describe the issue in detail...",
                                color = colorResource(R.color.black).copy(alpha = 0.6f)
                            ) 
                        },
                        minLines = 4,
                        maxLines = 8,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colorResource(R.color.keywise_primary),
                            focusedLabelColor = colorResource(R.color.keywise_primary),
                            cursorColor = colorResource(R.color.keywise_primary),
                            focusedTextColor = colorResource(R.color.black),
                            unfocusedTextColor = colorResource(R.color.black)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Please provide as much detail as possible about the inappropriate content, including when it occurred and what AI action was used.",
                        fontSize = 12.sp,
                        color = colorResource(R.color.keywise_text_secondary),
                        lineHeight = 16.sp
                    )
                }
            }
            
            // Submit Button
            Button(
                onClick = {
                    if (selectedIssue.isNotEmpty() && description.trim().isNotEmpty()) {
                        onSubmitReport(selectedIssue, description.trim())
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                enabled = selectedIssue.isNotEmpty() && description.trim().isNotEmpty(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorResource(R.color.keywise_primary),
                    disabledContainerColor = colorResource(R.color.keywise_text_secondary).copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Submit Report",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorResource(R.color.keywise_background)
                )
            }
            
            // Privacy Notice
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = colorResource(R.color.keywise_success_light).copy(alpha = 0.1f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                border = null
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Text(
                        text = "Privacy Notice",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorResource(R.color.keywise_primary)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Your report will be sent via email to our support team. We respect your privacy and will only use this information to investigate and resolve the reported issue.",
                        fontSize = 12.sp,
                        color = colorResource(R.color.keywise_text_secondary),
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}
