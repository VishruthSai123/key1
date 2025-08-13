package com.vishruth.key1.data

import kotlinx.serialization.Serializable

@Serializable
data class ChatMessage(
    val id: String = generateId(),
    val content: String,
    val isFromUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val conversationId: String = "",
    val messageType: MessageType = MessageType.TEXT
) {
    companion object {
        private fun generateId(): String = System.currentTimeMillis().toString() + (0..999).random()
    }
}

@Serializable
enum class MessageType {
    TEXT,
    SUMMARY,
    SYSTEM
}

@Serializable
data class ChatConversation(
    val id: String = generateConversationId(),
    val title: String = "Chat ${System.currentTimeMillis()}",
    val messages: List<ChatMessage> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val summary: String? = null,
    val messageCount: Int = messages.size
) {
    companion object {
        private fun generateConversationId(): String = "conv_${System.currentTimeMillis()}_${(0..999).random()}"
    }
}

@Serializable
data class ChatSummary(
    val conversationId: String,
    val summary: String,
    val messageRange: String,
    val createdAt: Long = System.currentTimeMillis()
) 