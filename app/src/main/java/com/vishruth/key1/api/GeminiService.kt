package com.vishruth.key1.api

import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import com.vishruth.key1.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

/**
 * Service for Gemini AI integration using Google's Generative AI SDK
 * 
 * MIGRATION NOTES (December 2024):
 * - Updated from deprecated Gemini 1.5 models to new Gemini 2.0 models
 * - Gemini 1.5 Pro 001 and Flash 001 are already discontinued  
 * - Gemini 1.5 Pro 002 and Flash 002 will be discontinued September 24, 2025
 * - Primary model: gemini-2.0-flash-exp (latest experimental Gemini 2.0)
 * - Fallback models prioritize Gemini 2.0 variants for best performance
 * - Legacy 1.5 models kept only as final fallbacks until full deprecation
 */
class GeminiService {
    
    companion object {
        // Primary API key - use this first (secured in BuildConfig)
        private val PRIMARY_API_KEY = BuildConfig.GEMINI_PRIMARY_API_KEY.takeIf { it.isNotEmpty() } 
            ?: throw IllegalStateException("GEMINI_PRIMARY_API_KEY not configured in local.properties")
        
        // Backup API keys - only use if primary fails (secured in BuildConfig)
        private val BACKUP_API_KEYS = listOf(
            BuildConfig.GEMINI_BACKUP_API_KEY_1,
            BuildConfig.GEMINI_BACKUP_API_KEY_2
        ).filter { it.isNotEmpty() }
        
        private const val MODEL_NAME = "gemini-2.0-flash-exp"
        private const val TAG = "GeminiService"
    }
    
    private val generativeModel = GenerativeModel(
        modelName = MODEL_NAME,
        apiKey = PRIMARY_API_KEY,
        generationConfig = generationConfig {
            temperature = 0.3f  // Lower temperature for more consistent, focused outputs
            topK = 40
            topP = 0.9f         // Slightly lower for more focused responses
            maxOutputTokens = 1024  // Increased for better detailed responses
        }
    )
    
    /**
     * List of fallback model names to try if the primary fails
     * Updated to use Gemini 2.0 models to avoid discontinued 1.5 models
     */
    private val fallbackModels = listOf(
        "gemini-2.0-flash-exp",
        "gemini-2.0-flash",
        "models/gemini-2.0-flash-exp", 
        "models/gemini-2.0-flash",
        "gemini-1.5-flash",  // Keep as final fallback until fully deprecated
        "gemini-1.5-pro"     // Keep as final fallback until fully deprecated
    )
    
    private fun createModelWithName(modelName: String, apiKey: String, isNormalMode: Boolean = true): GenerativeModel {
        return GenerativeModel(
            modelName = modelName,
            apiKey = apiKey,
            generationConfig = generationConfig {
                temperature = 0.3f  // Lower temperature for more consistent, focused outputs
                topK = 40
                topP = 0.9f         // Slightly lower for more focused responses
                maxOutputTokens = if (isNormalMode) 256 else 512  // Normal mode uses fewer tokens
            }
        )
    }
    
