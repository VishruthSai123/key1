package com.vishruth.key1.api

import android.util.Log
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

/**
 * Service for ChatGPT-5 AI integration using AI/ML API
 * 
 * This service provides access to the latest ChatGPT-5 model through the AI/ML API
 * offering advanced reasoning capabilities and latest AI features.
 */
class GPT5Service {
    
    companion object {
        private const val API_KEY = "d62241486f9f4fd48e76ba31452b30de"
        private const val BASE_URL = "https://api.aimlapi.com/v1"
        private const val MODEL_NAME = "openai/gpt-5-mini-2025-08-07"
        private const val TAG = "GPT5Service"
        private const val REQUEST_TIMEOUT_MS = 30000L // 30 seconds
    }
    
    /**
     * Generate content using ChatGPT-5 with the provided prompt and text
     */
    suspend fun generateContent(prompt: String, text: String, isNormalMode: Boolean): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Generating content with ChatGPT-5")
                Log.d(TAG, "Prompt: ${prompt.take(100)}...")
                Log.d(TAG, "Text: ${text.take(100)}...")
                Log.d(TAG, "Normal mode: $isNormalMode")
                
                val systemMessage = buildSystemPrompt(prompt, isNormalMode)
                val userMessage = text
                
                val response = makeAPIRequest(systemMessage, userMessage)
                
                if (response.isNotBlank()) {
                    Log.d(TAG, "ChatGPT-5 response received: ${response.take(100)}...")
                    Result.success(response)
                } else {
                    Log.w(TAG, "Empty response from ChatGPT-5")
                    Result.failure(Exception("Empty response from ChatGPT-5"))
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error generating content with ChatGPT-5", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Generate simple chat response using ChatGPT-5
     */
    suspend fun generateSimpleChatResponse(message: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Generating simple chat response with ChatGPT-5")
                
                val systemMessage = ""
                val response = makeAPIRequest(systemMessage, message)
                
                if (response.isNotBlank()) {
                    Log.d(TAG, "ChatGPT-5 chat response received: ${response.take(100)}...")
                    Result.success(response)
                } else {
                    Log.w(TAG, "Empty chat response from ChatGPT-5")
                    Result.failure(Exception("Empty response from ChatGPT-5"))
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error generating chat response with ChatGPT-5", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Build system prompt based on the action and mode
     */
    private fun buildSystemPrompt(prompt: String, isNormalMode: Boolean): String {
        val modeInstruction = if (isNormalMode) {
            "Provide a comprehensive, detailed response."
        } else {
            "Provide a concise, direct response."
        }
        
        return """$prompt

$modeInstruction

CRITICAL OUTPUT REQUIREMENTS:
- NO prefixes like "Answer:", "Response:", "Sure,", "Here's", "I'll help", etc.
- NO explanatory text, instructions, or meta-commentary  
- NO format descriptions or process explanations
- Start immediately with the actual response content
- Provide only the direct, helpful response"""
    }
    
    /**
     * Make API request to ChatGPT-5 via AI/ML API
     */
    private suspend fun makeAPIRequest(systemMessage: String, userMessage: String): String {
        return withTimeout(REQUEST_TIMEOUT_MS) {
            try {
                val url = URL("$BASE_URL/chat/completions")
                val connection = url.openConnection() as HttpURLConnection
                
                // Set up connection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("Authorization", "Bearer $API_KEY")
                connection.doOutput = true
                connection.connectTimeout = 10000 // 10 seconds
                connection.readTimeout = 20000 // 20 seconds
                
                // Build request body
                val requestBody = JSONObject().apply {
                    put("model", MODEL_NAME)
                    put("messages", JSONArray().apply {
                        // Only add system message if it's not empty
                        if (systemMessage.isNotBlank()) {
                            put(JSONObject().apply {
                                put("role", "system")
                                put("content", systemMessage)
                            })
                        }
                        put(JSONObject().apply {
                            put("role", "user")
                            put("content", userMessage)
                        })
                    })
                    put("max_tokens", 1024)
                    put("temperature", 0.3)
                    put("top_p", 0.9)
                }
                
                Log.d(TAG, "Sending request to ChatGPT-5 API")
                
                // Send request
                val outputStream = DataOutputStream(connection.outputStream)
                outputStream.write(requestBody.toString().toByteArray(StandardCharsets.UTF_8))
                outputStream.flush()
                outputStream.close()
                
                // Read response
                val responseCode = connection.responseCode
                Log.d(TAG, "ChatGPT-5 API response code: $responseCode")
                
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val inputStream = BufferedReader(InputStreamReader(connection.inputStream))
                    val response = StringBuilder()
                    var line: String?
                    
                    while (inputStream.readLine().also { line = it } != null) {
                        response.append(line)
                    }
                    inputStream.close()
                    
                    // Parse response
                    val responseJson = JSONObject(response.toString())
                    val choices = responseJson.getJSONArray("choices")
                    
                    if (choices.length() > 0) {
                        val message = choices.getJSONObject(0).getJSONObject("message")
                        val content = message.getString("content").trim()
                        
                        Log.d(TAG, "Successfully parsed ChatGPT-5 response")
                        return@withTimeout content
                    } else {
                        throw Exception("No choices in ChatGPT-5 response")
                    }
                } else {
                    // Read error response
                    val errorStream = connection.errorStream
                    if (errorStream != null) {
                        val errorReader = BufferedReader(InputStreamReader(errorStream))
                        val errorResponse = StringBuilder()
                        var line: String?
                        
                        while (errorReader.readLine().also { line = it } != null) {
                            errorResponse.append(line)
                        }
                        errorReader.close()
                        
                        Log.e(TAG, "ChatGPT-5 API error response: $errorResponse")
                        throw Exception("ChatGPT-5 API error: $responseCode - $errorResponse")
                    } else {
                        throw Exception("ChatGPT-5 API error: $responseCode")
                    }
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error making ChatGPT-5 API request", e)
                when {
                    e.message?.contains("timeout") == true -> 
                        throw Exception("Request timed out. Please try again.")
                    e.message?.contains("UnknownHost") == true -> 
                        throw Exception("Network error. Please check your internet connection.")
                    e.message?.contains("401") == true -> 
                        throw Exception("API authentication failed.")
                    e.message?.contains("429") == true -> 
                        throw Exception("Rate limit exceeded. Please try again later.")
                    e.message?.contains("500") == true -> 
                        throw Exception("ChatGPT-5 service temporarily unavailable.")
                    else -> throw e
                }
            }
        }
    }
}
