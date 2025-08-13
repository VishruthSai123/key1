package com.vishruth.key1.repository

import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.vishruth.key1.api.GeminiService
import com.vishruth.key1.data.AIAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository for handling AI operations using Gemini
 */
class AIRepository(private val context: Context) {
    
    private val sharedPreferences = 
        context.getSharedPreferences("neoboard_prefs", Context.MODE_PRIVATE)
    
    private val geminiService = GeminiService()
    
    /**
     * Execute an AI action on the given text using Gemini
     */
    suspend fun executeAIAction(action: AIAction, text: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                // Check network connectivity first
                if (!isNetworkAvailable()) {
                    Log.e("AIRepository", "No network connection available")
                    return@withContext Result.failure(Exception("No internet connection"))
                }
                
                // Get response mode setting
                val responseMode = getResponseMode()
                val isNormalMode = responseMode == "normal"
                
                Log.d("AIRepository", "Network connection available, proceeding with API call")
                Log.d("AIRepository", "Response mode: $responseMode (isNormalMode: $isNormalMode)")
                val result = geminiService.generateContent(action.prompt, text, isNormalMode)
                
                if (result.isSuccess) {
                    // Increment usage count for tracking
                    incrementUsageCount()
                    Log.d("AIRepository", "AI action completed successfully")
                    
                    // Clean the response to remove unwanted prefixes and text
                    val response = result.getOrNull()
                    if (response != null) {
                        val cleanedResponse = cleanAIResponse(response)
                        Result.success(cleanedResponse)
                    } else {
                        result
                    }
                } else {
                    Log.e("AIRepository", "AI action failed: ${result.exceptionOrNull()?.message}")
                    result
                }
                
            } catch (e: Exception) {
                Log.e("AIRepository", "Exception in executeAIAction", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Execute an AI action with custom instructions from curly braces
     */
    suspend fun executeAIActionWithInstructions(action: AIAction, text: String, customInstructions: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                // Check network connectivity first
                if (!isNetworkAvailable()) {
                    Log.e("AIRepository", "No network connection available")
                    return@withContext Result.failure(Exception("No internet connection"))
                }
                
                // Get response mode setting
                val responseMode = getResponseMode()
                val isNormalMode = responseMode == "normal"
                
                // Create enhanced prompt with custom instructions
                val enhancedPrompt = createEnhancedPrompt(action.prompt, customInstructions)
                
                Log.d("AIRepository", "Network connection available, proceeding with custom instruction API call")
                Log.d("AIRepository", "Response mode: $responseMode (isNormalMode: $isNormalMode)")
                Log.d("AIRepository", "Original prompt: ${action.prompt}")
                Log.d("AIRepository", "Custom instructions: $customInstructions")
                Log.d("AIRepository", "Enhanced prompt: $enhancedPrompt")
                
                val result = geminiService.generateContent(enhancedPrompt, text, isNormalMode)
                
                if (result.isSuccess) {
                    // Increment usage count for tracking
                    incrementUsageCount()
                    Log.d("AIRepository", "AI action with custom instructions completed successfully")
                    
                    // Clean the response to remove unwanted prefixes and text
                    val response = result.getOrNull()
                    if (response != null) {
                        val cleanedResponse = cleanAIResponse(response)
                        Result.success(cleanedResponse)
                    } else {
                        result
                    }
                } else {
                    Log.e("AIRepository", "AI action with custom instructions failed: ${result.exceptionOrNull()?.message}")
                    result
                }
                
            } catch (e: Exception) {
                Log.e("AIRepository", "Exception in executeAIActionWithInstructions", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Create enhanced prompt by combining the base action prompt with custom instructions
     */
    private fun createEnhancedPrompt(basePrompt: String, customInstructions: String): String {
        return """$basePrompt

ADDITIONAL CUSTOM INSTRUCTIONS: $customInstructions

Please follow both the base instructions above AND the custom instructions. The custom instructions should take priority for specific requirements like length, style, tone, or format. Provide only the direct result without prefixes like 'Answer:' or explanatory text."""
    }
    
    /**
     * Check if network is available with enhanced validation
     */
    private fun isNetworkAvailable(): Boolean {
        try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = connectivityManager.activeNetwork ?: return false
            val networkCapabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            
            val hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            val isValidated = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            val isNotMetered = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
            
            Log.d("AIRepository", "Network check - Internet: $hasInternet, Validated: $isValidated, Not Metered: $isNotMetered")
            
            // Return true if we have internet capability and it's validated
            // We don't require NOT_METERED since users might be on mobile data
            return hasInternet && isValidated
            
        } catch (e: Exception) {
            Log.e("AIRepository", "Error checking network connectivity: ${e.message}")
            return false
        }
    }
    
    /**
     * Check if user has reached daily limit (for free tier)
     * UPDATED: Now always returns false to make keyboard completely free
     */
    fun hasReachedDailyLimit(): Boolean {
        // Always return false to remove daily limits completely
        return false
    }
    
    /**
     * Get remaining AI actions for today
     * UPDATED: Now always returns unlimited actions
     */
    fun getRemainingActions(): Int {
        // Always return unlimited actions
        return Int.MAX_VALUE
    }
    
    /**
     * Send a chat message for Answer Mode
     */
    suspend fun sendChatMessage(message: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                // Check network connectivity first
                if (!isNetworkAvailable()) {
                    Log.e("AIRepository", "No network connection available for chat")
                    return@withContext Result.failure(Exception("No internet connection. Please check your network and try again."))
                }
                
                // Get response mode setting
                val responseMode = getResponseMode()
                val isNormalMode = responseMode == "normal"
                
                // Use direct prompt for chat messages - no prefixes or extra text
                val chatPrompt = """Respond directly to the user's message.
                
CRITICAL OUTPUT REQUIREMENTS:
- NO prefixes like "Answer:", "Response:", "Sure,", "Here's", "I'll help", etc.
- NO explanatory text, instructions, or meta-commentary
- NO format descriptions or process explanations
- Start immediately with the actual response content
- Provide only the direct, helpful response"""
                
                Log.d("AIRepository", "Sending chat message to Gemini: $message")
                val result = geminiService.generateContent(chatPrompt, message, isNormalMode)
                
                if (result.isSuccess) {
                    // Increment usage count for tracking
                    incrementUsageCount()
                    Log.d("AIRepository", "Chat message processed successfully")
                    val response = result.getOrNull()
                    if (response.isNullOrBlank()) {
                        // Try fallback method
                        Log.d("AIRepository", "Primary method returned empty response, trying fallback")
                        tryFallbackChatResponse(message)
                    } else {
                        // Clean the response to remove unwanted prefixes and text
                        val cleanedResponse = cleanAIResponse(response)
                        Result.success(cleanedResponse)
                    }
                } else {
                    Log.e("AIRepository", "Chat message failed: ${result.exceptionOrNull()?.message}")
                    
                    // Try fallback method before giving up
                    Log.d("AIRepository", "Primary method failed, trying fallback chat response")
                    val fallbackResult = tryFallbackChatResponse(message)
                    
                    if (fallbackResult.isSuccess) {
                        fallbackResult
                    } else {
                        // Both methods failed, provide specific error message
                        val errorMessage = result.exceptionOrNull()?.message ?: "Failed to process message"
                        
                        val specificErrorMessage = when {
                            errorMessage.contains("API_KEY_INVALID") -> "AI service temporarily unavailable. Please try again later."
                            errorMessage.contains("QUOTA_EXCEEDED") -> "AI service is currently busy. Please try again in a moment."
                            errorMessage.contains("PERMISSION_DENIED") -> "AI service access denied. Please try again later."
                            errorMessage.contains("UNAVAILABLE") -> "AI service is temporarily down. Please try again later."
                            errorMessage.contains("No internet connection") -> "No internet connection. Please check your network and try again."
                            errorMessage.contains("Empty response") -> "AI returned an empty response. Please try rephrasing your message."
                            errorMessage.contains("timeout") -> "Request timed out. Please try again."
                            else -> "Unable to process your message. Please try again or rephrase your question."
                        }
                        
                        Result.failure(Exception(specificErrorMessage))
                    }
                }
                
            } catch (e: Exception) {
                Log.e("AIRepository", "Exception in sendChatMessage", e)
                
                // Try fallback method as last resort
                Log.d("AIRepository", "Exception occurred, trying fallback as last resort")
                val fallbackResult = tryFallbackChatResponse(message)
                
                if (fallbackResult.isSuccess) {
                    fallbackResult
                } else {
                    val specificErrorMessage = when {
                        e.message?.contains("No internet connection") == true -> "No internet connection. Please check your network and try again."
                        e.message?.contains("timeout") == true -> "Request timed out. Please try again."
                        e.message?.contains("SSL") == true -> "Network security error. Please check your connection and try again."
                        else -> "An unexpected error occurred. Please try again."
                    }
                    Result.failure(Exception(specificErrorMessage))
                }
            }
        }
    }
    
    /**
     * Try fallback chat response method
     */
    private suspend fun tryFallbackChatResponse(message: String): Result<String> {
        return try {
            Log.d("AIRepository", "Attempting fallback chat response")
            val fallbackResult = geminiService.generateSimpleChatResponse(message)
            
            if (fallbackResult.isSuccess) {
                // Increment usage count for successful fallback
                incrementUsageCount()
                Log.d("AIRepository", "Fallback chat response successful")
                // Clean the fallback response as well
                val response = fallbackResult.getOrNull()
                if (response != null) {
                    val cleanedResponse = cleanAIResponse(response)
                    Result.success(cleanedResponse)
                } else {
                    fallbackResult
                }
            } else {
                Log.e("AIRepository", "Fallback chat response also failed: ${fallbackResult.exceptionOrNull()?.message}")
                fallbackResult
            }
        } catch (e: Exception) {
            Log.e("AIRepository", "Exception in fallback chat response: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * Clean AI response by removing unwanted prefixes and formatting
     */
    private fun cleanAIResponse(response: String): String {
        var cleaned = response.trim()
        
        // List of prefixes to remove (case insensitive)
        val prefixesToRemove = listOf(
            "Answer:", "Response:", "Result:", "Output:", "Reply:",
            "ANSWER:", "RESPONSE:", "RESULT:", "OUTPUT:", "REPLY:",
            "Here's the answer:", "Here is the answer:", "The answer is:",
            "Answer -", "Response -", "Result -", "Output -",
            "A:", "R:", "Ans:", "Resp:",
            "IMPORTANT:", "NOTE:", "WARNING:", "INFO:",
            "Important:", "Note:", "Warning:", "Info:",
            "Here's your", "Here is your", "The result is:",
            "Based on your request:", "In response to your query:",
            "To answer your question:", "In answer to your question:",
            "Sure, here's", "Sure! Here's", "Certainly, here's", "Of course, here's",
            "I'll help you", "I can help you", "Let me help you",
            "Here's what I found:", "Here's what you need:",
            "The converted text is:", "The conversion is:",
            "Converted:", "Translation:", "TRANSLATION:",
            "Here's the converted version:", "Here's the translation:",
            "As requested:", "As you requested:", "Per your request:",
            "Following your instructions:", "According to your request:",
            "Here you go:", "There you go:", "Here it is:",
            "The corrected version is:", "The corrected text is:",
            "Corrected:", "CORRECTED:", "Fixed:", "FIXED:",
            "The rewritten text is:", "Rewritten:", "REWRITTEN:",
            "Summary:", "SUMMARY:", "The summary is:",
            "Formal version:", "FORMAL VERSION:", "Made formal:",
            "Creative version:", "CREATIVE VERSION:", "Creatively rewritten:"
        )
        
        // Remove prefixes
        for (prefix in prefixesToRemove) {
            if (cleaned.startsWith(prefix, ignoreCase = true)) {
                cleaned = cleaned.substring(prefix.length).trim()
                break // Only remove the first matching prefix
            }
        }
        
        // Remove leading dashes, colons, quotes, or other punctuation that might be left
        while (cleaned.startsWith("-") || cleaned.startsWith(":") || cleaned.startsWith("•") || 
               cleaned.startsWith("\"") || cleaned.startsWith("'") || cleaned.startsWith(">") ||
               cleaned.startsWith("*") || cleaned.startsWith("~")) {
            cleaned = cleaned.substring(1).trim()
        }
        
        // Remove markdown formatting and other unwanted formatting
        cleaned = cleaned.replace("**", "").replace("*", "").replace("~~", "")
        cleaned = cleaned.replace("```", "").replace("`", "")
        
        // Remove prompt engineering instructions that might leak through
        val instructionPatterns = listOf(
            "Respond directly without prefixes",
            "Do not include prefixes",
            "No explanatory text",
            "Just provide the direct result",
            "Return ONLY the",
            "ONLY return the",
            "Don't add any explanation",
            "No additional text",
            "Direct response only",
            "Provide only the result"
        )
        
        for (pattern in instructionPatterns) {
            if (cleaned.contains(pattern, ignoreCase = true)) {
                // Find and remove the entire instruction sentence
                val sentences = cleaned.split(". ", ". \n", ".\n", "\n")
                cleaned = sentences.filter { 
                    !it.contains(pattern, ignoreCase = true) 
                }.joinToString(". ").trim()
                if (cleaned.endsWith(".")) cleaned = cleaned.dropLast(1)
                break
            }
        }
        
        // Remove any remaining leading/trailing whitespace and punctuation
        cleaned = cleaned.trim().removeSuffix(".").removeSuffix(":").trim()
        
        Log.d("AIRepository", "Original response length: ${response.length}, Cleaned length: ${cleaned.length}")
        Log.d("AIRepository", "Cleaning result: '${response.take(100)}...' -> '${cleaned.take(100)}...'")
        
        return cleaned.ifEmpty { response } // Fallback to original if cleaning results in empty string
    }
    
    private fun incrementUsageCount() {
        val today = getCurrentDateString()
        val lastUsageDate = sharedPreferences.getString("last_usage_date", "")
        
        val currentCount = if (lastUsageDate == today) {
            sharedPreferences.getInt("usage_count", 0)
        } else {
            0
        }
        
        sharedPreferences.edit()
            .putString("last_usage_date", today)
            .putInt("usage_count", currentCount + 1)
            .apply()
    }
    
    private fun getCurrentDateString(): String {
        return java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            .format(java.util.Date())
    }
    
    /**
     * API Key management - now hardcoded with Gemini
     */
    fun getApiKey(): String {
        return "CONFIGURED" // Always return configured since API key is hardcoded
    }
    
    fun setApiKey(apiKey: String) {
        // No-op since API key is hardcoded in GeminiService
    }
    
    /**
     * Pro user status
     */
    fun isProUser(): Boolean {
        return sharedPreferences.getBoolean("is_pro_user", false)
    }
    
    fun setProUser(isPro: Boolean) {
        sharedPreferences.edit()
            .putBoolean("is_pro_user", isPro)
            .apply()
    }
    
    /**
     * Get response mode setting
     */
    private fun getResponseMode(): String {
        val settingsPrefs = context.getSharedPreferences("neoboard_settings", Context.MODE_PRIVATE)
        return settingsPrefs.getString("response_mode", "normal") ?: "normal"
    }
    
    companion object {
        const val FREE_TIER_DAILY_LIMIT = 15
    }
} 