    /**
     * Generate content using Gemini model with API key fallback support
     */
    suspend fun generateContent(prompt: String, inputText: String, isNormalMode: Boolean = true): Result<String> {
        return withContext(Dispatchers.IO) {
            var lastException: Exception? = null
            var attemptCount = 0
            val maxAttempts = 3 // Retry up to 3 times for transient errors
            
            // Prepare all API keys to try (primary first, then backups)
            val allApiKeys = listOf(PRIMARY_API_KEY) + BACKUP_API_KEYS
            
            // Try each API key
            for ((keyIndex, apiKey) in allApiKeys.withIndex()) {
                val keyType = if (keyIndex == 0) "PRIMARY" else "BACKUP ${keyIndex}"
                Log.d(TAG, "Trying $keyType API key")
                
                // Try all model names with current API key
                for (modelName in fallbackModels) {
                    var currentAttempt = 0
                    
                    while (currentAttempt < maxAttempts) {
                        try {
                            currentAttempt++
                            attemptCount++
                            Log.d(TAG, "Attempt $currentAttempt/$maxAttempts - Trying model: $modelName with $keyType key")
                            
                            // Log when using new Gemini 2.0 models
                            if (modelName.contains("2.0")) {
                                Log.i(TAG, "🚀 Using new Gemini 2.0 model: $modelName")
                            } else {
                                Log.w(TAG, "⚠️  Using legacy model: $modelName (consider upgrading)")
                            }
                            
                            val model = createModelWithName(modelName, apiKey, isNormalMode)
                            
                            // Create a direct prompt without imposing identity
                            val directPrompt = if (isNormalMode) {
                                """$prompt

User Input: $inputText

CRITICAL OUTPUT REQUIREMENTS:
- Output ONLY the converted/processed text
- NO prefixes like "Answer:", "Response:", "Result:", "Converted:", "Translation:", "Corrected:", "Summary:", etc.
- NO explanatory sentences, instructions, or commentary
- NO formatting markers, quotes, or extra punctuation
- NO phrases like "Here's", "Sure,", "I'll help you", "The result is", etc.
- Start immediately with the actual content
- Keep response concise and under 300 words
- Focus on key points only"""
                            } else {
                                """$prompt

User Input: $inputText

CRITICAL OUTPUT REQUIREMENTS:
- Output ONLY the converted/processed text
- NO prefixes like "Answer:", "Response:", "Result:", "Converted:", "Translation:", "Corrected:", "Summary:", etc.
- NO explanatory sentences, instructions, or commentary
- NO formatting markers, quotes, or extra punctuation
- NO phrases like "Here's", "Sure,", "I'll help you", "The result is", etc.
- Start immediately with the actual content"""
                            }
                            
                            // Add timeout protection by using withTimeout
                            val response = withTimeout(30000L) { // 30 second timeout
                                model.generateContent(directPrompt)
                            }
                            val result = response.text
                            
                            if (result != null && result.isNotBlank()) {
                                val trimmedResult = result.trim()
                                // Additional validation to ensure quality response
                                if (trimmedResult.length >= 3) { // Minimum reasonable response length
                                    Log.d(TAG, "SUCCESS with $keyType key and model $modelName (attempt $currentAttempt)")
                                    return@withContext Result.success(trimmedResult)
                                } else {
                                    Log.w(TAG, "Response too short from $modelName with $keyType key: '$trimmedResult'")
                                    lastException = Exception("Response too short from AI model")
                                }
                            } else {
                                Log.w(TAG, "Empty response from $modelName with $keyType key")
                                lastException = Exception("Empty response from AI model")
                            }
                            
                        } catch (e: Exception) {
                            Log.w(TAG, "$keyType key with model $modelName failed (attempt $currentAttempt): ${e.message}")
                            lastException = e
                            
                            // Check if it's an API key related error
                            val isApiKeyError = e.message?.contains("API_KEY_INVALID") == true ||
                                              e.message?.contains("PERMISSION_DENIED") == true ||
                                              e.message?.contains("QUOTA_EXCEEDED") == true
                            
                            // Check if it's a transient error that we should retry
                            val isTransientError = e.message?.contains("UNAVAILABLE") == true ||
                                                 e.message?.contains("timeout") == true ||
                                                 e.message?.contains("DEADLINE_EXCEEDED") == true ||
                                                 e.message?.contains("INTERNAL") == true
                            
                            // If it's a model not found error, try next model with same key
                            if (e.message?.contains("not found") == true || 
                                e.message?.contains("not supported") == true) {
                                break // Break from retry loop to try next model
                            }
                            
                            // If it's an API key error and we're on primary key, try backup keys
                            if (isApiKeyError && keyIndex == 0) {
                                Log.w(TAG, "Primary API key failed with error: ${e.message}. Trying backup keys...")
                                break // Break from retry loop and model loop to try next API key
                            }
                            
                            // If it's an API key error on backup keys, try next backup
                            if (isApiKeyError && keyIndex > 0) {
                                Log.w(TAG, "Backup API key $keyIndex failed: ${e.message}")
                                break // Break from retry loop and model loop to try next API key
                            }
                            
                            // For transient errors, retry with same model/key
                            if (isTransientError && currentAttempt < maxAttempts) {
                                Log.d(TAG, "Transient error detected, retrying in 1 second...")
                                delay(1000) // Wait 1 second before retry
                                continue
                            }
                            
                            // For other errors, try next model
                            break // Break from retry loop to try next model
                        }
                    }
                }
                
                // If we successfully tried all models with this key but got no success,
                // and it's not an API key error, don't try other keys
                if (keyIndex == 0 && lastException != null) {
                    val isApiKeyError = lastException!!.message?.contains("API_KEY_INVALID") == true ||
                                      lastException!!.message?.contains("PERMISSION_DENIED") == true ||
                                      lastException!!.message?.contains("QUOTA_EXCEEDED") == true
                    
                    if (!isApiKeyError) {
                        Log.d(TAG, "Primary key failed with non-API-key error. Not trying backups.")
                        break
                    }
                }
            }
            
            // All API keys and models failed
            Log.e(TAG, "All API keys and models failed after $attemptCount attempts", lastException)
            
            // Provide specific error message based on the last exception
            val specificErrorMessage = when {
                lastException?.message?.contains("API_KEY_INVALID") == true -> "API_KEY_INVALID"
                lastException?.message?.contains("QUOTA_EXCEEDED") == true -> "QUOTA_EXCEEDED" 
                lastException?.message?.contains("PERMISSION_DENIED") == true -> "PERMISSION_DENIED"
                lastException?.message?.contains("UNAVAILABLE") == true -> "UNAVAILABLE"
                lastException?.message?.contains("timeout") == true -> "Request timed out after multiple attempts"
                lastException?.message?.contains("Empty response") == true -> "Empty response"
                lastException?.message?.contains("Response too short") == true -> "Response too short"
                else -> "All available API keys and models failed after $attemptCount attempts"
            }
            
            Result.failure(Exception(specificErrorMessage))
        }
    }
    
