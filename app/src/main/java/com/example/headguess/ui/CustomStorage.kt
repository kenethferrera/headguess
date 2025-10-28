package com.example.headguess.ui

import android.content.Context
import com.example.headguess.network.NetworkDiscovery
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object CustomStorage {
    private const val CUSTOM_CATEGORIES_FILE = "custom_categories.json"
    private const val CUSTOM_IMPOSTOR_FILE = "custom_impostor.json"
    private const val CUSTOM_TITLES_FILE = "custom_titles.json"
    private const val CUSTOM_IMPOSTOR_TITLES_FILE = "custom_impostor_titles.json"

    private fun appContext(): Context? = NetworkDiscovery.applicationContext

    fun saveCategories(words: List<String>): Boolean {
        val ctx = appContext() ?: return false
        return runCatching {
            val root = JSONObject().apply {
                put("Custom", JSONArray(words))
            }
            val f = File(ctx.filesDir, CUSTOM_CATEGORIES_FILE)
            f.writeText(root.toString())
            true
        }.getOrElse { false }
    }

    fun saveCategoriesWithTitle(title: String, words: List<String>): Boolean {
        val ctx = appContext() ?: return false
        return runCatching {
            // Save the word list with title
            val titleFile = File(ctx.filesDir, "custom_${title.replace(" ", "_")}.json")
            val titleData = JSONObject().apply {
                put("title", title)
                put("words", JSONArray(words))
            }
            titleFile.writeText(titleData.toString())
            
            // Update titles list
            val titlesFile = File(ctx.filesDir, CUSTOM_TITLES_FILE)
            val existingTitles = if (titlesFile.exists()) {
                val titlesJson = JSONObject(titlesFile.readText())
                val arr = titlesJson.optJSONArray("titles") ?: JSONArray()
                buildList(arr.length()) { for (i in 0 until arr.length()) add(arr.optString(i)) }
            } else emptyList()
            
            val updatedTitles = if (title !in existingTitles) existingTitles + title else existingTitles
            val titlesData = JSONObject().apply {
                put("titles", JSONArray(updatedTitles))
            }
            titlesFile.writeText(titlesData.toString())
            true
        }.getOrElse { false }
    }

    fun loadCategories(): List<String> {
        val ctx = appContext() ?: return emptyList()
        val f = File(ctx.filesDir, CUSTOM_CATEGORIES_FILE)
        if (!f.exists()) return emptyList()
        return runCatching {
            val root = JSONObject(f.readText())
            val arr = root.optJSONArray("Custom") ?: JSONArray()
            buildList(arr.length()) {
                for (i in 0 until arr.length()) add(arr.optString(i).trim())
            }.filter { it.isNotEmpty() }
        }.getOrElse { emptyList() }
    }

    fun loadSavedTitles(): List<String> {
        val ctx = appContext() ?: return emptyList()
        val f = File(ctx.filesDir, CUSTOM_TITLES_FILE)
        if (!f.exists()) return emptyList()
        return runCatching {
            val root = JSONObject(f.readText())
            val arr = root.optJSONArray("titles") ?: JSONArray()
            buildList(arr.length()) { for (i in 0 until arr.length()) add(arr.optString(i)) }
        }.getOrElse { emptyList() }
    }

    fun loadCategoriesByTitle(title: String): List<String> {
        val ctx = appContext() ?: return emptyList()
        val f = File(ctx.filesDir, "custom_${title.replace(" ", "_")}.json")
        if (!f.exists()) return emptyList()
        return runCatching {
            val root = JSONObject(f.readText())
            val arr = root.optJSONArray("words") ?: JSONArray()
            buildList(arr.length()) { for (i in 0 until arr.length()) add(arr.optString(i).trim()) }
        }.getOrElse { emptyList() }
    }

    fun loadTimerByTitle(title: String): String {
        val ctx = appContext() ?: return "1"
        val f = File(ctx.filesDir, "custom_${title.replace(" ", "_")}.json")
        if (!f.exists()) return "1"
        return runCatching {
            val root = JSONObject(f.readText())
            root.optString("timer", "1")
        }.getOrElse { "1" }
    }

    fun deleteTitle(title: String): Boolean {
        val ctx = appContext() ?: return false
        return runCatching {
            // Delete the title file
            val titleFile = File(ctx.filesDir, "custom_${title.replace(" ", "_")}.json")
            if (titleFile.exists()) titleFile.delete()
            
            // Update titles list
            val titlesFile = File(ctx.filesDir, CUSTOM_TITLES_FILE)
            if (titlesFile.exists()) {
                val titlesJson = JSONObject(titlesFile.readText())
                val arr = titlesJson.optJSONArray("titles") ?: JSONArray()
                val existingTitles = buildList(arr.length()) { for (i in 0 until arr.length()) add(arr.optString(i)) }
                val updatedTitles = existingTitles.filter { it != title }
                val titlesData = JSONObject().apply {
                    put("titles", JSONArray(updatedTitles))
                }
                titlesFile.writeText(titlesData.toString())
            }
            true
        }.getOrElse { false }
    }

    fun saveImpostor(guesserWords: List<String>, impostorWords: List<String>): Boolean {
        val ctx = appContext() ?: return false
        return runCatching {
            val root = JSONObject().apply {
                put("Guesser", JSONArray(guesserWords))
                put("Impostor", JSONArray(impostorWords))
            }
            val f = File(ctx.filesDir, CUSTOM_IMPOSTOR_FILE)
            f.writeText(root.toString())
            true
        }.getOrElse { false }
    }

    data class CustomImpostor(val guesser: List<String>, val impostor: List<String>)

    fun loadImpostor(): CustomImpostor {
        val ctx = appContext() ?: return CustomImpostor(emptyList(), emptyList())
        val f = File(ctx.filesDir, CUSTOM_IMPOSTOR_FILE)
        if (!f.exists()) return CustomImpostor(emptyList(), emptyList())
        return runCatching {
            val root = JSONObject(f.readText())
            val g = root.optJSONArray("Guesser") ?: JSONArray()
            val i = root.optJSONArray("Impostor") ?: JSONArray()
            val guesser = buildList(g.length()) { for (idx in 0 until g.length()) add(g.optString(idx).trim()) }.filter { it.isNotEmpty() }
            val impostor = buildList(i.length()) { for (idx in 0 until i.length()) add(i.optString(idx).trim()) }.filter { it.isNotEmpty() }
            CustomImpostor(guesser, impostor)
        }.getOrElse { CustomImpostor(emptyList(), emptyList()) }
    }

    // Impostor-specific title methods
    fun saveImpostorWithTitle(title: String, guesserWords: List<String>, impostorWords: List<String>): Boolean {
        val ctx = appContext() ?: return false
        return runCatching {
            // Save the impostor word list with title
            val titleFile = File(ctx.filesDir, "custom_impostor_${title.replace(" ", "_")}.json")
            val titleData = JSONObject().apply {
                put("title", title)
                put("guesserWords", JSONArray(guesserWords))
                put("impostorWords", JSONArray(impostorWords))
            }
            titleFile.writeText(titleData.toString())
            
            // Update impostor titles list
            val titlesFile = File(ctx.filesDir, CUSTOM_IMPOSTOR_TITLES_FILE)
            val existingTitles = if (titlesFile.exists()) {
                val titlesJson = JSONObject(titlesFile.readText())
                val arr = titlesJson.optJSONArray("titles") ?: JSONArray()
                buildList(arr.length()) { for (i in 0 until arr.length()) add(arr.optString(i)) }
            } else emptyList()
            
            val updatedTitles = if (title !in existingTitles) existingTitles + title else existingTitles
            val titlesData = JSONObject().apply {
                put("titles", JSONArray(updatedTitles))
            }
            titlesFile.writeText(titlesData.toString())
            true
        }.getOrElse { false }
    }

    fun loadImpostorTitles(): List<String> {
        val ctx = appContext() ?: return emptyList()
        val f = File(ctx.filesDir, CUSTOM_IMPOSTOR_TITLES_FILE)
        if (!f.exists()) return emptyList()
        return runCatching {
            val root = JSONObject(f.readText())
            val arr = root.optJSONArray("titles") ?: JSONArray()
            buildList(arr.length()) { for (i in 0 until arr.length()) add(arr.optString(i)) }
        }.getOrElse { emptyList() }
    }

    fun loadImpostorByTitle(title: String): CustomImpostor {
        val ctx = appContext() ?: return CustomImpostor(emptyList(), emptyList())
        val f = File(ctx.filesDir, "custom_impostor_${title.replace(" ", "_")}.json")
        if (!f.exists()) return CustomImpostor(emptyList(), emptyList())
        return runCatching {
            val root = JSONObject(f.readText())
            val guesserArr = root.optJSONArray("guesserWords") ?: JSONArray()
            val impostorArr = root.optJSONArray("impostorWords") ?: JSONArray()
            val guesser = buildList(guesserArr.length()) { for (i in 0 until guesserArr.length()) add(guesserArr.optString(i).trim()) }.filter { it.isNotEmpty() }
            val impostor = buildList(impostorArr.length()) { for (i in 0 until impostorArr.length()) add(impostorArr.optString(i).trim()) }.filter { it.isNotEmpty() }
            CustomImpostor(guesser, impostor)
        }.getOrElse { CustomImpostor(emptyList(), emptyList()) }
    }

    fun deleteImpostorTitle(title: String): Boolean {
        val ctx = appContext() ?: return false
        return runCatching {
            // Delete the title file
            val titleFile = File(ctx.filesDir, "custom_impostor_${title.replace(" ", "_")}.json")
            if (titleFile.exists()) titleFile.delete()
            
            // Update impostor titles list
            val titlesFile = File(ctx.filesDir, CUSTOM_IMPOSTOR_TITLES_FILE)
            if (titlesFile.exists()) {
                val titlesJson = JSONObject(titlesFile.readText())
                val arr = titlesJson.optJSONArray("titles") ?: JSONArray()
                val existingTitles = buildList(arr.length()) { for (i in 0 until arr.length()) add(arr.optString(i)) }
                val updatedTitles = existingTitles.filter { it != title }
                val titlesData = JSONObject().apply {
                    put("titles", JSONArray(updatedTitles))
                }
                titlesFile.writeText(titlesData.toString())
            }
            true
        }.getOrElse { false }
    }
}
