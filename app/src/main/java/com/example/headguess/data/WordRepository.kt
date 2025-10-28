package com.example.headguess.data

import com.example.headguess.network.NetworkDiscovery
import com.example.headguess.ui.CountryManager
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject

class WordRepository {
    @Volatile
    private var categories: Map<String, Any> = emptyMap()
    @Volatile
    private var currentCountry: String = "General"
    
    fun forceReload() {
        currentCountry = ""
        categories = emptyMap()
    }

    private fun ensureLoaded() {
        val context = NetworkDiscovery.applicationContext ?: return
        val countryFileName = CountryManager.getCountryFileName()
        
        android.util.Log.d("WordRepository", "ensureLoaded: currentCountry='$currentCountry', selectedCountry='${CountryManager.selectedCountry}', fileName='$countryFileName'")
        
        // Reload if country changed or not loaded yet
        if (categories.isNotEmpty() && currentCountry == CountryManager.selectedCountry) {
            android.util.Log.d("WordRepository", "Categories already loaded for country '$currentCountry'")
            return
        }
        
        try {
            val input = if (CountryManager.selectedCountry == "Custom") {
                // Load from internal storage saved by CustomStorage
                val f = java.io.File(context.filesDir, "custom_categories.json")
                if (f.exists()) {
                    val content = f.readText()
                    android.util.Log.d("WordRepository", "Loaded custom categories: ${content.take(200)}...")
                    content
                } else {
                    android.util.Log.d("WordRepository", "Custom categories file not found, using fallback")
                    context.assets.open("categories.json").bufferedReader().use { it.readText() }
                }
            } else {
                android.util.Log.d("WordRepository", "Loading file: $countryFileName.json")
                context.assets.open("$countryFileName.json").bufferedReader().use { it.readText() }
            }
            val json = Json { ignoreUnknownKeys = true }
            val root: JsonElement = json.parseToJsonElement(input)
            categories = when (root) {
                is JsonObject -> {
                    val result = root.mapValues { (_, value) -> value }
                    android.util.Log.d("WordRepository", "Successfully loaded categories: ${result.keys}")
                    result
                }
                else -> {
                    android.util.Log.e("WordRepository", "Root element is not a JsonObject")
                    emptyMap()
                }
            }
            currentCountry = CountryManager.selectedCountry
        } catch (e: Exception) {
            android.util.Log.e("WordRepository", "Failed to load $countryFileName.json, falling back to categories.json", e)
            try {
                // Fallback to general categories
                val input = context.assets.open("categories.json").bufferedReader().use { it.readText() }
                val json = Json { ignoreUnknownKeys = true }
                val root: JsonElement = json.parseToJsonElement(input)
                categories = when (root) {
                    is JsonObject -> {
                        val result = root.mapValues { (_, value) -> value }
                        android.util.Log.d("WordRepository", "Successfully loaded fallback categories: ${result.keys}")
                        result
                    }
                    else -> {
                        android.util.Log.e("WordRepository", "Fallback root element is not a JsonObject")
                        emptyMap()
                    }
                }
                currentCountry = "General"
            } catch (e2: Exception) {
                android.util.Log.e("WordRepository", "Failed to load fallback categories.json", e2)
                categories = emptyMap()
                currentCountry = "General"
            }
        }
    }

    fun getCategories(): List<String> {
        ensureLoaded()
        return categories.keys.toList()
    }
    
    /**
     * Debug method to get detailed information about loaded categories
     */
    fun getDebugInfo(): String {
        ensureLoaded()
        return buildString {
            appendLine("WordRepository Debug Info:")
            appendLine("Current Country: $currentCountry")
            appendLine("Selected Country: ${CountryManager.selectedCountry}")
            appendLine("Country File Name: ${CountryManager.getCountryFileName()}")
            appendLine("Categories Count: ${categories.size}")
            appendLine("Available Categories: ${categories.keys}")
            categories.forEach { (key, value) ->
                val wordCount = when (value) {
                    is JsonElement -> {
                        fun countWords(el: JsonElement): Int = when (el) {
                            is JsonObject -> el.values.sumOf { countWords(it) }
                            else -> el.toStringList().size
                        }
                        countWords(value)
                    }
                    else -> 0
                }
                appendLine("  $key: $wordCount words")
            }
        }
    }