    /**
     * Simple text generation with just the prompt and API key fallback support
     * Used as a fallback for chat messages when the main generateContent fails
     */
    suspend fun generateSimpleChatResponse(prompt: String): Result<String> {
        return withContext(Dispatchers.IO) {
            var lastException: Exception? = null
            
            // Prepare all API keys to try (primary first, then backups)
            val allApiKeys = listOf(PRIMARY_API_KEY) + BACKUP_API_KEYS
            
            // Try each API key
            for ((keyIndex, apiKey) in allApiKeys.withIndex()) {
                val keyType = if (keyIndex == 0) "PRIMARY" else "BACKUP ${keyIndex}"
                
                try {
                    Log.d(TAG, "Starting simple chat response with $keyType key")
                    
                    val model = GenerativeModel(
                        modelName = MODEL_NAME,
                        apiKey = apiKey,
                        generationConfig = generationConfig {
                            temperature = 0.5f  // Slightly higher temperature for more natural conversation
                            topK = 40
                            topP = 0.9f
                            maxOutputTokens = 512  // Adequate for chat responses
                        }
                    )
                    
                    val enhancedPrompt = """You are a helpful AI assistant. Please respond directly to the user's message.

User's message: $prompt

IMPORTANT INSTRUCTIONS:
- Respond directly without prefixes like "Answer:", "Response:", or "Here's the answer:"
- Do not add explanatory text before or after your response
- Keep your response conversational and informative
- Provide only the direct, helpful response"""
                    
                    val response = withTimeout(25000L) { // 25 second timeout for chat
                        model.generateContent(enhancedPrompt)
                    }
                    val result = response.text
                    
                    if (result != null && result.isNotBlank()) {
                        val trimmedResult = result.trim()
                        if (trimmedResult.length >= 3) {
                            Log.d(TAG, "Simple chat response successful with $keyType key")
                            return@withContext Result.success(trimmedResult)
                        } else {
                            Log.w(TAG, "Chat response too short with $keyType key: '$trimmedResult'")
                            lastException = Exception("Response too short")
                        }
                    } else {
                        Log.w(TAG, "Empty chat response from $keyType key")
                        lastException = Exception("Empty response from AI")
                    }
                    
                } catch (e: Exception) {
                    Log.w(TAG, "Simple chat response failed with $keyType key: ${e.message}")
                    lastException = e
                    
                    // Check if it's an API key related error
                    val isApiKeyError = e.message?.contains("API_KEY_INVALID") == true ||
                                      e.message?.contains("PERMISSION_DENIED") == true ||
                                      e.message?.contains("QUOTA_EXCEEDED") == true
                    
                    // If it's an API key error, try next key
                    if (isApiKeyError) {
                        Log.w(TAG, "$keyType key failed with API error: ${e.message}. Trying next key...")
                        continue
                    }
                    
                    // For non-API-key errors on primary key, don't try backups
                    if (keyIndex == 0) {
                        Log.d(TAG, "Primary key failed with non-API-key error. Not trying backups.")
                        break
                    }
                }
            }
            
            // All API keys failed
            Log.e(TAG, "All API keys failed for simple chat response", lastException)
            val specificErrorMessage = when {
                lastException?.message?.contains("API_KEY_INVALID") == true -> "API_KEY_INVALID"
                lastException?.message?.contains("QUOTA_EXCEEDED") == true -> "QUOTA_EXCEEDED"
                lastException?.message?.contains("PERMISSION_DENIED") == true -> "PERMISSION_DENIED"
                lastException?.message?.contains("UNAVAILABLE") == true -> "UNAVAILABLE"
                lastException?.message?.contains("timeout") == true -> "Request timed out"
                else -> "All API keys failed for chat"
            }
            Result.failure(Exception(specificErrorMessage))
        }
    }
} 