package com.vishruth.key1.repository

import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.vishruth.key1.api.GeminiService
import com.vishruth.key1.api.GPT5Service
import com.vishruth.key1.data.AIAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Central repository for managing AI operations and model selection
 * Handles both Gemini and GPT-5 services with user preference switching
 */
class AIRepository(private val context: Context) {
    private val sharedPreferences: SharedPreferences = 
        context.getSharedPreferences("sendright_prefs", Context.MODE_PRIVATE)
    
    private val geminiService = GeminiService()
    private val gpt5Service = GPT5Service()
    
    /**
     * Execute an AI action using the selected model (Gemini or GPT-5)
     */
    suspend fun executeAIAction(action: AIAction, text: String, isNormalMode: Boolean): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                // Check network connectivity first
                if (!isNetworkAvailable()) {
                    Log.e("AIRepository", "No network connection available")
                    return@withContext Result.failure(Exception("No internet connection"))
                }
                
                if (text.isBlank()) {
                    Log.e("AIRepository", "Empty text provided for AI action")
                    return@withContext Result.failure(Exception("Please provide text to process"))
                }
                
                // Check usage limits for free users
                if (!isProUser() && !canUserPerformAction()) {
                    Log.w("AIRepository", "User has reached daily limit")
                    return@withContext Result.failure(Exception("Daily limit reached. Please upgrade for unlimited access."))
                }
                
                // Log which model is being used
                val useGPT5 = isGPT5Enabled()
                val modelName = if (useGPT5) "ChatGPT-5" else "Gemini"
                
                Log.d("AIRepository", "Using $modelName for AI action: ${action.displayName}")
                
                // Execute with selected service using action's prompt
                val result = if (useGPT5) {
                    gpt5Service.generateContent(action.prompt, text, isNormalMode)
                } else {
                    geminiService.generateContent(action.prompt, text, isNormalMode)
                }
                
                if (result.isSuccess) {
                    // Increment usage count for successful generation
                    incrementUsageCount()
                    Log.d("AIRepository", "$modelName action successful")
                    // Clean the response
                    val response = result.getOrNull()
                    if (response != null) {
                        val cleanedResponse = cleanAIResponse(response)
                        Result.success(cleanedResponse)
                    } else {
                        result
                    }
                } else {
                    Log.e("AIRepository", "$modelName action failed: ${result.exceptionOrNull()?.message}")
                    result
                }
            } catch (e: Exception) {
                Log.e("AIRepository", "Exception in executeAIAction: ${e.message}")
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
    
    /**
     * Send a chat message using the selected AI model
     */
    suspend fun sendChatMessage(message: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                // Check network connectivity
                if (!isNetworkAvailable()) {
                    Log.e("AIRepository", "No network connection available")
                    return@withContext Result.failure(Exception("No internet connection"))
                }
                
                // Choose AI service based on user preference
                val useGPT5 = isGPT5Enabled()
                val modelName = if (useGPT5) "ChatGPT-5" else "Gemini"
                
                Log.d("AIRepository", "Using $modelName for chat message")
                
                // Get response mode for chat
                val responseMode = getResponseMode()
                
                // Add system context based on response mode
                val enhancedMessage = when (responseMode) {
                    "formal" -> "Please respond in a professional and formal manner: $message"
                    "casual" -> "Please respond in a friendly and casual manner: $message"
                    "creative" -> "Please respond creatively and imaginatively: $message"
                    else -> message // normal mode
                }
                
                // Check usage limits for free users
                if (!isProUser() && !canUserPerformAction()) {
                    Log.w("AIRepository", "User has reached daily limit")
                    return@withContext Result.failure(Exception("Daily limit reached. Please upgrade for unlimited access."))
                }
                
                // Execute with selected service
                val result = if (useGPT5) {
                    gpt5Service.generateContent("", enhancedMessage, true)
                } else {
                    geminiService.generateContent("", enhancedMessage, true)
                }
                
                if (result.isSuccess) {
                    // Increment usage count for successful generation
                    incrementUsageCount()
                    Log.d("AIRepository", "$modelName chat successful")
                    // Clean the response
                    val response = result.getOrNull()
                    if (response != null) {
                        val cleanedResponse = cleanAIResponse(response)
                        Result.success(cleanedResponse)
                    } else {
                        result
                    }
                } else {
                    Log.e("AIRepository", "$modelName chat failed: ${result.exceptionOrNull()?.message}")
                    result
                }
            } catch (e: Exception) {
                Log.e("AIRepository", "Exception in sendChatMessage: ${e.message}")
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
    
    /**
     * GPT-5 model preference management
     */
    fun isGPT5Enabled(): Boolean {
        return sharedPreferences.getBoolean("gpt5_enabled", false)
    }
    
    fun setGPT5Enabled(enabled: Boolean) {
        sharedPreferences.edit()
            .putBoolean("gpt5_enabled", enabled)
            .apply()
    }
    
    /**
     * Clean AI response by removing common prefixes and ensuring proper formatting
     */
    private fun cleanAIResponse(response: String): String {
        return response
            .replace("Here's the corrected text:", "")
            .replace("Here's the improved version:", "")
            .replace("Here's the formal version:", "")
            .replace("Here's the creative version:", "")
            .replace("Here's the summary:", "")
            .replace("Here's the translation:", "")
            .replace("Translation:", "")
            .replace("Corrected:", "")
            .replace("Improved:", "")
            .replace("Summary:", "")
            .replace("Answer:", "")
            .replace("Response:", "")
            .trim()
    }
    
    /**
     * Check if network is available
     */
    private fun isNetworkAvailable(): Boolean {
        try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = connectivityManager.activeNetwork ?: return false
            val networkCapabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            
            return networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                   networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        } catch (e: Exception) {
            Log.e("AIRepository", "Error checking network availability: ${e.message}")
            return false
        }
    }
    
    /**
     * Usage tracking for free tier limitations
     */
    private fun incrementUsageCount() {
        val today = getCurrentDateString()
        val currentCount = sharedPreferences.getInt("usage_count_$today", 0)
        sharedPreferences.edit()
            .putInt("usage_count_$today", currentCount + 1)
            .apply()
    }
    
    private fun canUserPerformAction(): Boolean {
        val today = getCurrentDateString()
        val currentCount = sharedPreferences.getInt("usage_count_$today", 0)
        return currentCount < FREE_TIER_DAILY_LIMIT
    }
    
    private fun getCurrentDateString(): String {
        val calendar = java.util.Calendar.getInstance()
        return "${calendar.get(java.util.Calendar.YEAR)}-${calendar.get(java.util.Calendar.MONTH)}-${calendar.get(java.util.Calendar.DAY_OF_MONTH)}"
    }
    
    fun getTodayUsageCount(): Int {
        val today = getCurrentDateString()
        return sharedPreferences.getInt("usage_count_$today", 0)
    }
    
    fun getRemainingUsageCount(): Int {
        if (isProUser()) return -1 // Unlimited for pro users
        val used = getTodayUsageCount()
        return maxOf(0, FREE_TIER_DAILY_LIMIT - used)
    }
    
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
        val settingsPrefs = context.getSharedPreferences("sendright_settings", Context.MODE_PRIVATE)
        return settingsPrefs.getString("response_mode", "normal") ?: "normal"
    }
    
    companion object {
        const val FREE_TIER_DAILY_LIMIT = 15
    }
}
