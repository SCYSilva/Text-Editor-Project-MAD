package com.example.tutorial_app_1.editor

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.gson.Gson
import java.io.InputStream

data class LangConfig(
    val name: String = "unknown",
    val keywords: List<String> = emptyList(),
    val commentLine: String? = null,
    val commentBlockStart: String? = null,
    val commentBlockEnd: String? = null,
    val stringDelimiters: List<String> = listOf("\"", "'")
)

class ConfigLoader(private val context: Context){
    private val gson = Gson()

    fun loadFromAssets(fileName: String): LangConfig? {
        return try {
            val input: InputStream = context.assets.open(fileName)
            val text = input.bufferedReader().use { it.readText() }
            gson.fromJson(text, LangConfig::class.java)
        } catch (e: Exception) {
            Log.e("LangConfigLoader", "Error loading $fileName: ${e.message}", e)
            null
        }
    }
    fun loadFromUri(uri: Uri): LangConfig?{
        return try {
            val text=context.contentResolver.openInputStream(uri)?.bufferedReader().use { it?.readText() }
             gson.fromJson(text, LangConfig::class.java)
        }catch (e: Exception) {
            Log.e("LangConfigLoader", "Error loading config: ${e.message}", e)
            null
        }

    }

}