    fun getRandomWord(category: String): String {
        ensureLoaded()
        android.util.Log.d("WordRepository", "getRandomWord: category='$category', availableCategories=${categories.keys}")
        
        // Find a key that matches case-insensitively; also handle singular -> plural fallbacks
        val catTrim = category.trim()
        val lowered = catTrim.lowercase()
        // Strong, whitelist-based mapping to prevent accidental mismatches
        val normalizedKey = when (lowered) {
            "objects", "object" -> categories.keys.firstOrNull { it.equals("Objects", ignoreCase = true) }
            "animals", "animal" -> categories.keys.firstOrNull { it.equals("Animals", ignoreCase = true) }
            "sports", "sport" -> categories.keys.firstOrNull { it.equals("Sports", ignoreCase = true) }
            "personality", "personalities" -> categories.keys.firstOrNull { it.equals("Personality", ignoreCase = true) }
            "custom" -> categories.keys.firstOrNull { it.equals("Custom", ignoreCase = true) }
            else -> categories.keys.firstOrNull { it.equals(catTrim, ignoreCase = true) }
        }
        android.util.Log.d("WordRepository", "getRandomWord: normalizedKey='$normalizedKey'")
        
        if (normalizedKey == null) {
            android.util.Log.e("WordRepository", "No matching category found for '$category'")
            return ""
        }
        
        val categoryData = categories[normalizedKey] as? JsonElement
        if (categoryData == null) {
            android.util.Log.e("WordRepository", "Category data is null for '$normalizedKey'")
            return ""
        }

        // Flatten arbitrarily nested objects/arrays into a single list of strings
        fun flattenWords(el: JsonElement): List<String> = when (el) {
            is JsonObject -> el.values.flatMap { value -> flattenWords(value) }
            else -> el.toStringList()
        }

        val allWords = flattenWords(categoryData)
        android.util.Log.d("WordRepository", "getRandomWord: allWords count=${allWords.size}, sample=${allWords.take(3)}")
        
        if (allWords.isEmpty()) {
            android.util.Log.e("WordRepository", "No words found in category '$normalizedKey'")
            return ""
        }
        
        val selectedWord = allWords.random()
        android.util.Log.d("WordRepository", "getRandomWord: selectedWord='$selectedWord'")
        return selectedWord
    }
    
    private fun JsonElement.toStringList(): List<String> = when (this) {
        is JsonPrimitive -> listOf(content)
        else -> runCatching { 
            this.jsonArray.mapNotNull { (it as? JsonPrimitive)?.content } 
        }.getOrElse { emptyList() }
    }
    
    // Impostor game specific methods
    private var impostorCategories: Map<String, List<Map<String, String>>> = emptyMap()
    @Volatile
    private var currentImpostorCountry: String = "General"
    
