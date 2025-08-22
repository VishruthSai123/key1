package com.vishruth.key1.api

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

/**
 * Service for OpenMoji emoji integration using emoji-api.com
 * Based on emojie.md documentation - supports Unicode 15 with 220+ categories
 */
class OpenMojiService {
    
    companion object {
        private const val BASE_URL = "https://emoji-api.com"
        private const val ACCESS_KEY = "414ee18c8fec19984dd2aecc72b46e343e2cfb4c"
        private const val TAG = "OpenMojiService"
        private const val REQUEST_TIMEOUT_MS = 10000L // 10 seconds
    }
    
    /**
     * Data class for emoji information
     */
    data class EmojiData(
        val slug: String,
        val character: String,
        val unicodeName: String,
        val codePoint: String,
        val group: String,
        val subGroup: String
    )
    
    /**
     * Data class for emoji category
     */
    data class EmojiCategory(
        val slug: String,
        val displayName: String,
        val assetIcon: String,  // Asset filename for custom icons
        val subCategories: List<String>
    )
    
    /**
     * Get all available emoji categories
     */
    suspend fun getCategories(): Result<List<EmojiCategory>> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Fetching emoji categories from API")
                
                val url = URL("$BASE_URL/categories?access_key=$ACCESS_KEY")
                val connection = url.openConnection() as HttpURLConnection
                
                connection.requestMethod = "GET"
                connection.connectTimeout = REQUEST_TIMEOUT_MS.toInt()
                connection.readTimeout = REQUEST_TIMEOUT_MS.toInt()
                connection.setRequestProperty("User-Agent", "NeoBoard-Android")
                
                val responseCode = connection.responseCode
                Log.d(TAG, "Categories API response code: $responseCode")
                
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val inputStream = BufferedReader(InputStreamReader(connection.inputStream, "UTF-8"))
                    val response = StringBuilder()
                    var line: String?
                    
                    while (inputStream.readLine().also { line = it } != null) {
                        response.append(line)
                    }
                    inputStream.close()
                    
