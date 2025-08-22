package com.vishruth.key1.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.vishruth.key1.data.ChatConversation
import com.vishruth.key1.data.ChatMessage
import com.vishruth.key1.data.ChatSummary
import com.vishruth.key1.data.MessageType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

/**
 * Repository for managing chat conversations with persistence and memory
 */
class ChatRepository(private val context: Context) {
    
    private val sharedPreferences: SharedPreferences = 
        context.getSharedPreferences("chat_storage", Context.MODE_PRIVATE)
    
    private val aiRepository = AIRepository(context)
    private val json = Json { ignoreUnknownKeys = true }
    
    companion object {
        private const val TAG = "ChatRepository"
        private const val CURRENT_CONVERSATION_KEY = "current_conversation"
        private const val CONVERSATIONS_LIST_KEY = "conversations_list"
        private const val MAX_CONTEXT_MESSAGES = 10 // Last 10 messages for context
        private const val SUMMARIZE_THRESHOLD = 19 // Summarize after 19 messages
    }
    
    /**
     * Get the current active conversation
     */
    fun getCurrentConversation(): ChatConversation? {
        return try {
            val conversationJson = sharedPreferences.getString(CURRENT_CONVERSATION_KEY, null)
            conversationJson?.let { json.decodeFromString<ChatConversation>(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading current conversation", e)
            null
        }
    }
    
    /**
     * Save the current conversation
     */
    fun saveCurrentConversation(conversation: ChatConversation) {
        try {
            val conversationJson = json.encodeToString(conversation)
            sharedPreferences.edit()
                .putString(CURRENT_CONVERSATION_KEY, conversationJson)
                .apply()
            
            // Also save to conversations list
            saveToConversationsList(conversation)
            Log.d(TAG, "Conversation saved with ${conversation.messages.size} messages")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving conversation", e)
        }
    }
    
    /**
     * Create a new conversation
     */
    fun createNewConversation(): ChatConversation {
        val conversation = ChatConversation(
            title = generateConversationTitle(),
            messages = listOf(
                ChatMessage(
                    content = "Hello! I'm ready to help with any questions or tasks you have. What can I assist you with today?",
                    isFromUser = false,
                    messageType = MessageType.SYSTEM
                )
            )
        )
        saveCurrentConversation(conversation)
        return conversation
    }
    
    /**
     * Add a message to the current conversation
     */
    fun addMessage(message: ChatMessage): ChatConversation {
        val currentConversation = getCurrentConversation() ?: createNewConversation()
        val updatedMessages = currentConversation.messages + message
        
        val updatedConversation = currentConversation.copy(
            messages = updatedMessages,
            updatedAt = System.currentTimeMillis(),
            messageCount = updatedMessages.size
        )
        
        saveCurrentConversation(updatedConversation)
        return updatedConversation
    }
    
    /**
     * Get conversation context for AI (recent messages + summary if available)
     */
    fun getConversationContext(conversation: ChatConversation): String {
        val recentMessages = conversation.messages
            .filter { it.messageType == MessageType.TEXT }
            .takeLast(MAX_CONTEXT_MESSAGES)
        
        val contextBuilder = StringBuilder()
        
        // Add summary if available
        conversation.summary?.let { summary ->
            contextBuilder.append("Previous conversation summary: $summary\n\n")
        }
        
        // Add recent message history
        if (recentMessages.isNotEmpty()) {
            contextBuilder.append("Recent conversation history:\n")
            recentMessages.forEach { message ->
                val role = if (message.isFromUser) "User" else "Assistant"
                contextBuilder.append("$role: ${message.content}\n")
            }
            contextBuilder.append("\n")
        }
        
        return contextBuilder.toString()
    }
    
    /**
     * Send a message with conversation context
     */
    suspend fun sendMessageWithContext(userMessage: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val conversation = getCurrentConversation() ?: createNewConversation()
                
                // Add user message first
                val userChatMessage = ChatMessage(
                    content = userMessage,
                    isFromUser = true,
                    conversationId = conversation.id
                )
                val updatedConversation = addMessage(userChatMessage)
                
                // Get conversation context
                val context = getConversationContext(updatedConversation)
                
                // Create context-aware prompt
                val contextualPrompt = buildString {
                    append(context)
                    append("Current user message: $userMessage\n\n")
                    append("Please respond naturally considering the conversation context above. ")
                    append("Keep your response conversational and relevant to our ongoing discussion.")
                }
                
                Log.d(TAG, "Sending message with context. Context length: ${context.length}")
                
                // Send to AI with context
                val result = aiRepository.sendChatMessage(contextualPrompt)
                
                result.onSuccess { response ->
                    // Add AI response to conversation
                    val aiMessage = ChatMessage(
                        content = response,
                        isFromUser = false,
                        conversationId = conversation.id
                    )
                    addMessage(aiMessage)
                    
                    // Check if we need to summarize
                    checkAndSummarizeIfNeeded(updatedConversation)
                }
                
                result
                
            } catch (e: Exception) {
                Log.e(TAG, "Error sending message with context", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Check if conversation needs summarization and perform it
     */
    private suspend fun checkAndSummarizeIfNeeded(conversation: ChatConversation) {
        if (conversation.messages.size >= SUMMARIZE_THRESHOLD && conversation.summary == null) {
            Log.d(TAG, "Conversation has ${conversation.messages.size} messages, starting summarization")
            summarizeConversation(conversation)
        }
    }
    
    /**
     * Summarize the conversation
     */
    suspend fun summarizeConversation(conversation: ChatConversation): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val messages = conversation.messages
                    .filter { it.messageType == MessageType.TEXT }
                    .take(15) // Summarize first 15 messages to keep context manageable
                
                if (messages.size < 5) {
                    return@withContext Result.success("Not enough messages to summarize")
                }
                
                val conversationText = messages.joinToString("\n") { message ->
                    val role = if (message.isFromUser) "User" else "Assistant"
                    "$role: ${message.content}"
                }
                
                val summaryPrompt = """
                    Please provide a concise summary of this conversation between a user and an AI assistant. 
                    Focus on the main topics discussed, key questions asked, and important information shared.
                    Keep the summary under 150 words and make it useful for providing context in future conversations.
                    
                    Conversation to summarize:
                    $conversationText
                    
                    Summary:
                """.trimIndent()
                
                val result = aiRepository.sendChatMessage(summaryPrompt)
                
                result.onSuccess { summary ->
                    // Update conversation with summary
                    val updatedConversation = conversation.copy(
                        summary = summary.trim(),
                        updatedAt = System.currentTimeMillis()
                    )
                    saveCurrentConversation(updatedConversation)
                    
                    // Add summary message to conversation
                    val summaryMessage = ChatMessage(
                        content = "Conversation Summary: $summary",
                        isFromUser = false,
                        messageType = MessageType.SUMMARY,
                        conversationId = conversation.id
                    )
                    addMessage(summaryMessage)
                    
                    Log.d(TAG, "Conversation summarized successfully")
                }
                
                result
                
            } catch (e: Exception) {
                Log.e(TAG, "Error summarizing conversation", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Get all conversations
     */
    fun getAllConversations(): List<ChatConversation> {
        return try {
            val conversationsJson = sharedPreferences.getString(CONVERSATIONS_LIST_KEY, "[]")
            json.decodeFromString<List<ChatConversation>>(conversationsJson ?: "[]")
        } catch (e: Exception) {
            Log.e(TAG, "Error loading conversations list", e)
            emptyList()
        }
    }
    
    /**
     * Save conversation to the list
     */
    private fun saveToConversationsList(conversation: ChatConversation) {
        try {
            val conversations = getAllConversations().toMutableList()
            val existingIndex = conversations.indexOfFirst { it.id == conversation.id }
            
            if (existingIndex >= 0) {
                conversations[existingIndex] = conversation
            } else {
                conversations.add(conversation)
            }
            
            // Keep only recent 50 conversations
            val limitedConversations = conversations.sortedByDescending { it.updatedAt }.take(50)
            
            val conversationsJson = json.encodeToString(limitedConversations)
            sharedPreferences.edit()
                .putString(CONVERSATIONS_LIST_KEY, conversationsJson)
                .apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error saving to conversations list", e)
        }
    }
    
    /**
     * Delete a conversation
     */
    fun deleteConversation(conversationId: String) {
        try {
            val conversations = getAllConversations().toMutableList()
            conversations.removeAll { it.id == conversationId }
            
            val conversationsJson = json.encodeToString(conversations)
            sharedPreferences.edit()
                .putString(CONVERSATIONS_LIST_KEY, conversationsJson)
                .apply()
            
            // If this was the current conversation, create a new one
            val currentConversation = getCurrentConversation()
            if (currentConversation?.id == conversationId) {
                createNewConversation()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting conversation", e)
        }
    }
    
    /**
     * Load a specific conversation
     */
    fun loadConversation(conversationId: String): ChatConversation? {
        return getAllConversations().find { it.id == conversationId }?.also {
            saveCurrentConversation(it)
        }
    }
    
    /**
     * Generate a conversation title based on content
     */
    private fun generateConversationTitle(): String {
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        val timeOfDay = when (hour) {
            in 5..11 -> "Morning"
            in 12..16 -> "Afternoon"
            in 17..20 -> "Evening"
            else -> "Night"
        }
        return "$timeOfDay Chat"
    }
    
    /**
     * Clear all conversations (for settings/reset)
     */
    fun clearAllConversations() {
        sharedPreferences.edit()
            .remove(CURRENT_CONVERSATION_KEY)
            .remove(CONVERSATIONS_LIST_KEY)
            .apply()
    }
} 