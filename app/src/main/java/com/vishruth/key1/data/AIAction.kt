package com.vishruth.key1.data

/**
 * Represents the different AI actions available in the smart top bar
 * Each action has carefully crafted prompts for optimal AI performance
 */
enum class AIAction(val displayName: String, val prompt: String) {
    REWRITE("Rewrite", """
        Fix grammar, spelling, and punctuation errors. Keep the original text style and tone intact.
        
        Text to correct:
    """.trimIndent()),
    
    SUMMARIZE("Summarize", """
        Create a concise summary with the main points.
        
        Text to summarize:
    """.trimIndent()),
    
    EXPLAIN("Explain", """
        Explain this concept clearly and simply.
        
        Text/concept to explain:
    """.trimIndent()),
    
    LISTIFY("Listify", """
        Convert this into a clean bullet point list.
        
        Text to convert to list:
    """.trimIndent()),
    
    EMOJIFY("Emojify", """
        Add relevant emojis to enhance the text naturally.
        
        Text to emojify:
    """.trimIndent()),
    
    MAKE_FORMAL("Make Formal", """
        Rewrite in a professional, formal tone.
        
        Text to make formal:
    """.trimIndent()),
    
    TWEETIFY("Tweetify", """
        Rewrite for Twitter under 280 characters. Make it engaging and punchy.
        
        Text to tweetify:
    """.trimIndent()),
    
    PROMPTIFY("Promptify", """
        Improve this as a clear, specific AI prompt with better instructions.
        
        Text to improve as a prompt:
    """.trimIndent()),
    
    TRANSLATE("Translate", """
        Translate accurately. If English, translate to the most appropriate language. If foreign, translate to English.
        
        Text to translate:
    """.trimIndent()),
    
    CREATIVE_WRITE("Creative", """
        Rewrite creatively with vivid, engaging language while keeping the core meaning.
        
        Text to rewrite creatively:
    """.trimIndent()),
    
    ANSWER("Answer", """
        Provide a helpful, accurate answer.
        
        Question/statement to respond to:
    """.trimIndent()),
    
    LETTER("Letter", """
        Transform this text into a well-structured letter format. Include:
        - Appropriate greeting (Dear/Hi/Hello based on context)
        - Properly formatted body with clear paragraphs
        - Professional closing (Sincerely/Best regards/etc.)
        - Proper spacing and letter structure
        - Maintain the original message content and tone
        
        Text to format as a letter:
    """.trimIndent())
}

data class AIActionData(
    val id: String,
    val emoji: String,
    val description: String
) {
    companion object {
        fun getAllActions(): List<AIActionData> {
            return AIAction.values().map {
                AIActionData(it.name, "", "")
            }
        }
        
        fun getActionById(id: String): AIActionData? {
            return getAllActions().find { it.id == id }
        }
    }
} 