                    val categories = parseCategories(response.toString())
                    Log.d(TAG, "Successfully fetched ${categories.size} categories")
                    Result.success(categories)
                } else {
                    val errorMessage = "Failed to fetch categories: HTTP $responseCode"
                    Log.e(TAG, errorMessage)
                    Result.failure(Exception(errorMessage))
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching categories", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Get emojis for a specific category
     */
    suspend fun getEmojisForCategory(categorySlug: String): Result<List<EmojiData>> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Fetching emojis for category: $categorySlug")
                
                val url = URL("$BASE_URL/categories/$categorySlug?access_key=$ACCESS_KEY")
                val connection = url.openConnection() as HttpURLConnection
                
                connection.requestMethod = "GET"
                connection.connectTimeout = REQUEST_TIMEOUT_MS.toInt()
                connection.readTimeout = REQUEST_TIMEOUT_MS.toInt()
                connection.setRequestProperty("User-Agent", "NeoBoard-Android")
                
                val responseCode = connection.responseCode
                Log.d(TAG, "Category emojis API response code: $responseCode")
                
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val inputStream = BufferedReader(InputStreamReader(connection.inputStream, "UTF-8"))
                    val response = StringBuilder()
                    var line: String?
                    
                    while (inputStream.readLine().also { line = it } != null) {
                        response.append(line)
                    }
                    inputStream.close()
                    
                    val emojis = parseEmojis(response.toString())
                    Log.d(TAG, "Successfully fetched ${emojis.size} emojis for $categorySlug")
                    Result.success(emojis)
                } else {
                    val errorMessage = "Failed to fetch emojis for $categorySlug: HTTP $responseCode"
                    Log.e(TAG, errorMessage)
                    Result.failure(Exception(errorMessage))
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching emojis for category $categorySlug", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Search emojis by query string
     */
    suspend fun searchEmojis(query: String): Result<List<EmojiData>> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Searching emojis for: $query")
                
                val url = URL("$BASE_URL/emojis?search=$query&access_key=$ACCESS_KEY")
                val connection = url.openConnection() as HttpURLConnection
                
                connection.requestMethod = "GET"
                connection.connectTimeout = REQUEST_TIMEOUT_MS.toInt()
                connection.readTimeout = REQUEST_TIMEOUT_MS.toInt()
                connection.setRequestProperty("User-Agent", "NeoBoard-Android")
                
                val responseCode = connection.responseCode
                Log.d(TAG, "Search API response code: $responseCode")
                
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val inputStream = BufferedReader(InputStreamReader(connection.inputStream, "UTF-8"))
                    val response = StringBuilder()
                    var line: String?
                    
                    while (inputStream.readLine().also { line = it } != null) {
                        response.append(line)
                    }
                    inputStream.close()
                    
                    val emojis = parseEmojis(response.toString())
                    Log.d(TAG, "Successfully found ${emojis.size} emojis for '$query'")
                    Result.success(emojis)
                } else {
                    val errorMessage = "Failed to search emojis: HTTP $responseCode"
                    Log.e(TAG, errorMessage)
                    Result.failure(Exception(errorMessage))
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error searching emojis for '$query'", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Parse categories JSON response based on emojie.md structure
     */
    private fun parseCategories(jsonString: String): List<EmojiCategory> {
        val categories = mutableListOf<EmojiCategory>()
        
        try {
            Log.d(TAG, "Parsing categories JSON response of length: ${jsonString.length}")
            val jsonArray = JSONArray(jsonString)
            
            for (i in 0 until jsonArray.length()) {
                val categoryObj = jsonArray.getJSONObject(i)
                val slug = categoryObj.optString("slug", "")
                val subCategoriesArray = categoryObj.optJSONArray("subCategories") ?: JSONArray()
                
                val subCategories = mutableListOf<String>()
                for (j in 0 until subCategoriesArray.length()) {
                    subCategories.add(subCategoriesArray.getString(j))
                }
                
                // Map category slugs to display names and asset icons (custom icons from assets folder)
                val categoryMapping = when (slug) {
                    "smileys-emotion" -> Pair("Faces", "faces.png")
                    "people-body" -> Pair("People", "people.png")
                    "animals-nature" -> Pair("Animals", "animals.png")  // Add animals category back
                    "food-drink" -> Pair("Food", "food.png")
                    "travel-places" -> Pair("Travel", "vehiclesandbuildings.png")
                    "activities" -> Pair("Activities", "Activities.png")
                    "objects" -> Pair("Objects", "objects.png")
                    "symbols" -> Pair("Symbols", "symbols.png")
                    "flags" -> Pair("Flags", "flags.png")
                    // Skip unknown categories to prevent duplicates
                    else -> null
                }
                
                // Only process known categories
                if (categoryMapping != null) {
                    val (displayName, assetIcon) = categoryMapping
                    
                    // Only add categories we explicitly support
                    if (slug.isNotBlank()) {
                        categories.add(EmojiCategory(slug, displayName, assetIcon, subCategories))
                        Log.d(TAG, "Parsed category: $slug -> $displayName (${subCategories.size} subcategories)")
                    }
                }
            }
            
            Log.d(TAG, "Successfully parsed ${categories.size} categories")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing categories JSON", e)
            Log.e(TAG, "JSON content preview: ${jsonString.take(500)}")
        }
        
        return categories
    }
    
    /**
     * Parse emojis JSON response based on emojie.md structure
     */
    private fun parseEmojis(jsonString: String): List<EmojiData> {
        val emojis = mutableListOf<EmojiData>()
        
        try {
            Log.d(TAG, "Parsing emojis JSON response of length: ${jsonString.length}")
            val jsonArray = JSONArray(jsonString)
            
            for (i in 0 until jsonArray.length()) {
                val emojiObj = jsonArray.getJSONObject(i)
                
                val slug = emojiObj.optString("slug", "")
                val character = emojiObj.optString("character", "")
                val unicodeName = emojiObj.optString("unicodeName", "")
                val codePoint = emojiObj.optString("codePoint", "")
                val group = emojiObj.optString("group", "")
                val subGroup = emojiObj.optString("subGroup", "")
                
                // Validate that we have essential data
                if (slug.isNotBlank() && character.isNotBlank() && codePoint.isNotBlank()) {
                    val emoji = EmojiData(
                        slug = slug,
                        character = character,
                        unicodeName = unicodeName,
                        codePoint = codePoint,
                        group = group,
                        subGroup = subGroup
                    )
                    
                    emojis.add(emoji)
                    
                    // Log first few emojis for debugging
                    if (i < 3) {
                        Log.d(TAG, "Parsed emoji [$i]: '$character' ($unicodeName) - $codePoint")
                    }
                } else {
                    Log.w(TAG, "Skipping invalid emoji at index $i: slug='$slug' character='$character' codePoint='$codePoint'")
                }
            }
            
            Log.d(TAG, "Successfully parsed ${emojis.size} valid emojis from ${jsonArray.length()} JSON objects")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing emojis JSON", e)
            Log.e(TAG, "JSON content preview: ${jsonString.take(500)}")
        }
        
        return emojis
    }
    
    /**
     * Get fallback emoji categories with custom asset icons (no duplicates)
     */
    fun getFallbackCategories(): List<EmojiCategory> {
        return listOf(
            EmojiCategory("smileys-emotion", "Faces", "faces.png", emptyList()),
            EmojiCategory("people-body", "People", "people.png", emptyList()),
            EmojiCategory("animals-nature", "Animals", "animals.png", emptyList()),
            EmojiCategory("food-drink", "Food", "food.png", emptyList()),
            EmojiCategory("travel-places", "Travel", "vehiclesandbuildings.png", emptyList()),
            EmojiCategory("activities", "Activities", "Activities.png", emptyList()),
            EmojiCategory("objects", "Objects", "objects.png", emptyList()),
            EmojiCategory("symbols", "Symbols", "symbols.png", emptyList()),
            EmojiCategory("flags", "Flags", "flags.png", emptyList())
        )
    }
}