    private fun ensureImpostorLoaded() {
        val context = NetworkDiscovery.applicationContext ?: return
        val impostorFileName = CountryManager.getImpostorFileName()
        
        android.util.Log.d("WordRepository", "ensureImpostorLoaded: selectedCountry='${CountryManager.selectedCountry}', fileName='$impostorFileName', currentImpostorCountry='$currentImpostorCountry'")
        
        // Reload if country changed or not loaded yet
        if (impostorCategories.isNotEmpty() && currentImpostorCountry == CountryManager.selectedCountry) {
            android.util.Log.d("WordRepository", "Impostor categories already loaded for country '$currentImpostorCountry', categories: ${impostorCategories.keys}")
            return
        }
        
        try {
            val input = if (CountryManager.selectedCountry == "Custom") {
                val f = java.io.File(context.filesDir, "custom_impostor.json")
                if (f.exists()) f.readText() else context.assets.open("impostor.json").bufferedReader().use { it.readText() }
            } else {
                android.util.Log.d("WordRepository", "Loading impostor file: $impostorFileName.json")
                context.assets.open("$impostorFileName.json").bufferedReader().use { it.readText() }
            }
            val json = Json { ignoreUnknownKeys = true }
            val root: JsonElement = json.parseToJsonElement(input)
            if (root is JsonObject) {
                impostorCategories = root.mapValues { (_, value) ->
                    // Expected structure: category -> JsonArray of {A:..., B:...}
                    runCatching {
                        value.jsonArray.mapNotNull { element ->
                            val obj = element.jsonObject
                            val a = (obj["A"] as? JsonPrimitive)?.content ?: ""
                            val b = (obj["B"] as? JsonPrimitive)?.content ?: ""
                            if (a.isNotEmpty() || b.isNotEmpty()) mapOf("A" to a, "B" to b) else null
                        }
                    }.getOrElse { emptyList() }
                }
                currentImpostorCountry = CountryManager.selectedCountry
                android.util.Log.d("WordRepository", "Successfully loaded impostor categories: ${impostorCategories.keys}, word pair counts: ${impostorCategories.mapValues { it.value.size }}")
            }
        } catch (e: Exception) {
            android.util.Log.e("WordRepository", "Failed to load $impostorFileName.json, falling back to impostor.json", e)
            // Fallback to general impostor
            try {
                val input = context.assets.open("impostor.json").bufferedReader().use { it.readText() }
                val json = Json { ignoreUnknownKeys = true }
                val root: JsonElement = json.parseToJsonElement(input)
                if (root is JsonObject) {
                    impostorCategories = root.mapValues { (_, value) ->
                        runCatching {
                            value.jsonArray.mapNotNull { element ->
                                val obj = element.jsonObject
                                val a = (obj["A"] as? JsonPrimitive)?.content ?: ""
                                val b = (obj["B"] as? JsonPrimitive)?.content ?: ""
                                if (a.isNotEmpty() || b.isNotEmpty()) mapOf("A" to a, "B" to b) else null
                            }
                        }.getOrElse { emptyList() }
                    }
                    currentImpostorCountry = "General"
                }
            } catch (e2: Exception) {
                android.util.Log.e("WordRepository", "Failed to load fallback impostor words", e2)
            }
        }
    }
    
    fun getImpostorWordPair(category: String): Pair<String, String> {
        ensureImpostorLoaded()
        val catTrim = category.trim()
        val lowered = catTrim.lowercase()
        
        android.util.Log.d("WordRepository", "getImpostorWordPair: category='$category', available impostor categories: ${impostorCategories.keys}")
        
        val normalizedKey = when (lowered) {
            "objects", "object" -> impostorCategories.keys.firstOrNull { it.equals("Objects", ignoreCase = true) }
            "animals", "animal" -> impostorCategories.keys.firstOrNull { it.equals("Animals", ignoreCase = true) }
            "sports", "sport" -> impostorCategories.keys.firstOrNull { it.equals("Sports", ignoreCase = true) }
            "personality", "personalities" -> impostorCategories.keys.firstOrNull { it.equals("Personality", ignoreCase = true) }
            "places", "place" -> impostorCategories.keys.firstOrNull { it.equals("Places", ignoreCase = true) }
            "food" -> impostorCategories.keys.firstOrNull { it.equals("Food", ignoreCase = true) }
            else -> impostorCategories.keys.firstOrNull { it.equals(catTrim, ignoreCase = true) }
        }
        
        android.util.Log.d("WordRepository", "getImpostorWordPair: normalizedKey='$normalizedKey'")
        
        val categoryData = normalizedKey?.let { impostorCategories[it] } ?: emptyList()
        if (categoryData.isEmpty()) {
            android.util.Log.w("WordRepository", "getImpostorWordPair: No impostor pairs found for category '$category', falling back to regular words from categories file")
            // Fallback: derive a pair from the regular category words
            val first = getRandomWord(category)
            var second = getRandomWord(category)
            var guard = 0
            while (second == first && guard++ < 5) second = getRandomWord(category)
            return Pair(first, second)
        }
        
        // Select a random A word (correct word)
        val randomPairForA = categoryData.random()
        val wordA = randomPairForA["A"] ?: ""
        
        // Select a random B word (impostor word) - can be from any pair, not necessarily the same row
        val allBWords = categoryData.mapNotNull { it["B"] }.filter { it.isNotEmpty() && it != wordA }
        val wordB = if (allBWords.isNotEmpty()) {
            allBWords.random()
        } else {
            randomPairForA["B"] ?: ""
        }
        
        android.util.Log.d("WordRepository", "getImpostorWordPair: Selected CorrectWord (A)='$wordA', ImpostorWord (B)='$wordB' (randomly selected from ${allBWords.size} B words)")
        
        return Pair(wordA, wordB)
    }
}


